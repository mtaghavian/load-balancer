package loadbalancer.misc;

import loadbalancer.Application;

import java.io.File;
import java.lang.reflect.Field;

public class SystemUtils {

    public static String executeSingleCommand(String addr, String cmd) {
        File cmd1File = null, cmd2File = null, cmdOut = null;
        try {
            boolean windows = System.getProperty("os.name").toLowerCase().contains("windows");
            cmd = (windows ? "@echo off\n" : "") + (addr != null ? "cd " + addr + "\n" : "") + cmd + "\n";
            long time = System.currentTimeMillis();
            cmd1File = new File(Application.tmpPath + "/" + Application.appName + "-cmd1-" + time + (windows ? ".bat" : ".sh"));
            cmd2File = new File(Application.tmpPath + "/" + Application.appName + "-cmd2-" + time + (windows ? ".bat" : ".sh"));
            cmdOut = new File(Application.tmpPath + "/" + Application.appName + "-cmd-" + time + ".out");
            StreamUtils.writeString(cmd, cmd2File);
            StreamUtils.writeString((windows ? "cmd /c " : "sh ") + cmd2File.getPath() + " > " + cmdOut.getPath() + " 2>&1" + "\n", cmd1File);
            Process proc = Runtime.getRuntime().exec(((windows ? "cmd /c " : "sh ") + cmd1File.getPath()), null, new File("."));
            proc.waitFor();
            return StreamUtils.readString(cmdOut);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        } finally {
            StreamUtils.delete(cmd1File);
            StreamUtils.delete(cmd2File);
            StreamUtils.delete(cmdOut);
        }
    }

    public static synchronized long getPID(Process p) {
        long pid = -1;
        try {
            if (p.getClass().getName().equals("java.lang.UNIXProcess")) {
                Field f = p.getClass().getDeclaredField("pid");
                f.setAccessible(true);
                pid = f.getLong(p);
                f.setAccessible(false);
            }
        } catch (Exception e) {
            pid = -1;
        }
        return pid;
    }

    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }
}
