package com.example.eurekamoviehouse.dao.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by zhouqiang on 8/31/17.
 */
public class UserProfile {
    public int id;
    public String username;
    public String password;
    public String email;

    @JsonCreator
    public UserProfile(@JsonProperty("id") int id, @JsonProperty("username") String username, @JsonProperty("password") String password, @JsonProperty("email") String email) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
