/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.oauth20.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class UTC {
    private static final DateFormat fFormat;
    private static final DateFormat fParser;
    static {
        fFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'", Locale.US);
        fFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        fParser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        fParser.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private static final Object lock = new Object();

    public static Date parse(String time) throws ParseException {
        return parse(fParser, time);
    }

    private static Date parse(DateFormat parser, String src)
            throws ParseException {
        int cc_start;
        int second_end;
        int tz_ind;
        int t_ind;
        String parsed;
        long msec;
        long tz_offset;
        long fraction;

        cc_start = 0;
        if (src.charAt(0) == '+') // SimpleDateFormat can't handle leading +.
            cc_start = 1;
        t_ind = src.indexOf('T');
        if (t_ind < 0)
            throw new ParseException("'T' is needed in a timestamp", 0);
        second_end = src.indexOf('.');
        tz_ind = src.indexOf('-', t_ind);
        if (tz_ind < 0) {
            tz_ind = src.indexOf('+', t_ind);
            if (tz_ind < 0) {
                tz_ind = src.indexOf('Z', t_ind);
                if (tz_ind < 0) { // No timezone
                    tz_ind = src.length();
                }
            }
        }
        tz_offset = parseTimeZone(src, tz_ind);
        fraction = 0;
        if (second_end < 0) {
            second_end = tz_ind;
        } else { // It has '.'
            // for (int i = second_end + 1; i < tz_ind; i++) {
            for (int i = tz_ind - 1; i > second_end; i--) {
                char ch = src.charAt(i);
                if (ch < '0' || '9' < ch)
                    throw new ParseException("Non-digit in fractional part: " + ch, i);
                if (i == tz_ind - 3)
                    fraction += (ch - '0') * 100;
                else if (i == tz_ind - 2)
                    fraction += (ch - '0') * 10;
                else if (i == tz_ind - 1)
                    fraction += ch - '0';
            }
        }

        parsed = src.substring(cc_start, second_end);
        synchronized (lock) {
            msec = parser.parse(parsed).getTime();
        }
        return new Date(msec + fraction + tz_offset);
    }

    private static long parseTimeZone(String src, int ind) throws ParseException {
        int len = src.length();
        int sign = 1;
        int h, m;

        /* If no timezone, we assume UTC */
        if (len <= ind)
            return 0;

        if (src.charAt(ind) == 'Z') {
            if (ind + 1 != len)
                throw new ParseException("Invalid timezone format", ind);
            return 0;
        }

        if (src.indexOf(ind) == '-')
            sign = -1;
        ind++;
        if (len != ind + 5)
            throw new ParseException("Invalid timezone format", ind);
        h = Integer.parseInt(src.substring(ind, ind + 2));
        m = Integer.parseInt(src.substring(ind + 3, ind + 5));
        return sign * (h * 60 + m) * 60L * 1000L;
    }

    public static String format(Date time) {
        String v = null;
        synchronized (lock) {
            v = fFormat.format(time);
        }
        return v;
    }
}
