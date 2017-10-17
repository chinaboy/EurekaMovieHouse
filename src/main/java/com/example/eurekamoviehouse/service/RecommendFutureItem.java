package com.example.eurekamoviehouse.service;

import org.apache.mahout.cf.taste.recommender.RecommendedItem;

import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.Future;

/**
 * Created by zhouqiang on 10/14/17.
 */
public class RecommendFutureItem {

    public long userId;
    public Future<List<RecommendedItem>> future;
    public Timestamp timestamp;

    public RecommendFutureItem(long userId, Future<List<RecommendedItem>> future, Timestamp timestamp) {
        this.userId = userId;
        this.future = future;
        this.timestamp = timestamp;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public Future<List<RecommendedItem>> getFuture() {
        return future;
    }

    public void setFuture(Future<List<RecommendedItem>> future) {
        this.future = future;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}
