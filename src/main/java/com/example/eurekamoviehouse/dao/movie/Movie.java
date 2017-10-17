package com.example.eurekamoviehouse.dao.movie;

/**
 * Created by zhouqiang on 9/5/17.
 */
public class Movie {
    private String title;
    private String director;
    private String actors;
    private String plot;
    private String posterUrl;
    private Float rating;

    public Movie(String title, String director, String actors, String plot, String posterUrl, Float rating) {
        this.title = title;
        this.director = director;
        this.actors = actors;
        this.plot = plot;
        this.posterUrl = posterUrl;
        this.rating = rating;
    }

    public Float getRating() {
        return rating;
    }

    public void setRating(Float rating) {
        this.rating = rating;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDirector() {
        return director;
    }

    public void setDirector(String director) {
        this.director = director;
    }

    public String getActors() {
        return actors;
    }

    public void setActors(String actors) {
        this.actors = actors;
    }

    public String getPlot() {
        return plot;
    }

    public void setPlot(String plot) {
        this.plot = plot;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }
}
