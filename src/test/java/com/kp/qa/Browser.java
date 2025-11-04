package com.kp.qa;

public enum Browser {
    CHROME, FIREFOX, SAFARI;

    public static boolean isMac() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("mac");
    }
}
