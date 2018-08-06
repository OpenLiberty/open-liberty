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
import java.util.logging.Logger;

public interface Util_BidirectionalMapDelta {
    String getHashText();
    
    void log(Logger logger);
    void log(PrintWriter writer);
    void log(Util_PrintLogger logger);

    String getHolderTag();
    String getHeldTag();

    void describe(String prefix, List<String> nonNull);

    //

    Util_Factory getFactory();

    //

    Util_BidirectionalMap getAddedMap();
    boolean isNullAdded();

    Util_BidirectionalMap getRemovedMap();
    boolean isNullRemoved();

    Util_BidirectionalMap getStillMap();
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
     * If the holder and held intern maps of the two bi-directional maps not the same,
     * conversions are performed when subtracting the bi-directional maps.
     *
     * @param finalMap The final map which is to be compared.
     * @param initialMap The initial Map which is to be compared.
     */
    void subtract(Util_BidirectionalMap finalMap, Util_BidirectionalMap initialMap);
}
