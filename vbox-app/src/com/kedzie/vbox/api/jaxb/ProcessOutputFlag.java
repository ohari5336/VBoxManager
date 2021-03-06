package com.kedzie.vbox.api.jaxb;

import java.io.Serializable;

public enum ProcessOutputFlag implements Serializable{
    NONE("None"),
    STD_ERR("StdErr");
    private final String value;
    public String toString() {
        return value;
    }
    ProcessOutputFlag(String v) {
        value = v;
    }
    public String value() {
        return value;
    }
    public static ProcessOutputFlag fromValue(String v) {
        for (ProcessOutputFlag c: ProcessOutputFlag.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
