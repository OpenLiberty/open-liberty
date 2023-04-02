/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * A direct copy of relevant methods from com.ibm.ws.kernel.service
 * (com.ibm.wsspi.kernel.service.utils.MetatypeUtils)
 * to parse ibm:type = duration Strings to longs for ENV and bootstrap variables
 * 
 * When importin MetatypeUtils.java from com.ibm.ws.kernel.service, a
 * circular build dependency is introduced, so this class copy is the workaround.
 */
public class MetatypeUtils {
    static final TraceComponent tc = Tr.register(MetatypeUtils.class);

    private static final Map<String, TimeUnit> UNIT_DESCRIPTORS;
    static {
        HashMap<String, TimeUnit> units = new HashMap<String, TimeUnit>();

        // We used to allow the duration abbreviations to be translated, but
        // that causes config to be non-portable, so we now recommend that
        // customers use English abbreviations only.  We hardcode the previously
        // translated abbreviations for compatibility.  Additional translations
        // should not be added.

        units.put("d", TimeUnit.DAYS); // en, es, ja, ko, pl, pt_BR, zh
        units.put("dn\u016f", TimeUnit.DAYS); // cs
        units.put("g", TimeUnit.DAYS); // it
        units.put("j", TimeUnit.DAYS); // fr
        units.put("n", TimeUnit.DAYS); // hu
        units.put("t", TimeUnit.DAYS); // de
        units.put("z", TimeUnit.DAYS); // ro
        units.put("\u0434", TimeUnit.DAYS); // ru
        units.put("\u5929", TimeUnit.DAYS); // zh_TW

        //units.put("g", TimeUnit.HOURS); // pl - conflicts with DAYS for "it"
        units.put("h", TimeUnit.HOURS); // en, de, es, fr, it, ja, ko, pt_BR, ro, zh
        units.put("hod", TimeUnit.HOURS); // cs
        units.put("\u00f3", TimeUnit.HOURS); // hu
        units.put("\u0447", TimeUnit.HOURS); // ru
        units.put("\u5c0f\u6642", TimeUnit.HOURS); // zh_TW

        units.put("m", TimeUnit.MINUTES); // en, de, es, fr, hu, it, ja, ko, pl, pt_BR, ro, zh
        units.put("min", TimeUnit.MINUTES); // cs
        units.put("\u043c", TimeUnit.MINUTES); // ru
        units.put("\u5206", TimeUnit.MINUTES); // zh_TW

        units.put("e", TimeUnit.SECONDS); // pt_BR
        units.put("mp", TimeUnit.SECONDS); // hu
        units.put("s", TimeUnit.SECONDS); // en, cs, de, es, fr, it, ja, ko, pl, ro, zh
        units.put("\u0441", TimeUnit.SECONDS); // ru
        units.put("\u79d2", TimeUnit.SECONDS); // zh_TW

        units.put("ms", TimeUnit.MILLISECONDS); // en, cs, de, es, fr, hu, it, ja, ko, pl, pt_BR, ro, zh
        units.put("\u043c\u0441", TimeUnit.MILLISECONDS); // ru
        units.put("\u6beb\u79d2", TimeUnit.MILLISECONDS); // zh_TW

        UNIT_DESCRIPTORS = Collections.unmodifiableMap(units);
    }

    private final static Pattern INTERVAL_STRING = Pattern.compile("(\\d+)(\\D+)");

    /**
     * Converts a string value representing a unit of time into a Long value.
     * 
     * @param strVal
     *            A String representing a unit of time.
     * @param unit
     *            The unit of time that the string value should be converted into
     * @return Long The value of the string in the desired time unit
     */
    @FFDCIgnore(NumberFormatException.class)
    public static Long evaluateDuration(String strVal, TimeUnit endUnit) {
        // If the value is a number, simply return the numeric value as a long
        try {
            return Long.valueOf(strVal);
        } catch (NumberFormatException ex) {
            // ignore
        }

        // Otherwise, parse the duration with unit descriptors.
        return evaluateDuration​(strVal, endUnit, UNIT_DESCRIPTORS);
    }

    private static Long evaluateDuration​(String strVal, TimeUnit endUnit, Map<String, TimeUnit> unitDescriptors) {
        Matcher m = INTERVAL_STRING.matcher(strVal);
        long retVal = 0;
        boolean somethingParsed = false;
        while (m.find()) {
            somethingParsed = true;
            // either of these could throw it's own Illegal argument exception
            // if one of the component parts is bad.
            Long numberVal = Long.valueOf(m.group(1));
            String unitStr = m.group(2);
            if (unitStr == null) {
                throw new IllegalArgumentException("Could not parse configuration value as a duration: " + strVal);
            }
            TimeUnit sourceUnit = unitDescriptors.get(unitStr.trim().toLowerCase());
            if (sourceUnit == null) {
                throw new IllegalArgumentException("Could not parse configuration value as a duration: " + strVal);
            }
            retVal += endUnit.convert(numberVal, sourceUnit);
        }

        if (!somethingParsed) {
            throw new IllegalArgumentException("Could not parse configuration value as a duration: " + strVal);
        }

        return retVal;
    }
}
