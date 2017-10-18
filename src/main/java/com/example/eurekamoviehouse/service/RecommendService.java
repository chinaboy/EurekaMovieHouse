package com.example.eurekamoviehouse.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.Recommender;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.tomcat.dbcp.dbcp.PoolingDataSource;


/**
 * Created by zhouqiang on 10/11/17.
 */
public class RecommendService {

    private Log logger = LogFactory.getLog(RecommendService.class);
    private ExecutorService executor;


    public RecommendService() {

        executor = Executors.newSingleThreadExecutor();



    }

    /*
           First, select all from table ratings.
           Construct a matrix users by movies.  Keep matrix in the memory.

           Second, feed both user array and movie array into ALS function.
   *       Get user * k array, k * movie array.
   *       Calcute U M matrix.
        */
    public void reloadMatrix(PoolingDataSource dataSource){


    }

    /*
    *
    *   Third, serve the whole row by a certain user.
    * */
    public void recommendForUser(Recommender recommender, PoolingDataSource dataSource, long userId, int howMany, List<RecommendFutureItem> queue) throws InterruptedException {
        logger.info("Looking up user with ID of " + userId);

        Future<List<RecommendedItem>> recommendFuture = null;

        Callable<List<RecommendedItem>> task = () -> {
                List<RecommendedItem> recommendations = null;
                try {
                    Connection conn = dataSource.getConnection();

                        recommendations = recommender.recommend(userId, howMany);
                        for(RecommendedItem item : recommendations) {
                            PreparedStatement insertRecommendations = conn.prepareStatement("INSERT INTO recommendations (userid, movieid) VALUES(?, ?)");
                            insertRecommendations.setString(1, String.valueOf(userId));
                            insertRecommendations.setString(2, String.valueOf(item.getItemID()));
                            insertRecommendations.executeQuery();
                        }
                        conn.commit();
                        conn.close();


                } catch (SQLException e) {
                    e.printStackTrace();
                }
                catch (TasteException e) {
                    e.printStackTrace();
                    return null;
                }
                return recommendations;
        };

        Future<List<RecommendedItem>> result = executor.submit( task );
        queue.add(new RecommendFutureItem(userId, recommendFuture, new Timestamp(System.currentTimeMillis())) );
    }
}
