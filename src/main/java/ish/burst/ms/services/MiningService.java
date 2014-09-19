package ish.burst.ms.services;

import fr.cryptohash.Shabal256;
import ish.burst.ms.objects.MiningPlot;
import ish.burst.ms.objects.NetState;
import ish.burst.ms.objects.PlotFile;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Scope;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by ihartney on 9/3/14.
 */
@Service
@Scope("singleton")
public class MiningService {

    public static String POOL_TYPE_URAY="uray";
    public static String POOL_TYPE_OFFICAL="offical";

    private static final Log LOGGER = LogFactory.getLog(MiningService.class);

    RestTemplate restTemplate = new RestTemplate();

    @Autowired
    @Value("${pool.type}")
    String poolType;


    @Autowired
    @Value("${pool.url}")
    String poolUrl;

    @Autowired
    NetStateService netStateService;

    @Autowired
    PlotService plotService;

    @Autowired
    @Qualifier(value = "minerPool")
    TaskExecutor executor;

    @Autowired
    @Qualifier(value = "cpuMiningPool")
    TaskExecutor cpuMiningPool;

    @Autowired
    @Qualifier(value = "shareSubmitPool")
    TaskExecutor shareExecutor;

    @Autowired
    @Value("${miner.cpu.address}")
    String minerCpuAddress;


    @Autowired
    @Value("${miner.cpu.threads}")
    int minerCpuThreads;
    @Autowired
    @Value("${miner.cpu.enabled}")
    boolean minerCpuEnabled;


    NetState processing;
    NetState current;

    ArrayList<PlotFileMiner> minerThreads = new ArrayList<PlotFileMiner>();

    Map<Long, String> loadedPassPhrases = new HashMap<Long, String>();

    long lastShareSubmitTime;

    BigInteger deadline = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF", 16);

    Random rand;

    boolean startedCPUMining = false;

       public synchronized void registerBestShareForChunk(BigInteger lowestInChunk,long nonce,PlotFile plotFile){
           if(lowestInChunk.compareTo(deadline) < 0) {
               deadline = lowestInChunk;
               shareExecutor.execute(new SubmitShare(nonce,plotFile,deadline));
           }
       }



    @PostConstruct
    public void init(){
        rand = new Random();
        List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
        converters.add(new StringHttpMessageConverter());
        restTemplate.setMessageConverters(converters);
        loadPassPhrases();
    }

    public long getLastShareSubmitTime(){
        return lastShareSubmitTime;
    }


    public synchronized long nextNonce(){
        return (long)(rand.nextDouble()*Long.MAX_VALUE);

    }



    private void loadPassPhrases(){
        try {
            List<String> passphrases = Files.readAllLines(Paths.get("passphrases.txt"), Charset.forName("US-ASCII"));
            for(String ps : passphrases) {
                if(!ps.isEmpty()) {
                    byte[] publicKey = Crypto.getPublicKey(ps);
                    byte[] publicKeyHash = Crypto.sha256().digest(publicKey);
                    Long id = Convert.fullHashToId(publicKeyHash);
                    loadedPassPhrases.put(id, ps);
                    LOGGER.info("Added key: {" + ps + "} -> {" + Convert.toUnsignedLong(id) + "}");
                }
            }
        } catch (IOException e) {
            LOGGER.info("Warning: no passphrases.txt found");

        }
    }



    public void stopAndRestartMining(){


        if(minerCpuEnabled){
            if(!startedCPUMining){
                LOGGER.info("Starting CPU Mining with {"+minerCpuThreads+"} threads");

                for(int i=0;i<minerCpuThreads;i++){
                    cpuMiningPool.execute(new CPUMiner());
                }

            }
        }


        for(PlotFileMiner miner : minerThreads){
            miner.stop();
            miner.plotFile.addIncomplete();
            LOGGER.info("Stopped mining {"+miner.plotFile.getUUID()+"} due to block change");
        }
        minerThreads.clear();
        this.processing = null;
        while(this.processing==null){
            this.processing = netStateService.getCurrentState();
            if(this.processing==null)try{Thread.sleep(500);}catch(Exception ex){}
        }

        deadline = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF", 16);
        for(PlotFile plotFile: plotService.getPlots()){
            PlotFileMiner miner = new PlotFileMiner(plotFile);
            minerThreads.add(miner);
            executor.execute(miner);
        }





    }


