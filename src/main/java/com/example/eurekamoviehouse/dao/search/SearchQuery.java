package com.example.eurekamoviehouse.dao.search;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by zhouqiang on 9/25/17.
 */
public class SearchQuery{
    public String search;

    @JsonCreator
    public SearchQuery(@JsonProperty("search") String search){
        this.search = search;
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }
}