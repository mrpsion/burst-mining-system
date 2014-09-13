package ish.burst.ms.services;

import ish.burst.ms.objects.NetState;
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
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Created by ihartney on 9/2/14.
 */
@Service
@Scope("singleton")
public class NetStateService {

    @Autowired
    @Value("${pool.type}")
    String poolType;


    private static final Log LOGGER = LogFactory.getLog(NetStateService.class);

    @Autowired
    @Value("${pool.url}")
    String poolUrl;

    @Autowired
    @Qualifier(value = "taskScheduler")
    TaskScheduler scheduler;

    @Autowired
    @Value("${netstat.update.time}")
    long updateInterval;

    @Autowired
    MiningService miningService;

    RestTemplate restTemplate = new RestTemplate();

    long startUpTime;

    private NetState currentState;


    public NetStateService(){

    }

    @PostConstruct
    public void init(){
        startUpTime = System.currentTimeMillis();
        enableSSL();
        new UpdateNetState().run();
        scheduler.scheduleAtFixedRate(new UpdateNetState(),updateInterval);
    }


    public long getStartUpTime(){
        return this.startUpTime;
    }


    private void enableSSL() {
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
        };

        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
        }
    }

    public NetState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(NetState currentState) {
        this.currentState = currentState;
    }



    private class UpdateNetState implements Runnable{

        @Override
        public void run() {
            try {

                NetState requestedState = null;
                if(poolType.equals(MiningService.POOL_TYPE_URAY)) {
                    requestedState = restTemplate.getForObject(poolUrl + "/burst?requestType=getMiningInfo", NetState.class);
                }else if(poolType.equals(MiningService.POOL_TYPE_OFFICIAL)){
                    requestedState = restTemplate.getForObject(poolUrl + "/pool/getMiningInfo", NetState.class);

                }
                if (currentState == null) {
                    setCurrentState(requestedState);
                    miningService.stopAndRestartMining();

                } else {
                    if (!requestedState.getHeight().equals(currentState.getHeight())) {
                        setCurrentState(requestedState);
                        miningService.stopAndRestartMining();
                        LOGGER.info("New block detected");
                    }
                }
            }catch(Exception ex){
                LOGGER.info("Could not fetch block data from pool");
            }
        }
    }
}
