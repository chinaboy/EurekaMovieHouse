package com.example.eurekamoviehouse.dao.movie;

/**
 * Created by zhouqiang on 9/26/17.
 */
public class MiniMovie {
    private String title;
    private String director;
    private String actors;
    private String id;

    public MiniMovie(String title, String director, String actors, String id) {
        this.title = title;
        this.director = director;
        this.actors = actors;
        this.id = id;
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
