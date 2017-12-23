package com.example.eurekamoviehouse.service;

/**
 * Created by zhouqiang on 10/25/17.
 */
public class UserIdWrapper {
    private long userid;

    public UserIdWrapper(long userid) {
        this.userid = userid;
    }

    public long getUserid() {
        return userid;
    }

    public void setUserid(long userid) {
        this.userid = userid;
    }
}
