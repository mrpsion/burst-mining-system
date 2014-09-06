package ish.burst.ms.services;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * Created by ihartney on 9/1/14.
 */
@Service
@Scope("singleton")
public class FileSystemInfo {

   File workingDir;

   public FileSystemInfo(){
       workingDir = new File(System.getProperty("user.dir"));
   }

    public long getTotalSpace(){
        return workingDir.getTotalSpace();
    }

    public long getUseableSpace(){
        return workingDir.getUsableSpace();
    }

    public String getMinerBaseDirectory(){
        return workingDir.getAbsolutePath();
    }

    public double getTotalSpaceMb(){
        return (getTotalSpace() / 1024 / 1024);
    }

    public double getUseableSpaceMb(){
        return (getUseableSpace() / 1024 / 1024);
    }

}
