package ish.burst.ms.controllers.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

/**
 * Created by ihartney on 9/7/14.
 */

@Controller
public class LogController {

    @RequestMapping("/log")
    public String log(Map<String, Object> model) {
        return "log";
    }

}
