package ish.burst.ms.objects;

/**
 * Created by ihartney on 9/3/14.
 */
public class CreatePlotRequest {

    private String address;
    private String startnonce;
    private String plots;
    private String staggeramt;

    public String getStaggeramt() {
        return staggeramt;
    }

    public void setStaggeramt(String staggeramt) {
        this.staggeramt = staggeramt;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getStartnonce() {
        return startnonce;
    }

    public void setStartnonce(String startnonce) {
        this.startnonce = startnonce;
    }

    public String getPlots() {
        return plots;
    }

    public void setPlots(String plots) {
        this.plots = plots;
    }
}
