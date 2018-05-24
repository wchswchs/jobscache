package com.jobs.cache;

import java.io.Serializable;

public class ShowInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String name;

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ShowInfo() {
    }

    public ShowInfo(String id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("{");
        sb.append("id=").append(id);
        sb.append("name=").append(name);
        sb.append("}");

        return sb.toString();
    }

}
