/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.common.time;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.common.TraceConstants;

public class TimeUtils {

    private static final TraceComponent tc = Tr.register(TimeUtils.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    public static final String YearMonthDateHourMinSecZone = "yyyy-MM-dd'T'HH:mm:ssZ";
    public static final String YearMonthDateHourMinSecMillisZone = "yyyy-MM-dd'T'HH:mm:ss:SSSZ";

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat();

    public TimeUtils() {

    }

    public TimeUtils(String dateFormat) {
        setSimpleDateFormat(dateFormat);
    }

    /**
     * Set the SimpleDateFormat string that should be used to format date strings created by this class.
     * 
     * @param formatString
     */
    @FFDCIgnore(value = { Exception.class })
    public void setSimpleDateFormat(String formatString) {
        if (formatString == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Null format string provided; date format will not be changed");
            }
            return;
        }
        try {
            simpleDateFormat = new SimpleDateFormat(formatString);
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception caught setting date format: " + e);
                Tr.debug(tc, "Date format will not be changed");
            }
        }
    }

    /**
     * Creates a date string in the format specified for this instance of the class.
     * 
     * @param timeMilliseconds
     * @return
     */
    public String createDateString(long timeMilliseconds) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Creating date string based on long value: " + timeMilliseconds);
        }
        return createDateString(new Date(timeMilliseconds));
    }

    /**
     * Creates a date string in the format specified for this instance of the class.
     * 
     * @param date
     * @return
     */
    public String createDateString(Date date) {
        if (date == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Null Date object provided; returning null");
            }
            return null;
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Creating date string based on date: " + date);
        }
        String formatted = simpleDateFormat.format(date);
        return formatted;
    }

}