    class CPUMiner implements Runnable{
        long address = 0;

        @Override
        public void run() {


            if(StringUtils.isEmpty(minerCpuAddress)){

            }else{
                address = plotService.getPlots().get(0).getAddress();
            }

            if(address==0){
                LOGGER.info("No address found for cpu mining.");
                return;
            }


            while(true){
                long nonce = nextNonce();

                MiningPlot plot = new MiningPlot(address,nonce);
                if(poolType.equals(POOL_TYPE_OFFICAL)){
                    checkPlotOffical(plot,nonce);
                }else if(poolType.equals(POOL_TYPE_URAY)){
                    checkPlotOffical(plot,nonce);
                }

            }

        }

        public void checkPlotOffical(MiningPlot plot,long nonce){
            ByteBuffer buf = ByteBuffer.allocate(32 + 8);
            buf.put(processing.getGensig());
            buf.putLong(processing.getHeightL());

            Shabal256 md = new Shabal256();
            md.update(buf.array());
            BigInteger hashnum = new BigInteger(1, md.digest());
            int scoopnum = hashnum.mod(BigInteger.valueOf(MiningPlot.SCOOPS_PER_PLOT)).intValue();
            md.reset();
            md.update(processing.getGensig());
            plot.hashScoop(md,scoopnum);
            byte[] hash = md.digest();
            BigInteger num = new BigInteger(1, new byte[] {hash[7], hash[6], hash[5], hash[4], hash[3], hash[2], hash[1], hash[0]});
            BigInteger deadline = num.divide(BigInteger.valueOf(processing.getBaseTargetL()));
            int compare = deadline.compareTo(BigInteger.valueOf(processing.getTargetDeadlineL()));

            if(compare <= 0) {
                shareExecutor.execute(new SubmitShare(nonce, address ,deadline));
            }

        }

        public void checkPlotUray(MiningPlot plot,long nonce){


        }
    }



    class PlotFileMiner implements Runnable{

        private PlotFile plotFile;
        private int scoopnum;
        private boolean running=true;

        public void stop(){
            running=false;
        }

        public PlotFileMiner(PlotFile plotFile){

            ByteBuffer buf = ByteBuffer.allocate(32 + 8);
            buf.put(processing.getGensig());
            buf.putLong(processing.getHeightL());

            Shabal256 md = new Shabal256();
            md.update(buf.array());
            BigInteger hashnum = new BigInteger(1, md.digest());
            scoopnum = hashnum.mod(BigInteger.valueOf(MiningPlot.SCOOPS_PER_PLOT)).intValue();
            this.plotFile = plotFile;
        }

        @Override
        public void run() {

            try(RandomAccessFile f = new RandomAccessFile(plotFile.getPlotFile(), "r")) {
                long chunks = plotFile.getPlots()/ plotFile.getStaggeramt();
                for(long i = 0; i < chunks; i++) {
                    f.seek((i * plotFile.getStaggeramt() * MiningPlot.PLOT_SIZE) + (scoopnum * plotFile.getStaggeramt() * MiningPlot.SCOOP_SIZE));
                    byte[] chunk = new byte[(int) (plotFile.getStaggeramt() * MiningPlot.SCOOP_SIZE)];
                    f.readFully(chunk);
                    if(poolType.equals(POOL_TYPE_URAY)) {
                        checkChunkPoolUray(chunk,plotFile.getStartnonce() + (i * plotFile.getStaggeramt()));
                    }else if(poolType.equals(POOL_TYPE_OFFICAL)){
                        checkChunkPoolOffical(chunk,plotFile.getStartnonce() + (i * plotFile.getStaggeramt()));
                    }
                    if(!running)return;
                }
            } catch (FileNotFoundException e) {
                LOGGER.info("Cannot open file: " + plotFile.getPlotFile().getName());
                e.printStackTrace();
            } catch (IOException e) {
                LOGGER.info("Error reading file: " + plotFile.getPlotFile().getName());
            }

            LOGGER.info("Finished mining {"+plotFile.getUUID()+"}");
            plotFile.addChecked();
            minerThreads.remove(this);
        }

