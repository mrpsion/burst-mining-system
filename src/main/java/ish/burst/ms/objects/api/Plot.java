package ish.burst.ms.objects.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ish.burst.ms.objects.PlotFile;

/**
 * Created by ihartney on 9/5/14.
 */
public class Plot {

    private String UUID;

    private long address;
    private long startnonce;
    private long plots;
    private long staggeramt;


    private double fileSizeGb;


    private double expectedFileSizeGb;

    private int percentageComplete;

    private int sharesFound;
    private int timesChecked;
    private int timesIncomplete;

    private String displayAddress;

    public String getDisplayAddress() {
        return displayAddress;
    }

    public void setDisplayAddress(String displayAddress) {
        this.displayAddress = displayAddress;
    }



    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
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

    public int getPercentageComplete() {
        return percentageComplete;
    }

    public void setPercentageComplete(int percentageComplete) {
        this.percentageComplete = percentageComplete;
    }

    public int getSharesFound() {
        return sharesFound;
    }

    public void setSharesFound(int sharesFound) {
        this.sharesFound = sharesFound;
    }

    public int getTimesChecked() {
        return timesChecked;
    }

    public void setTimesChecked(int timesChecked) {
        this.timesChecked = timesChecked;
    }

    public int getTimesIncomplete() {
        return timesIncomplete;
    }

    public void setTimesIncomplete(int timesIncomplete) {
        this.timesIncomplete = timesIncomplete;
    }


    public Plot(PlotFile plotFile){
        this.address = plotFile.getAddress();
        this.displayAddress = plotFile.getDisplayAddress();
        this.startnonce = plotFile.getStartnonce();
        this.plots = plotFile.getPlots();
        this.staggeramt = plotFile.getStaggeramt();
        this.fileSizeGb = plotFile.getFileSizeGb();
        this.expectedFileSizeGb = plotFile.getExpectedFileSizeGb();
        this.percentageComplete = plotFile.getPercentageComplete();
        this.sharesFound = plotFile.getShares();
        this.timesChecked = plotFile.getChecked();
        this.timesIncomplete = plotFile.getInterrupted();
        this.UUID = plotFile.getUUID();
    }

    @JsonIgnore
    public double getSharesPerBlock(){
        return (double)this.sharesFound / (double)(timesChecked+timesIncomplete);
    }

    public Plot(){

    }

}
