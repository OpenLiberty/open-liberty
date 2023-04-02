/*******************************************************************************
 * Copyright (c) 1997, 2009 IBM Corporation and others.
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
package com.ibm.ws.fat.util;

/**
 * Convenience class for converting measurements into human-readable
 * strings
 *
 * @author Tim Burns
 *
 */
public class UnitConverter {

    protected final String unit;
    protected long value;
    protected final UnitConverter parent;

    /**
     * Encapsulates single value, and associates the value with a unit.
     * 
     * @param value
     *                  The value to encapsulate
     * @param unit
     *                  The unit of the value
     */
    public UnitConverter(long value, String unit) {
        this.value = value;
        this.unit = unit;
        this.parent = null;
    }

    /**
     * Encapsulates a value that's derived from a parent value in terms of a
     * size factor
     * 
     * @param factor
     *                   The number of parents that can fit in one instance of this
     * @param parent
     *                   The more granular value size
     * @param unit
     *                   The unit of the value
     */
    public UnitConverter(long factor, UnitConverter parent, String unit) {
        this.unit = unit;
        this.parent = parent;
        this.value = this.parent.value / factor;
        this.parent.value = this.parent.value % factor;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        this.appendValue(buffer, false);
        return buffer.toString();
    }

    protected void appendValue(final StringBuilder buffer, final boolean alwaysAppend) {
        boolean nextAlwaysAppend = false;
        if (this.value > 0 || alwaysAppend) {
            if (alwaysAppend) {
                buffer.append(" ");
            }
            buffer.append(this.value).append(" ").append(this.unit);
            if (this.value != 1) {
                buffer.append("s");
            }
            nextAlwaysAppend = true;
        }
        if (this.parent == null) {
            if (!nextAlwaysAppend) {
                buffer.append("Less than 1 ").append(this.unit);
            }
        } else {
            this.parent.appendValue(buffer, nextAlwaysAppend);
        }
    }

    /**
     * Converts a raw number of bytes into a human-readable String. For
     * example:<br>
     * <code>UnitConverter.bytesAsString(423466398)</code><br>
     * returns:<br>
     * <code>403 megabytes 869 kilobytes 414 bytes</code><br>
     * 
     * @param  bytes
     *                   the raw number of bytes to convert
     * @return       a human-readable String representation of the input number of
     *               bytes
     */
    public static String bytesAsString(long bytes) {
        UnitConverter B = new UnitConverter(bytes, "byte");
        UnitConverter KB = new UnitConverter(1024, B, "kilobyte"); // 1024 B per KB
        UnitConverter MB = new UnitConverter(1024, KB, "megabyte"); // 1024 KB per MB
        UnitConverter GB = new UnitConverter(1024, MB, "gigabyte"); // 1024 MB per GB
        UnitConverter TB = new UnitConverter(1024, GB, "terabyte"); // 1024 GB per TB
        UnitConverter PB = new UnitConverter(1024, TB, "petabyte"); // 1024 TB per PB
        return PB.toString();
    }

    /**
     * Converts a raw number of milliseconds into a human-readable String.
     * For example:<br>
     * <code>UnitConverter.millisecondsAsString(System.currentTimeMillis())</code>
     * <br>
     * returns something like:<br>
     * <code>40 years 98 days 14 hours 14 minutes 21 seconds 453 milliseconds</code>
     * <br>
     * 
     * @param  milliseconds
     *                          the raw number of milliseconds to convert
     * @return              a human-readable String representation of the input number of
     *                      milliseconds
     */
    public static String millisecondsAsString(long milliseconds) {
        UnitConverter ms = new UnitConverter(milliseconds, "millisecond");
        UnitConverter s = new UnitConverter(1000, ms, "second"); // 1000 ms per s
        UnitConverter m = new UnitConverter(60, s, "minute"); // 60 s per m
        UnitConverter h = new UnitConverter(60, m, "hour"); // 60 m per h
        UnitConverter d = new UnitConverter(24, h, "day"); // 24 h per d
        UnitConverter y = new UnitConverter(365, d, "year"); // 365 d per y
        return y.toString();
    }

    /**
     * Print a few example conversions to standard out
     * 
     * @param args
     *                 no arguments needed
     */
    public static void main(String[] args) {
        System.out.println(UnitConverter.bytesAsString(423466398));
        System.out.println(UnitConverter.bytesAsString(0));
        System.out.println(UnitConverter.bytesAsString(-1));
        System.out.println(UnitConverter.bytesAsString(1));
        System.out.println(UnitConverter.millisecondsAsString(System.currentTimeMillis()));
        System.out.println(UnitConverter.millisecondsAsString(0));
    }

}
