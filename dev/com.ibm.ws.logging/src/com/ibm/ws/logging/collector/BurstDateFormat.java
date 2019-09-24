/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.collector;

import java.text.DateFormat.Field;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Formats dates that are relatively close to one another.
 * Leverages the fact that the majority of the Date does not change when formats are milliseconds apart.
 * Calls format again when the seconds change. (To avoid tracking DST).
 *
 * Not thread-safe, use ThreadLocal
 */
public class BurstDateFormat {

    private final SimpleDateFormat formatter;

    /**
     * Reference timestamp
     */
    private long refTimestamp = 0;

    /**
     * Reference beginning of the date
     */
    private String refBeginning;

    /**
     * Reference ending of the date
     */
    private String refEnding;

    /**
     * Reference millisecond value
     */
    private long refMilli = 0;

    /**
     * Upper range for the timestamp difference before reformatting
     */
    private long pdiff = 0;

    /**
     * Lower range for the timestamp difference before reformatting
     */
    private long ndiff = 0;

    /**
     * Tracks the position of the milliseconds field
     */
    private final FieldPosition position = new FieldPosition(Field.MILLISECOND);

    /**
     * Tracks whether the format is not valid. If true, the underlying SimpleDateFormat is used
     */
    boolean invalidFormat = false;

    private StringBuffer sb;

    /**
     * Constructs a BurstDateFormat
     *
     * @param formatter
     */
    public BurstDateFormat(SimpleDateFormat formatter) {
        this(formatter, isFormatInvalid(formatter));
    }

    public BurstDateFormat(SimpleDateFormat formatter, boolean isInvalid) {
        this.formatter = formatter;
        this.invalidFormat = isInvalid;
        if (!invalidFormat) {
            sb = new StringBuffer();
        }
    }

    static boolean isFormatInvalid(SimpleDateFormat formatter) {
        /**
         * Setup the date formatter, determine if the format is valid
         */
        FieldPosition position = new FieldPosition(Field.MILLISECOND);
        boolean invalidFormat = false;
        try {
            StringBuffer refTime = new StringBuffer();

            // Get the positions of the millisecond digits
            String pattern = formatter.toLocalizedPattern();
            if (pattern.lastIndexOf('S') - pattern.indexOf('S') != 2) {
                invalidFormat = true;
            }
            formatter.format(new Date().getTime(), refTime, position);

            // Should be redundant check with above but check again
            // just in case some locale expands the milliseconds field
            if (position.getEndIndex() - position.getBeginIndex() != 3) {
                invalidFormat = true;
            }
            String str = refTime.substring(position.getBeginIndex(), position.getEndIndex());

            // Make sure we are using ascii digits (The loop could be unwrapped)
            for (int i = str.length() - 1; i >= 0; --i) {
                char a = str.charAt(i);
                if (a < 48 || a > 57) {
                    invalidFormat = true;
                    break;
                }
            }
        } catch (Exception e) {
            invalidFormat = true;
        }
        return invalidFormat;
    }

    /**
     * Formats a timestamp
     *
     * @param timestamp
     * @return
     */
    public String format(long timestamp) {

        // If the format is unknown, use the default formatter.
        if (invalidFormat) {
            return formatter.format(timestamp);
        }

        try {
            long delta = timestamp - refTimestamp;
            sb.setLength(0);

            // If we need to reformat
            if (delta >= pdiff || delta < ndiff) {
                refTimestamp = timestamp;
                formatter.format(timestamp, sb, position);

                refMilli = Long.parseLong(sb.substring(position.getBeginIndex(), position.getEndIndex()));

                refBeginning = sb.substring(0, position.getBeginIndex());
                refEnding = sb.substring(position.getEndIndex());

                pdiff = 1000 - refMilli;
                ndiff = -refMilli;
                return sb.toString();
            } else {
                long newMilli = delta + refMilli;
                if (newMilli >= 100)
                    sb.append(refBeginning).append(newMilli).append(refEnding);
                else if (newMilli >= 10)
                    sb.append(refBeginning).append('0').append(newMilli).append(refEnding);
                else
                    sb.append(refBeginning).append("00").append(newMilli).append(refEnding);
                return sb.toString();

            }
        } catch (Exception e) {
            // Throw FFDC in case anything goes wrong
            // Still generate the date via the SimpleDateFormat
            invalidFormat = true;
            sb = null;
            return formatter.format(timestamp);
        }
    }

    public SimpleDateFormat getSimpleDateFormat() {
        return formatter;
    }
}
