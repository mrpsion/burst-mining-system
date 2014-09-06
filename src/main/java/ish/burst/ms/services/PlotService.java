package ish.burst.ms.services;

import ish.burst.ms.objects.CreatePlotRequest;
import ish.burst.ms.objects.GeneratePlotRequest;
import ish.burst.ms.objects.PlotFile;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by ihartney on 9/2/14.
 */
@Service
@Scope("singleton")
public class PlotService {


    private static final Log LOGGER = LogFactory.getLog(PlotService.class);



    @Autowired
    @Value("${plotmonitor.update.time}")
    long updateInterval;


    @Autowired
    @Value("${plot.folder}")
    String plotFolder;

    File folder;

    ArrayList<PlotFile> plots = new ArrayList<PlotFile>();

    @Autowired
    @Qualifier(value = "taskScheduler")
    TaskScheduler scheduler;

    @Autowired
    @Qualifier(value = "taskExecutor")
    TaskExecutor executor;

    @Autowired
    @Qualifier(value = "plotGenerationPool")
    TaskExecutor plotGenerationPool;



    public ArrayList<PlotFile> getPlots() {
        return plots;
    }

    public void setPlots(ArrayList<PlotFile> plots) {
        this.plots = plots;
    }

    public long getUpdateInterval() {
        return updateInterval;
    }

    public TaskScheduler getTaskScheduler(){
        return scheduler;
    }

    public TaskExecutor getPlotGenerationPool(){
        return plotGenerationPool;
    }

    public TaskExecutor getTaskExecutor(){
        return executor;
    }

    @PostConstruct
    public void init(){
        folder = new File(plotFolder);
        if(!folder.exists()) {
            folder.mkdir();
        }
        File[] files = folder.listFiles();
        for(File file : files){
            try {
                PlotFile plot = new PlotFile(file, this);
                plots.add(plot);
            }catch(Exception ex){
                LOGGER.error(ex);
            }
        }
        scheduler.scheduleAtFixedRate(new PlotMonitor(),updateInterval);

    }

    public boolean plotExists(File file){
        for(PlotFile plotFile : plots){
            if(plotFile.getPlotFile().getName().equals(file.getName()))return true;
        }
        return false;
    }

    public PlotService getThis(){
        return this;
    }

    public void createPlot(CreatePlotRequest createRequest){
        try {
            File plotFile = new File(folder.getPath() + File.separator + createRequest.getAddress() + "_" + createRequest.getStartnonce() + "_" + createRequest.getPlots() + "_" + createRequest.getStaggeramt());
            plotFile.createNewFile();
            updatePlots();
        }catch(IOException ioex){
            LOGGER.error(ioex);
        }
    }

    public void generatePlot(GeneratePlotRequest generatePlotRequest){
        PlotFile plotFile = getPlot(generatePlotRequest.getUUID());
        if(plotFile!=null){
            plotFile.generate();
        }
    }

    public PlotFile getPlot(String UUID){
        for(PlotFile plotFile : plots){
            if(plotFile.getUUID().equals(UUID))return plotFile;
        }
        return null;
    }

    public synchronized void updatePlots(){
        File[] files = folder.listFiles();
        for(File file : files){
            if(!plotExists(file)) {
                try {
                    PlotFile plot = new PlotFile(file,getThis());
                    plots.add(plot);
                }catch(Exception ex){

                }
            }
        }

        for(PlotFile plotFile : plots){
            boolean exists = false;
            for(File file : files){
                if(file.getName().equals(plotFile.getPlotFile().getName())){
                    exists = true;
                }
            }
            if(!exists){
                plots.remove(plotFile);
            }
        }
    }


    class PlotMonitor implements Runnable{

        @Override
        public void run() {
           updatePlots();
        }
    }








}
