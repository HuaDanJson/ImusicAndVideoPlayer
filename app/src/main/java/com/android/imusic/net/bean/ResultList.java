package com.android.imusic.net.bean;

import java.util.List;

/**
 * 409
 * 2019/3/13
 */

public class ResultList<T> {

    private List<T> list;

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }

    @Override
    public String toString() {
        return "ResultList{" +
                "list=" + list +
                '}';
    }
}
