package com.ibm.ws.cdi12.alterablecontext.test.extension;

import java.util.LinkedList;
import java.util.List;

public class DirtySingleton {
    private static List<String> strings = new LinkedList<String>();

    public static List<String> getStrings() {
        return strings;

    }

    public static void addString(String s) {
        strings.add(s);
    }
}
