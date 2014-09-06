package ish.burst.ms.controllers.web;

import ish.burst.ms.objects.api.Plot;
import ish.burst.ms.objects.api.Plots;
import ish.burst.ms.services.PlotService;
import ish.burst.ms.services.SystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;

/**
 * Created by ihartney on 9/5/14.
 */
@Controller
public class SystemController {

    @Autowired
    SystemService systemService;


    @RequestMapping("/system")
    public String systemdashboard(Map<String, Object> model) {

        List<Plot> plots = systemService.getSystemPlots();
        model.put("plots",plots);

        double totalFileSize = 0;
        double totalExpected = 0;
        int totalShares = 0;
        int totalChecked = 0;
        int totalInterrupted = 0;
        for(Plot p : plots){
            totalFileSize+=p.getFileSizeGb();
            totalExpected+=p.getExpectedFileSizeGb();
            totalShares+=p.getSharesFound();
            totalChecked+=p.getTimesChecked();
            totalInterrupted+=p.getTimesIncomplete();
        }

        model.put("totalSize",totalFileSize);
        model.put("totalExpected",totalExpected);
        model.put("totalShares",totalShares);
        model.put("totalChecked",totalChecked);
        model.put("totalInterrupted",totalInterrupted);



        return "system";
    }


}
