package ish.burst.ms.objects.api;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by ihartney on 9/6/14.
 */
public class SystemInfo {


    long uptime;
    long timeSinceLastShare;


    String name;

    public long getUptime() {
        return uptime;
    }

    public void setUptime(long uptime) {
        this.uptime = uptime;
    }

    public long getTimeSinceLastShare() {
        return timeSinceLastShare;
    }

    public void setTimeSinceLastShare(long timeSinceLastShare) {
        this.timeSinceLastShare = timeSinceLastShare;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUptimeH(){
        return convertDurationToH(this.uptime);
    }

    public String getTimeSinceLastShareH(){
        return convertDurationToH(this.timeSinceLastShare);
    }


    private String convertDurationToH(long duration){

        String remainingStr = "";
        long remainingTime = duration;

        int days = (int)TimeUnit.MILLISECONDS.toDays(remainingTime);
        remainingStr += (days == 1) ? "1 Day : " : days+" Days : ";
        remainingTime -= TimeUnit.DAYS.toMillis(days);


        int hours = (int)TimeUnit.MILLISECONDS.toHours(remainingTime);
        remainingStr += (hours == 1) ? "1 Hour : " : hours+" Hours : ";
        remainingTime -= TimeUnit.HOURS.toMillis(hours);

        int minutes = (int)TimeUnit.MILLISECONDS.toMinutes(remainingTime);
        remainingStr += (minutes == 1) ? "1 Minute : " : minutes+" Minutes : ";
        remainingTime -= TimeUnit.MINUTES.toMillis(minutes);


        int seconds = (int)TimeUnit.MILLISECONDS.toSeconds(remainingTime);
        remainingStr += (seconds == 1) ? "1 Second" : seconds+" Seconds";

        return remainingStr;

    }




}
