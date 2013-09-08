package com.simon.catkins.demo.app;

import com.simon.catkins.demo.app.mvc.BaseController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Simon Yu
 */
public class Config {
    static final String ENTRY_ID = "entry_id";

    public static class ControllerEntry {
        String id;
        String content;
        Class<? extends BaseController> clazz;

        public ControllerEntry(String id, String content, Class<? extends BaseController> clazz) {
            this.id = id;
            this.content = content;
            this.clazz = clazz;
        }
    }

    public static Map<String, ControllerEntry> MAP = new HashMap<String, ControllerEntry>();
    public static List<ControllerEntry> LIST = new ArrayList<ControllerEntry>();

    private static void addEntry(ControllerEntry entry) {
        MAP.put(entry.id, entry);
        LIST.add(entry);
    }

    static {
        addEntry(new ControllerEntry("1", "FlipperLayout", FlipperLayoutController.class));
        addEntry(new ControllerEntry("2", "Horizontal translate", HorizontalTranslateLayoutController.class));
    }
}
