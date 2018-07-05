import com.gargoylesoftware.htmlunit.BrowserVersion;
import models.NexusPHP;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import tools.ConvertJson;
import tools.DefaultKey;
import tools.MailBySendgrid;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.sun.org.apache.xalan.internal.lib.ExsltDatetime.time;
import static java.lang.Thread.sleep;

/**
 * Created by SpereShelde on 2018/6/6.
 */
public class BoxHelper {

    private Map configures = new HashMap();
    private Map cookies = new HashMap();
    private Map drivers = new HashMap();

    private void getConfigures() {// Get configures from file.

        try {
            this.configures = ConvertJson.convertConfigure("config.json");
        } catch (IOException e) {
            e.printStackTrace();
        }

        ArrayList<Path> jsonFiles = new ArrayList<>();

        try {
            DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get("cookies"));
            for(Path path : stream){
                if (path.getFileName().toString().endsWith(".json")) {
                    jsonFiles.add(path);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Loading cookies ...");
        for (Path path: jsonFiles) {
            try {
                String domainName = path.getFileName().toString();
                cookies.put(domainName.substring(0, domainName.lastIndexOf(".")), ConvertJson.convertCookie(path.toString()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        ArrayList<String> urls = (ArrayList<String>) this.configures.get("urls");
        urls.forEach(url -> {
            HtmlUnitDriver driver = new HtmlUnitDriver(BrowserVersion.FIREFOX_45, false);
            String domain = url.substring(url.indexOf("//") + 2, url.indexOf("/", url.indexOf("//") + 2));
            driver.get("http://" + domain);
            ArrayList<Cookie> cookiesT = (ArrayList) cookies.get(domain);
            cookiesT.forEach(cookie -> driver.manage().addCookie(cookie));
            drivers.put(url, driver);
        });

        System.out.println("Initialization done.");
    }

    private String getMaxDisk(){

        String maxDisk = "/home";
        try {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec("df -l");
            process.waitFor();
            BufferedReader in = null;
            try {
                in = new BufferedReader(new InputStreamReader(process.getInputStream()));
                int maxSize = 0;
                int currentSize = 0;
                String line = in.readLine().replaceAll("\\s+", " ");
                String[] temp = line.split(" ");
                int indexofA = 0;
                int indexofP = 0;
                int indexofM = 0;
                int count = 0;
                for (String s:temp) {
                    if (s.contains("Avail")){
                        indexofA = count;
                    }
                    if (s.contains("%")){
                        indexofP = count;
                    }
                    if (s.contains("Mount")){
                        indexofM = count;
                    }
                    count++;
                }
                while ((line = in.readLine()) != null) {
                    temp = line.replaceAll("\\s+", " ").split(" ");
                    currentSize = Integer.parseInt(temp[indexofA]);
                    if (currentSize > maxSize){
                        maxSize = currentSize;
                        maxDisk = temp[indexofM];
                    }
                }
                in.close();
            } catch (Exception e) {
                System.out.println("Cannot get max disk 1.");
                System.exit(107);
            }
        } catch (Exception e) {
            System.out.println("Cannot get max disk 2.");
            System.exit(108);
        }
        System.out.println("The max disk is " + maxDisk);
        return maxDisk;
    }

    private boolean canContinue(String disk, int limit){
        boolean flag = true;
        try {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec("df -l");
            BufferedReader in = null;
            int current = 0;
            try {
                in = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line = in.readLine();
                while (!line.contains(disk)){
                    line = in.readLine();
                }
                current = Integer.parseInt(line.substring(line.indexOf("%") - 2, line.indexOf("%")));
                System.out.println("Current disk use : " + current);
                if (current >= limit) flag = false;
                in.close();
            } catch (Exception e) {
                System.out.println("Cannot restrict 1.");
                System.exit(109);
            }
        } catch (Exception e) {
            System.out.println("Cannot restrict 2.");
            System.exit(110);
        }
        return flag;
    }

    public static void main(String[] args) {

        Logger logger = Logger.getLogger("");
        logger.setLevel(Level.OFF);
        BoxHelper boxHelper = new BoxHelper();
        boxHelper.getConfigures();
        String maxDisk = "";
        int limit = Integer.parseInt(boxHelper.configures.get("diskUsedPercentage").toString());
        if (limit != -1 && limit != 0) {
            maxDisk = boxHelper.getMaxDisk();
        }
        int cpuThreads = Runtime.getRuntime().availableProcessors();
        int count  = 1;

        ArrayList<NexusPHP> nexusPHPS = new ArrayList<>();
        boxHelper.drivers.forEach((url, driver) -> {
            String[] urlAndLimit = boxHelper.configures.get(url).toString().split("/");
            Map qbconfig = new HashMap();
            qbconfig.put("webUI", boxHelper.configures.get("webUI").toString());
            qbconfig.put("sessionID", boxHelper.configures.get("sessionID").toString());
            nexusPHPS.add(new NexusPHP(url.toString(), Double.parseDouble(urlAndLimit[0]), Double.parseDouble(urlAndLimit[1]), Double.parseDouble(urlAndLimit[2]),Double.parseDouble(urlAndLimit[3]), (HtmlUnitDriver)driver, qbconfig));
        });

        while (true){
            ExecutorService executorService = Executors.newFixedThreadPool(cpuThreads);
            System.out.println("\nBoxHelper " + count + " begins at " + time());
            if (limit != -1 && limit != 0) {
                if (!boxHelper.canContinue(maxDisk, limit)){
                    System.out.println("Reached limit, exit.");
                    if (!"".equals(boxHelper.configures.get("email").toString())) {
                        MailBySendgrid mailBySendgrid = null;
                        try {
                            InetAddress inetAddress = InetAddress.getLocalHost();
                            if (!"".equals(boxHelper.configures.get("sendgridKey").toString())) {
                                mailBySendgrid = new MailBySendgrid("seedboxhelper@gmail.com", boxHelper.configures.get("email").toString(), "Disk reached limit!", "Disk reached limit!\n Box IP: " + inetAddress + "\nLog in and check!", boxHelper.configures.get("sendgridKey").toString());
                            }else {
                                mailBySendgrid = new MailBySendgrid("seedboxhelper@gmail.com", boxHelper.configures.get("email").toString(), "Disk reached limit!", "Disk reached limit!\n Box IP: " + inetAddress + "\nLog in and check!", DefaultKey.getKey());
                            }
                            if (mailBySendgrid.send()){
                                System.out.println("Email sent.");
                            }else {
                                System.out.println("Cannot send email.");
                            }
                        } catch (UnknownHostException e) {
                            System.out.println("Cannot get IP.");
                        } catch (IOException e) {
                            System.out.println("Cannot send email.");
                        }
                    }

                    System.exit(111);
                } else {
                    System.out.println("Under limit, continue.");
                }
            }

            nexusPHPS.forEach(nexusPHP -> {
                executorService.submit(nexusPHP);
            });
            executorService.shutdown();
            try {
                sleep((long) (1000*Double.valueOf(boxHelper.configures.get("runningCycleInSec").toString())));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            count++;
        }
    }
}
