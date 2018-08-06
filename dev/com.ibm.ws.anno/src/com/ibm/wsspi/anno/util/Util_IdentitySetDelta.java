/*
* IBM Confidential
*
* OCO Source Materials
*
* WLP Copyright IBM Corp. 2014, 2018
*
* The source code for this program is not published or otherwise divested
* of its trade secrets, irrespective of what has been deposited with the
* U.S. Copyright Office.
*/
package com.ibm.wsspi.anno.util;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public interface Util_IdentitySetDelta {

    String getHashText();

    void log(Logger logger);
    void log(PrintWriter writer);
    void log(Util_PrintLogger logger);

    void describe(String prefix, List<String> nonNull);

    //

    Set<String> getAdded();
    boolean isNullAdded();

    Set<String> getRemoved();
    boolean isNullRemoved();

    Set<String> getStill();
    boolean isNullStill();

    boolean isNull();
    boolean isNull(boolean ignoreRemoved);

    //

    /**
     * Subtract an initial map from a final map.  Populate the receiver with
     * the difference.  A value which is in the initial map but not in the final
     * map is noted as having been removed.  A value which is in the final map but
     * which is not in the final map is noted as having been added.  A value which
     * is in both maps is noted as still.
     *
     * The two maps must have the same domains.  If the maps have different domains,
     * no values of the two maps will match, and the sets will compare as having an
     * empty intersection.
     *
     * @param finalMap The final map which is to be compared.
     * @param initialMap The initial Map which is to be compared.
     */
    void subtract(Map<String, String> finalMap, Map<String, String> initialMap);

    /**
     * Subtract an initial map from a final map.  Populate the receiver with the
     * difference.
     * 
     * Values in the final map must be in the final domain.  Values in the initial map
     * must be in the initial domain.  The initial and final domains may be the same, but
     * are expected to be different.
     * 
     * See {@link #subtract(Map, Map)} for subtraction details.
     *
     * @param finalMap The final map which is to be compared.
     * @param finalDomain The intern map from which the final map was created.
     * @param initialMap The initial Map which is to be compared.
     * @param initialDomain The intern map from which the initial map was created.
     */
    void subtract(Map<String, String> finalMap, Util_InternMap finalDomain,
                  Map<String, String> initialMap, Util_InternMap initialDomain);
}
