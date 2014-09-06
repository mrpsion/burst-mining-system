package ish.burst.ms.services;

import fr.cryptohash.Shabal256;
import ish.burst.ms.objects.MiningPlot;
import ish.burst.ms.objects.NetState;
import ish.burst.ms.objects.PlotFile;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ihartney on 9/3/14.
 */
@Service
@Scope("singleton")
public class MiningService {



    private static final Log LOGGER = LogFactory.getLog(MiningService.class);

    RestTemplate restTemplate = new RestTemplate();

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
    @Qualifier(value = "shareSubmitPool")
    TaskExecutor shareExecutor;


    NetState processing;
    NetState current;

    ArrayList<PlotFileMiner> minerThreads = new ArrayList<PlotFileMiner>();

    @PostConstruct
    public void init(){
        List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
        converters.add(new StringHttpMessageConverter());
        restTemplate.setMessageConverters(converters);
    }

    public void stopAndRestartMining(){
        for(PlotFileMiner miner : minerThreads){
            miner.stop();
            minerThreads.remove(miner);
            miner.plotFile.addIncomplete();
            LOGGER.info("Stopped mining {"+miner.plotFile.getUUID()+"} due to block change.");

        }
        this.processing = null;
        while(this.processing==null){
            this.processing = netStateService.getCurrentState();
            if(this.processing==null)try{Thread.sleep(500);}catch(Exception ex){}
        }

        for(PlotFile plotFile: plotService.getPlots()){
            PlotFileMiner miner = new PlotFileMiner(plotFile);
            minerThreads.add(miner);
            executor.execute(miner);
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

                    checkChunkPool(chunk);
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

        private void checkChunkPool(byte[] chunk){
            Shabal256 md = new Shabal256();
            for(long i = 0; i < plotFile.getStaggeramt(); i++) {
                md.reset();
                md.update(processing.getGensig());
                md.update(chunk, (int) (i * MiningPlot.SCOOP_SIZE), MiningPlot.SCOOP_SIZE);
                byte[] hash = md.digest();
                BigInteger num = new BigInteger(1, new byte[] {hash[7], hash[6], hash[5], hash[4], hash[3], hash[2], hash[1], hash[0]});
                BigInteger deadline = num.divide(BigInteger.valueOf(processing.getBaseTargetL()));
                if(deadline.compareTo(BigInteger.valueOf(processing.getTargetDeadlineL())) <= 0) {
                    shareExecutor.execute(new SubmitShare(plotFile.getStartnonce()+i, plotFile));
                }
                if(!running)return;
            }

        }
    }


    private class SubmitShare implements  Runnable{

        long nonce;
        PlotFile plotFile;

        public SubmitShare(long nonce,PlotFile plotFile){
            this.nonce = nonce;
            this.plotFile = plotFile;
        }


        @Override
        public void run() {
            try {
                String shareRequest = plotFile.getAddress()+":"+nonce+":"+processing.getHeight()+"\n";
                LOGGER.info("Submitting Share {"+shareRequest+"}");
                String response = restTemplate.postForObject(poolUrl + "/pool/submitWork",shareRequest,String.class);
                LOGGER.info("Response {"+response+"}");
                plotFile.addShare();
            }catch(Exception ex){
                LOGGER.info("Failed to submitShare");
            }
        }
    }



}
