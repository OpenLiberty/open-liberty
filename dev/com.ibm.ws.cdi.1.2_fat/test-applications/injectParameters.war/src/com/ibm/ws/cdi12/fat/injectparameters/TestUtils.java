package com.ibm.ws.cdi12.fat.injectparameters;

import java.util.List;

public class TestUtils {

    public static String join(List<String> stringList) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String string : stringList) {
            if (!first) {
                sb.append(", ");
            } else {
                first = false;
            }
            sb.append(string);
        }
        return sb.toString();
    }
}
