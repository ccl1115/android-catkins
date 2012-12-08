package com.bbsimon.android.demo.data.json.model;

import com.fasterxml.jackson.annotation.*;

/**
 * @author simon.yu
 */
public class Demo {
    @JsonProperty("name")
    public String name;

    @JsonProperty("description")
    public String description;

    @JsonIgnore
    public Class activityClass;

    @JsonSetter("activity")
    public void setActivity(String activity) {
        try {
            this.activityClass = Class.forName("activity");
        } catch (ClassNotFoundException ignore) {
        }
    }

    @JsonGetter("activity")
    public String getActivity() {
        return this.activityClass.getName();
    }
}
