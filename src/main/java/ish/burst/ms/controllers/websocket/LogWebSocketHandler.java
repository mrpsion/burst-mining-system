package ish.burst.ms.controllers.websocket;

import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusListener;
import ish.burst.ms.services.SystemService;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created by ihartney on 9/7/14.
 */
public class LogWebSocketHandler extends TextWebSocketHandler{

    @Autowired
    @Value("${logging.file}")
    String logFileName;

    @Autowired
    @Qualifier(value = "taskScheduler")
    TaskScheduler scheduler;

    String lastLine=null;

    File logFile;

    @PostConstruct
    public void init(){
        logFile = new File(logFileName);
        scheduler.scheduleAtFixedRate(new UpdateLog(),5000);
    }


    HashMap<String,WebSocketSession> sessions = new HashMap<String,WebSocketSession>();


    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        sessions.remove(session.getId());
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        sessions.put(session.getId(),session);
    }

    public void sendLine(String line){
        for(WebSocketSession session : sessions.values()){
            try {
                session.sendMessage(new TextMessage(line));
            }catch(IOException ioex){

            }
        }
    }



    class UpdateLog implements Runnable{


        @Override
        public void run() {
            if(!sessions.isEmpty()){
                try {
                    ReversedLinesFileReader rlfr = new ReversedLinesFileReader(logFile);
                    if (lastLine == null) {
                        lastLine = rlfr.readLine();
                        if(lastLine!=null){
                            sendLine(lastLine);
                        }
                    }else{
                        LinkedList<String> lines = new LinkedList<String>();
                        boolean foundLast = false;
                        while(!foundLast){
                            String line = rlfr.readLine();
                            if(!line.equals(lastLine)){
                                lines.addFirst(line);
                            }else{
                                foundLast = true;
                                if(lines.size()!=0)lastLine = lines.getLast();
                            }
                        }
                        for(String line : lines)sendLine(line);

                    }

                }catch(IOException ioex){
                                     System.out.println("here");
                }
            }else{
                lastLine = null;
            }
        }
    }



}
