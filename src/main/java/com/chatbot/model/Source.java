package com.chatbot.model;

public class Source {

    private String url;
    private String title;
    private double score;

    public Source() {}

    public Source(String url, String title, double score) {
        this.url = url;
        this.title = title;
        this.score = score;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }
}
