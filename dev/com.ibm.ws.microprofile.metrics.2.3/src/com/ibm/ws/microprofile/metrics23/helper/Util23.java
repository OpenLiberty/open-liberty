/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.metrics23.helper;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.metrics.SimpleTimer;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.metrics.Constants;
import com.ibm.ws.microprofile.metrics.helper.Util;

/**
 *
 */
public class Util23 extends Util {
    private static final TraceComponent tc = Tr.register(Util23.class);

    public static Map<String, Number> getSimpleTimerNumbers(SimpleTimer simpleTimer, String tags, double conversionFactor) {
        Map<String, Number> results = new HashMap<String, Number>();
        results.put(Constants.COUNT + tags, simpleTimer.getCount());
        results.put("elapsedTime" + tags, simpleTimer.getElapsedTime().toNanos());
        return results;
    }

}
