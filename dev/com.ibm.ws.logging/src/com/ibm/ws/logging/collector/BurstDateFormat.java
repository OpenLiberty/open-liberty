/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
     * Forces a re-format
     */
    private boolean reset = false;

    /**
     * Constructs a BurstDateFormat
     * 
     * @param formatter
     */
    public BurstDateFormat(SimpleDateFormat formatter) {
        this.formatter = formatter;
    }

    /**
     * Apply a pattern to the formatter (only new formats are applied)
     *
     * @param pattern
     */
    public void applyPattern(String pattern) {
        if (!formatter.toPattern().equals(pattern)) {
            formatter.applyPattern(pattern);
            reset = true;
        }
    }

    /**
     * Formats a timestamp
     *
     * @param timestamp
     * @return
     */
    public String format(long timestamp) {
        long delta = timestamp - refTimestamp;

        // If we need to reformat
        if (delta >= pdiff || delta < ndiff || reset) {
            reset = false;
            StringBuffer refTime = new StringBuffer();
            refTimestamp = timestamp;
            formatter.format(timestamp, refTime, position);

            refMilli = Long.parseLong(refTime.substring(position.getBeginIndex(), position.getEndIndex()));
            refBeginning = refTime.substring(0, position.getBeginIndex());
            refEnding = refTime.substring(position.getEndIndex());

            pdiff = 1000 - refMilli;
            ndiff = -refMilli;
            return refTime.toString();
        } else {
            StringBuffer sb = new StringBuffer();
            long newMilli = delta + refMilli;
            if (newMilli >= 100)
                sb.append(refBeginning).append(newMilli).append(refEnding);
            else if (newMilli >= 10)
                sb.append(refBeginning).append('0').append(newMilli).append(refEnding);
            else
                sb.append(refBeginning).append("00").append(newMilli).append(refEnding);
            return sb.toString();

        }
    }
}
