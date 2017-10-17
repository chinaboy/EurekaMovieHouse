package com.example.eurekamoviehouse.dao.movie;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by zhouqiang on 10/11/17.
 */
public class Rating {
    private long userid;
    private long movieid;
    private int rating;

    @JsonCreator
    public Rating(@JsonProperty("userid") long userid, @JsonProperty("movieid") long movieid, @JsonProperty("rating") int rating) {
        this.userid = userid;
        this.movieid = movieid;
        this.rating = rating;
    }

    public long getUserid() {
        return userid;
    }

    public void setUserid(long userid) {
        this.userid = userid;
    }

    public long getMovieid() {
        return movieid;
    }

    public void setMovieid(long movieid) {
        this.movieid = movieid;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }
}
