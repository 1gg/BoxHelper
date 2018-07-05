package models;

/**
 * Created by SpereShelde on 2018/7/2.
 */
public class Torrents {

    private String name, hash, tracker;
    private int downloadLimit, uploadLimit, size, lastActivity;

    public Torrents(String name, String hash, String tracker, int downloadLimit, int uploadLimit, int size, int lastActivity) {
        this.name = name;
        this.hash = hash;
        this.tracker = tracker;
        this.downloadLimit = downloadLimit;
        this.uploadLimit = uploadLimit;
        this.size = size;
        this.lastActivity = lastActivity;
    }

    public String getName() {
        return name;
    }

    public String getHash() {
        return hash;
    }

    public String getTracker() {
        return tracker;
    }

    public int getDownloadLimit() {
        return downloadLimit;
    }

    public int getUploadLimit() {
        return uploadLimit;
    }

    public int getSize() {
        return size;
    }

    public int getLastActivity() {
        return lastActivity;
    }
}
