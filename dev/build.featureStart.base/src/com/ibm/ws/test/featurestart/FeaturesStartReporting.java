package com.ibm.ws.test.featurestart;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class FeaturesStartReporting {
    public static void logInfo(String m, String msg) {
        FeaturesStartTestBase.logInfo(m, msg);
    }

    // Raw display API ...
    
    /**
     * Display a table of collections.
     *
     * @param m      The method requesting the display.
     * @param prefix A prefix to display on each line.
     * @param nestedPrefix A prefix to display on each nested line.
     * @param length The length at which to wrap the emitted lines.
     * @param values The values which are to be displayed.
     */
    public static void display(String m,
        String prefix, String nestedPrefix, int length,
        Map<String, ? extends Collection<String>> values,
        StringBuilder builder) {
        
        values.forEach( (String featureName, Collection<String> featureValues) -> {
           display(m, prefix, featureName, builder);
           display(m, nestedPrefix, length, featureValues, builder);
        });
    }
        
    /**
     * Display values as a comma-delimited list, with values split across lines
     * at the specified length.
     *
     * @param m      The method requesting the display.
     * @param prefix A prefix to display on each line.
     * @param length The length at which to wrap the emitted lines.
     * @param values The values which are to be displayed.
     */
    public static void display(String m, String prefix, int length, Collection<String> values, StringBuilder builder) {
        int valuesOnLine = 0;

        int numValues = values.size();
        int valueNo = 0;

        for (String value : values) {
            if (valuesOnLine > 0) {
                builder.append(','); // Always need a comma.

                int spaceNeeded = 1; // Room for a space.
                spaceNeeded += value.length(); // Room for the value.
                if (valueNo < numValues - 1) {
                    spaceNeeded++; // Room for a comma after the value.
                }

                if ((builder.length() + spaceNeeded) > length) {
                    logInfo(m, builder.toString());
                    builder.setLength(0);
                    valuesOnLine = 0;
                } else {
                    builder.append(' ');
                }
            }

            if (valuesOnLine == 0) {
                builder.append(prefix);
            }

            // The first value on each line is added without
            // checking the length.  That allows over-size values
            // to be displayed.

            builder.append(value);
            valuesOnLine++;

            valueNo++;
        }

        if (valuesOnLine > 0) {
            logInfo(m, builder.toString());
            builder.setLength(0);
        }
    }

    public static void display(String m,
                               String head, String middle, String tail,
                               List<String> keys, Map<String, String> values,
                               StringBuilder builder) {
        
        keys.forEach((name) -> {
            builder.append(head);
            builder.append(name);
            builder.append(middle);
            builder.append(values.get(name));
            builder.append(tail);

            logInfo(m, builder.toString());
            
            builder.setLength(0);
        });
    }
    
    public static void display(String m, String head, String tail, StringBuilder builder) {
        builder.append(head);
        builder.append(tail);
        
        logInfo(m, builder.toString());
        
        builder.setLength(0);        
    }

    public static void logBanner(String m) {
        logInfo(m, "**************************************************************************");
    }   
}