        private void checkChunkPoolUray(byte[] chunk,long chunk_start_nonce){
            Shabal256 md = new Shabal256();
            BigInteger lowest = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF", 16);
            long lowestscoop = 0;
            for(long i = 0; i < plotFile.getStaggeramt(); i++) {
                md.reset();
                md.update(processing.getGensig());
                md.update(chunk, (int) (i * MiningPlot.SCOOP_SIZE), MiningPlot.SCOOP_SIZE);
                byte[] hash = md.digest();
                BigInteger num = new BigInteger(1, new byte[] {hash[7], hash[6], hash[5], hash[4], hash[3], hash[2], hash[1], hash[0]});
                BigInteger deadline = num.divide(BigInteger.valueOf(processing.getBaseTargetL()));

                int compare = deadline.compareTo(lowest);
                if(compare < 0) {
                    lowest = deadline;
                    lowestscoop = chunk_start_nonce + i;
                }
                if(!running)return;
            }
            registerBestShareForChunk(lowest,lowestscoop,plotFile);
        }

        private void checkChunkPoolOffical(byte[] chunk,long chunk_start_nonce){
            Shabal256 md = new Shabal256();
            for(long i = 0; i < plotFile.getStaggeramt(); i++) {
                md.reset();
                md.update(processing.getGensig());
                md.update(chunk, (int) (i * MiningPlot.SCOOP_SIZE), MiningPlot.SCOOP_SIZE);
                byte[] hash = md.digest();
                BigInteger num = new BigInteger(1, new byte[] {hash[7], hash[6], hash[5], hash[4], hash[3], hash[2], hash[1], hash[0]});
                BigInteger deadline = num.divide(BigInteger.valueOf(processing.getBaseTargetL()));
                int compare = deadline.compareTo(BigInteger.valueOf(processing.getTargetDeadlineL()));

                if(compare <= 0) {
                    shareExecutor.execute(new SubmitShare(chunk_start_nonce+i, plotFile,deadline));
                }
                if(!running)return;
            }

        }

    }


    private class SubmitShare implements  Runnable{

        long nonce;
        PlotFile plotFile;
        BigInteger deadline;
        long address;

        public SubmitShare(long nonce,PlotFile plotFile,BigInteger deadline){
            this.nonce = nonce;
            this.plotFile = plotFile;
            this.deadline = deadline;
            this.address = plotFile.getAddress();
        }

        public SubmitShare(long nonce,long address,BigInteger deadline){
            this.nonce = nonce;
            this.deadline = deadline;
            this.address = address;
        }


        @Override
        public void run() {
            String shareRequest = Convert.toUnsignedLong(plotFile.getAddress()) + ":" + nonce + ":" + processing.getHeight()+" deadline {"+deadline+"}";
            LOGGER.info("Submitting share {"+shareRequest+"}");
            try {
                if(poolType.equals(POOL_TYPE_URAY)) {
                    String request = poolUrl + "/burst?requestType=submitNonce&secretPhrase=pool-mining&nonce=" + Convert.toUnsignedLong(nonce) + "&accountId=" + Convert.toUnsignedLong(address);
                    String response = restTemplate.postForObject(request, shareRequest, String.class);
                    LOGGER.info("Reponse {"+response+"}}");
                    if(plotFile==null){
                        LOGGER.info("Submitted CPU Share.");
                    }else{
                        plotFile.addShare();
                    }
                }else if(poolType.equals(POOL_TYPE_OFFICAL)){
                    shareRequest = Convert.toUnsignedLong(address)+":"+Convert.toUnsignedLong(nonce)+":"+processing.getHeight()+"\n";
                    String response = restTemplate.postForObject(poolUrl + "/pool/submitWork",shareRequest,String.class);
                    LOGGER.info("Reponse {"+response+"}}");
                    if(plotFile==null){
                        LOGGER.info("Submitted CPU Share.");
                    }else{
                        plotFile.addShare();
                    }                }
                lastShareSubmitTime = System.currentTimeMillis();
            }catch(Exception ex){
                LOGGER.info("Failed to submitShare {"+shareRequest+"}");
            }
        }
    }



}
