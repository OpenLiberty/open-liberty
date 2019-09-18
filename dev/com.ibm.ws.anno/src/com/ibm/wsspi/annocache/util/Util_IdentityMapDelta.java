/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
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
import java.util.Map;
import java.util.logging.Logger;

public interface Util_IdentityMapDelta {

    String getHashText();

    void log(Logger logger);
    void log(PrintWriter writer);
    void log(Util_PrintLogger logger);

    void describe(String prefix, List<String> nonNull);

    //

    Map<String, String> getAddedMap();
    boolean isNullAdded();

    Map<String, String> getRemovedMap();
    boolean isNullRemoved();

    int FINAL_VALUE_OFFSET = 0;
    int INITIAL_VALUE_OFFSET = 1;

    Map<String, String[]> getChangedMap();
    boolean isNullChanged();

    Map<String, String> getStillMap();
    boolean isNullUnchanged();

    boolean isNull();

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
    void subtract(
        Map<String, String> finalMap, Util_InternMap finalDomain,
        Map<String, String> initialMap, Util_InternMap initialDomain);
}
