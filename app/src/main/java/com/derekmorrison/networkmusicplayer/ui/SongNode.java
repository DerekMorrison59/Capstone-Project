package com.derekmorrison.networkmusicplayer.ui;

/**
 * Created by Derek on 8/25/2016.
 */
public class SongNode {

    private int Id;
    private int ParentId;
    private String FileName;
    private String FilePath;

    private String Title;
    private String Artist;
    private String Album;
    private String Genre;
    private String ArtUrl;

    private int Track;
    private int PlayCount;
    private int LastPlayed;
    private int Duration;
    private int DeepScan;

    public SongNode() {
    }

    public int getId() {
        return Id;
    }

    public void setId(int id) {
        Id = id;
    }

    public int getParentId() {
        return ParentId;
    }

    public void setParentId(int parentId) {
        ParentId = parentId;
    }

    public String getFileName() {
        return FileName;
    }

    public void setFileName(String fileName) {
        FileName = fileName;
    }

    public String getFilePath() {
        return FilePath;
    }

    public void setFilePath(String filePath) {
        FilePath = filePath;
    }

    public String getTitle() {
        return Title;
    }

    public void setTitle(String title) {
        Title = title;
    }

    public String getArtist() {
        return Artist;
    }

    public void setArtist(String artist) {
        Artist = artist;
    }

    public String getAlbum() {
        return Album;
    }

    public void setAlbum(String album) {
        Album = album;
    }

    public String getGenre() {
        return Genre;
    }

    public void setGenre(String genre) {
        Genre = genre;
    }

    public String getArtUrl() {
        return ArtUrl;
    }

    public void setArtUrl(String artUrl) {
        ArtUrl = artUrl;
    }

    public int getTrack() {
        return Track;
    }

    public void setTrack(int track) {
        Track = track;
    }

    public int getPlayCount() {
        return PlayCount;
    }

    public void setPlayCount(int playCount) {
        PlayCount = playCount;
    }

    public int getLastPlayed() {
        return LastPlayed;
    }

    public void setLastPlayed(int lastPlayed) {
        LastPlayed = lastPlayed;
    }

    public int getDuration() {
        return Duration;
    }

    public void setDuration(int duration) {
        Duration = duration;
    }

    public int getDeepScan() {
        return DeepScan;
    }

    public void setDeepScan(int deepScan) {
        DeepScan = deepScan;
    }
}
