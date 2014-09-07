package ish.burst.ms.objects;

import fr.cryptohash.MD5;
import ish.burst.ms.services.PlotService;
import org.apache.catalina.util.MD5Encoder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Date;
import java.util.UUID;
import java.util.zip.CRC32;

/**
 * Created by ihartney on 9/2/14.
 */
public class PlotFile {

    private static final Log LOGGER = LogFactory.getLog(PlotFile.class);

    PlotService plotService;
    File plotFile;

    private long address;
    private long startnonce;
    private long plots;
    private long staggeramt;

    private long fileSize;
    private long leftToGenerate;


    private double fileSizeGb;
    private double expectedFileSizeGb;

    private int percentageComplete;
    private boolean complete;

    private boolean generating=false;

    private int sharesFound=0;
    private int timesChecked=0;
    private int timesIncomplete=0;

    public double getFileSizeGb() {
        return fileSizeGb;
    }

    public void setFileSizeGb(double fileSizeGb) {
        this.fileSizeGb = fileSizeGb;
    }

    public double getExpectedFileSizeGb() {
        return expectedFileSizeGb;
    }

    public void setExpectedFileSizeGb(double expectedFileSizeGb) {
        this.expectedFileSizeGb = expectedFileSizeGb;
    }


    public File getPlotFile() {
        return plotFile;
    }

    public void setPlotFile(File plotFile) {
        this.plotFile = plotFile;
    }

    public long getAddress() {
        return address;
    }

    public void setAddress(long address) {
        this.address = address;
    }

    public long getStartnonce() {
        return startnonce;
    }

    public void setStartnonce(long startnonce) {
        this.startnonce = startnonce;
    }

    public long getPlots() {
        return plots;
    }

    public void setPlots(long plots) {
        this.plots = plots;
    }

    public long getStaggeramt() {
        return staggeramt;
    }

    public void setStaggeramt(long staggeramt) {
        this.staggeramt = staggeramt;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public long getLeftToGenerate() {
        return leftToGenerate;
    }

    public void setLeftToGenerate(long leftToGenerate) {
        this.leftToGenerate = leftToGenerate;
    }

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    public int getPercentageComplete() {
        return percentageComplete;
    }

    synchronized public void addShare(){
        sharesFound++;
    }

    synchronized public void addChecked(){
        timesChecked++;
    }

    synchronized public void addIncomplete(){
        timesIncomplete++;
    }

    public int getShares(){
        return sharesFound;
    }

    public int getChecked(){
        return timesChecked;
    }

    public int getInterrupted(){
        return timesIncomplete;
    }

    public PlotFile(File plotFile,PlotService plotService){
        this.plotFile = plotFile;
        this.plotService = plotService;
        String[] data = plotFile.getName().split("_");

        this.address = parseUnsignedLong(data[0], 10);
        this.startnonce = Long.valueOf(data[1]);
        this.plots = Long.valueOf(data[2]);
        this.staggeramt = Long.valueOf(data[3]);
        refreshData();
    }

    public void refreshData(){
        //plotFile = new File(plotFile.toURI());
        this.fileSize = plotFile.length();
        this.leftToGenerate = (MiningPlot.PLOT_SIZE * plots) - fileSize;
        if(leftToGenerate==0){
            this.percentageComplete = 100;
        }else {
            this.percentageComplete = (int) (((double) 100 / (double) (MiningPlot.PLOT_SIZE * plots)) * this.fileSize);
        }
        this.fileSizeGb = (double)this.fileSize / 1024d / 1024d / 1024d;
        this.expectedFileSizeGb = (double)(MiningPlot.PLOT_SIZE * plots) / 1024d / 1024d / 1024d;
        if(leftToGenerate==0){
            complete = true;
        }else{
            complete = false;
            Date nextUpdate = new Date();
            nextUpdate.setTime(System.currentTimeMillis()+plotService.getUpdateInterval());
            plotService.getTaskScheduler().schedule(new UpdatePlotData(), nextUpdate);
        }
    }

    public String getUUID(){
        return UUID.nameUUIDFromBytes(plotFile.getName().getBytes()).toString();
    }


    public void generate(){
        if(fileSize!=0 && generating == false){
            return;
        }else{
            plotService.getTaskExecutor().execute(new GeneratePlotFile());
            generating = true;
        }
    }

    public static long parseUnsignedLong(String s, int radix)
            throws NumberFormatException {
        BigInteger b= new BigInteger(s,radix);
        if(b.bitLength()>64)
            throw new NumberFormatException(s+" is to big!");
        return b.longValue();
    }

    class UpdatePlotData implements Runnable{
        @Override
        public void run() {
            refreshData();
        }
    }


    class GeneratePlotFile implements Runnable{

        int done = 0;
        long currentNonce;
        boolean staggergenerating = false;

        byte[] outputbuffer = new byte[(int) (staggeramt * MiningPlot.PLOT_SIZE)];

        FileOutputStream out;

        synchronized public void plotDone(MiningPlot plot, long nonce){
            long off = nonce - currentNonce;
            for(int i = 0; i < MiningPlot.SCOOPS_PER_PLOT; i++) {
                System.arraycopy(plot.data,
                        i * MiningPlot.SCOOP_SIZE,
                        outputbuffer,
                        (int) ((i * MiningPlot.SCOOP_SIZE * staggeramt) + (off * MiningPlot.SCOOP_SIZE)),
                        MiningPlot.SCOOP_SIZE);
            }
            done++;
        }

        @Override
        public void run() {
            try{out=new FileOutputStream(plotFile);}catch(IOException ioex){
                LOGGER.error("Error opening PlotFile",ioex);
                return;
            }
            currentNonce = startnonce;
            while(currentNonce != (startnonce+plots)){
                for(int i=0;i<staggeramt;i++){
                    plotService.getPlotGenerationPool().execute(new GeneratePlot(currentNonce+i,this));
                }
                while(done!=staggeramt){
                    try{Thread.sleep(1000);}catch(Exception ex){}
                }
                done=0;
                try{
                    out.write(outputbuffer);
                    out.flush();
                }catch(IOException ioex){
                    LOGGER.error("Error writing plotFile {"+plotFile.getName()+"}");
                    return;
                }
                currentNonce += staggeramt;
            }
            try{out.close();}catch(IOException ioex){return;}
        }
    }

    class GeneratePlot implements  Runnable{

        private long nonce;
        private GeneratePlotFile controller;

        public GeneratePlot(long nonce,GeneratePlotFile controller){
            this.nonce = nonce;
            this.controller = controller;

        }

        @Override
        public void run() {
            MiningPlot plot = new MiningPlot(address,nonce);
            controller.plotDone(plot,nonce);
        }
    }



}
