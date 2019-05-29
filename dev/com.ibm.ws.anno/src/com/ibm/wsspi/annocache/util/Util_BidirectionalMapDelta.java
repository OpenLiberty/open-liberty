/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.annocache.util;

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
