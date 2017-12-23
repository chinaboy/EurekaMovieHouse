package com.example.eurekamoviehouse.service;

import org.apache.mahout.cf.taste.recommender.RecommendedItem;

import java.util.List;

/**
 * Created by zhouqiang on 10/25/17.
 */
public class RecommendList {

    private List<RecommendedItem> recommendations;

    public List<RecommendedItem> getRecommendations() {
        return recommendations;
    }

    public boolean empty(){
        return recommendations.isEmpty();
    }

    public String getMovieIdList(){
        String movieQuery = "";
        for (RecommendedItem recommendation : recommendations) {

            movieQuery += recommendation.getItemID() + ",";
        }
        movieQuery = movieQuery.substring(0, movieQuery.length() - 1);
        return movieQuery;
    }
}
