package com.bbsimon.android.demo.data.json.models;

import com.fasterxml.jackson.annotation.*;
import de.akquinet.android.androlog.Log;

/**
 * @author simon.yu
 */
public class Demo {
  private static final String TAG = "Demo";

  @JsonProperty("name")
  public String name;

  @JsonProperty("description")
  public String description;

  @JsonIgnore
  public Class activityClass;

  @JsonSetter("activity")
  public void setActivity(String activity) {
    try {
      activityClass = Class.forName(activity);
    } catch (ClassNotFoundException e) {
      Log.e(TAG, "@setActivity : ClassNotFoundException");
      e.printStackTrace();
    }
  }

  @JsonGetter("activity")
  public String getActivity() {
    return activityClass.getName();
  }
}
