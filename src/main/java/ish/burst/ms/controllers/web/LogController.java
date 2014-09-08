package ish.burst.ms.controllers.web;

import ish.burst.ms.services.SystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * Created by ihartney on 9/7/14.
 */

@Controller
public class LogController {

    @Autowired
    SystemService systemService;

    @RequestMapping("/log")
    public String log(@RequestParam(value = "host", defaultValue = "local") String host, Map<String, Object> model) {

        model.put("systemInfos",systemService.getSystemInfos());
        model.put("host",host);
        return "log";
    }

}
