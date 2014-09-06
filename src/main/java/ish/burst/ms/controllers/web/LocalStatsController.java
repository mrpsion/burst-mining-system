package ish.burst.ms.controllers.web;

import ish.burst.ms.objects.CreatePlotRequest;
import ish.burst.ms.objects.GeneratePlotRequest;
import ish.burst.ms.services.FileSystemInfo;
import ish.burst.ms.services.NetStateService;
import ish.burst.ms.services.PlotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Map;

/**
 * Created by ihartney on 9/1/14.
 */
@Controller
public class LocalStatsController {

    @Autowired
    FileSystemInfo fileSystemInfo;

    @Autowired
    NetStateService netStateService;

    @Autowired
    PlotService plotService;


    @RequestMapping("/")
    public String localdashboard(Map<String, Object> model) {
        model.put("fileSystemInfo",fileSystemInfo);
        model.put("netState",netStateService.getCurrentState());
        model.put("plots",plotService.getPlots());
        return "local";
    }

    @RequestMapping(value="/addplot", method= RequestMethod.POST)
    public String addplot(@ModelAttribute CreatePlotRequest createRequest, Map<String, Object> model) {
        plotService.createPlot(createRequest);
        return "redirect:/";
    }


    @RequestMapping(value="/generateplot", method= RequestMethod.POST)
    public String generateplot(@ModelAttribute GeneratePlotRequest generateRequest, Map<String, Object> model) {
        plotService.generatePlot(generateRequest);
        return "redirect:/";
    }
}
