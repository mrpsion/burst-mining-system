package ish.burst.ms.services;

import ish.burst.ms.objects.PlotFile;
import ish.burst.ms.objects.api.Plot;
import ish.burst.ms.objects.api.Plots;
import ish.burst.ms.objects.api.SystemInfo;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ihartney on 9/5/14.
 */

@Service
@Scope("singleton")
public class SystemService {

    private static final Log LOGGER = LogFactory.getLog(SystemService.class);


    @Autowired
    NetStateService netStateService;

    @Autowired
    MiningService miningService;

    @Autowired
    @Value("${system.hosts}")
    String hosts;

    @Autowired
    @Qualifier(value = "taskScheduler")
    TaskScheduler scheduler;

    @Autowired
    @Value("${system.update.time}")
    long updateInterval;


    @Autowired
    PlotService plotService;

    String[] hostsList;

    @PostConstruct
    public void init(){
        hostsList = hosts.split(",");
        scheduler.scheduleAtFixedRate(new UpdateSystem(),updateInterval);

    }

    ArrayList<Plot> plots = new ArrayList<Plot>();
    ArrayList<SystemInfo> systemInfos = new ArrayList<SystemInfo>();

    RestTemplate restTemplate = new RestTemplate();

    public List<Plot> getSystemPlots(){
        return plots;
    }

    public List<SystemInfo> getSystemInfos(){
        return this.systemInfos;
    }


    class UpdateSystem implements Runnable{

        @Override
        public void run() {
            ArrayList<Plot> newPlots = new ArrayList<Plot>();
            ArrayList<SystemInfo> newInfos = new ArrayList<SystemInfo>();
            Plots ps = new Plots(plotService.getPlots());
            for(Plot p : ps.getPlots() ){
                newPlots.add(p);
            }
            SystemInfo localInfo = new SystemInfo();
            localInfo.setName("local");

            long uptime = System.currentTimeMillis() - netStateService.getStartUpTime();
            long timeSinceLastShare = System.currentTimeMillis() - miningService.getLastShareSubmitTime();
            localInfo.setTimeSinceLastShare(timeSinceLastShare);
            localInfo.setUptime(uptime);
            newInfos.add(localInfo);

            for(String host:hostsList){
                try {
                    if (StringUtils.isNotEmpty(host)){
                        Plots remotePlots = restTemplate.getForObject("http://" + host + "/api/plots", Plots.class);
                        for (Plot p : remotePlots.getPlots()) {
                            newPlots.add(p);
                        }
                        SystemInfo info = restTemplate.getForObject("http://" + host + "/api/system_info",SystemInfo.class);
                        info.setName(host);
                        newInfos.add(info);
                    }
                }catch(Exception ex){
                    LOGGER.info("Could not get information from miner at {"+host+"}");
                    SystemInfo info = new SystemInfo();
                    info.setUptime(0);
                    info.setTimeSinceLastShare(0);
                    info.setName(host);
                    newInfos.add(info);
                }
            }
            systemInfos = newInfos;
            plots = newPlots;
        }
    }


}
