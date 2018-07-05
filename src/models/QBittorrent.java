package models;

import tools.HttpHelper;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by SpereShelde on 2018/7/2.
 */
public class QBittorrent{

    private String sessionID;
    private String webUI;
    private String site;
    private ArrayList<String> urls = new ArrayList<>();
    private double upload, download;
    private int apiVersion = 0;

    public void setUrls(ArrayList<String> urls) {
        this.urls = urls;
    }

    public QBittorrent(String sessionID, String webUI, String site, ArrayList<String> urls, double upload, double download) {
        this.sessionID = sessionID;
        if (webUI.lastIndexOf("/") == webUI.length() - 1) {
            this.webUI = webUI.substring(0, webUI.length() - 1);
        } else {
            this.webUI = webUI;
        }
        this.site = site;
        this.urls = urls;
        this.upload = upload;
        this.download = download;
    }

    public void addTorrents(){

        if (this.apiVersion == 0){
            try {
                apiVersion = Integer.parseInt(HttpHelper.doGet(webUI + "/version/api", "Fiddler", sessionID, "127.0.0.1"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (apiVersion >= 7){
            Map<String, String> contents = new HashMap<String, String>();
            StringBuilder links = new StringBuilder();
            urls.forEach(url -> links.append(url + "\n"));
            contents.put("urls", links.toString());
            contents.put("dlLimit", new BigDecimal(download * 1024 * 1024 + "").toPlainString());
            contents.put("upLimit", new BigDecimal(upload * 1024 * 1024 + "").toPlainString());
            try {
                Boolean success =  HttpHelper.doPost(webUI + "/command/download", "Fiddler", sessionID, "127.0.0.1", contents);
                if (success) System.out.println("Successfully add torrents in " + site + ".");
                else System.out.println("Cannot add torrents above, please check your session ID.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }


}