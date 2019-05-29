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
     * Subtract an initial identity set from a final identity set.  Populate the
     * receiver with the difference.  A value which is in the initial set but not in
     * the final set is noted as having been removed.  A value which is in the final
     * set but which is not in the final set is noted as having been added.  A value
     * which is in both sets is noted as being unchanged ("still").
     *
     * The two sets must have the same domains.
     *
     * @param finalSet The final set which is to be compared.
     * @param initialSet The initial set which is to be compared.
     */
    void subtract(Map<String, String> finalSet, Map<String, String> initialSet);

    /**
     * Subtract an initial set from a final set.  Populate the receiver with the
     * difference.
     * 
     * Values in the final set must be in the final domain.  Values in the initial set
     * must be in the initial domain.  The initial and final domains may be the same, but
     * are expected to be different.
     *
     * Either set may be null, 
     * See {@link #subtract(Map, Map)} for subtraction details.
     *
     * @param finalSet The final set which is to be compared.
     * @param finalDomain The intern set from which the final set was created.
     * @param initialSet The initial set which is to be compared.
     * @param initialDomain The intern set from which the initial set was created.
     */
    void subtract(Map<String, String> finalSet, Util_InternMap finalDomain,
                  Map<String, String> initialSet, Util_InternMap initialDomain);
}
