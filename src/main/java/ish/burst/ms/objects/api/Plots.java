package ish.burst.ms.objects.api;

import ish.burst.ms.objects.PlotFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ihartney on 9/5/14.
 */
public class Plots {

    public List<Plot> getPlots() {
        return plots;
    }

    public void setPlots(List<Plot> plots) {
        this.plots = plots;
    }

    private List<Plot> plots;

    public Plots(ArrayList<PlotFile> pPlots){
        plots = new ArrayList<Plot>();
        for(PlotFile file : pPlots){
            plots.add(new Plot(file));
        }
    }

    public Plots(){

    }

}
