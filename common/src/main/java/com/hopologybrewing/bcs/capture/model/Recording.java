package com.hopologybrewing.bcs.capture.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class Recording {
    private String id;
    protected Date timestamp;

    public String getId() {
        // name plus timestamp is unique -> name = primary, timestamp = sort key
        String name = getName();
        if (id == null && name != null) {
            setId(name);
        }

        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    protected abstract String getName();

    public abstract Object getData();
}
