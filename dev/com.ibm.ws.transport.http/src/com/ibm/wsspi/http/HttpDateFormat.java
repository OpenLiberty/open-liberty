/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.http;

import java.text.ParseException;
import java.util.Date;

/**
 * Class to handle formatting and parsing of dates in the various allowed
 * HTTP formats.
 * <br>
 */
public interface HttpDateFormat {

    /**
     * Get the current time formatted for RFC 1123.
     * <br>
     * EEE, dd MMM yyyy HH:mm:ss z
     * <br>
     * 
     * @return byte[]
     */
    byte[] getRFC1123TimeAsBytes();

    /**
     * Get the time formatted for RFC 1123, this will use a cached format value
     * if that value is within the input range of the current time.
     * <br>
     * EEE, dd MMM yyyy HH:mm:ss z
     * <br>
     * 
     * @param range (milliseconds, -1 means use default, 0 means right now)
     * @return byte[]
     */
    byte[] getRFC1123TimeAsBytes(long range);

    /**
     * Get an RFC1123 compliant date string based on the current time.
     * <br>
     * EEE, dd MMM yyyy HH:mm:ss z
     * <br>
     * 
     * @return String
     */
    String getRFC1123Time();

    /**
     * Get the time formatted for RFC 1123, this will use a cached format value
     * if that value is within the input range of the current time.
     * <br>
     * EEE, dd MMM yyyy HH:mm:ss z
     * <br>
     * 
     * @param range (milliseconds, -1 means use default, 0 means right now)
     * @return String
     */
    String getRFC1123Time(long range);

    /**
     * Get an RFC1123 compliant date string based on the given Date object.
     * <br>
     * 
     * @param inDate
     * @return String
     */
    String getRFC1123Time(Date inDate);

    /**
     * Get the current time formatted for RFC 1036.
     * <br>
     * EEEEEEEEE, dd-MMM-yy HH:mm:ss z
     * <br>
     * 
     * @return byte[]
     */
    byte[] getRFC1036TimeAsBytes();

    /**
     * Get the current time formatted for RFC 1036, this will use a cached
     * format value if that value is within the input range of the current time.
     * <br>
     * EEEEEEEEE, dd-MMM-yy HH:mm:ss z
     * <br>
     * 
     * @param range (milliseconds, -1 means use default, 0 means right now)
     * @return byte[]
     */
    byte[] getRFC1036TimeAsBytes(long range);

    /**
     * Get the current time formatted for RFC 1036.
     * <br>
     * EEEEEEEEE, dd-MMM-yy HH:mm:ss z
     * <br>
     * 
     * @return String
     */
    String getRFC1036Time();

    /**
     * Get the current time formatted for RFC 1036, this will use a cached
     * format value if that value is within the input range of the current time.
     * <br>
     * EEEEEEEEE, dd-MMM-yy HH:mm:ss z
     * <br>
     * 
     * @param range (milliseconds, -1 means use default, 0 means right now)
     * @return String
     */
    String getRFC1036Time(long range);

    /**
     * Get an RFC1036 compliant date string based on the given Date object.
     * <br>
     * 
     * @param inDate
     * @return String
     */
    String getRFC1036Time(Date inDate);

    /**
     * Get the current time formatted for RFC 2109.
     * <br>
     * EEE, dd-MMM-YY HH:mm:ss GMT
     * <br>
     * 
     * @return byte[]
     */
    byte[] getRFC2109TimeAsBytes();

    /**
     * Get the current time formatted for RFC 2109, this will use a cached
     * format value if that value is within the input range of the current time.
     * <br>
     * EEE, dd-MMM-YY HH:mm:ss GMT
     * <br>
     * 
     * @param range (milliseconds, -1 means use default, 0 means right now)
     * @return byte[]
     */
    byte[] getRFC2109TimeAsBytes(long range);

    /**
     * Get the current time formatted for RFC 2109.
     * <br>
     * EEE, dd-MMM-YY HH:mm:ss GMT
     * <br>
     * 
     * @return String
     */
    String getRFC2109Time();

    /**
     * Get the current time formatted for RFC 2109, this will use a cached
     * format value if that value is within the input range of the current time.
     * <br>
     * EEE, dd-MMM-YY HH:mm:ss GMT
     * <br>
     * 
     * @param range (milliseconds, -1 means use default, 0 means right now)
     * @return String
     */
    String getRFC2109Time(long range);

    /**
     * Get an RFC2109 compliant date string based on the given Date object.
     * <br>
     * 
     * @param inDate
     * @return String
     */
    String getRFC2109Time(Date inDate);

    /**
     * Get the current time formatted for standard ASCII.
     * <br>
     * EEE MMM d HH:mm:ss yyyy
     * <br>
     * 
     * @return byte[]
     */
    byte[] getASCIITimeAsBytes();

    /**
     * Get the current time formatted for standard ASCII, this will use a cached
     * format value if that value is within the input range of the current time.
     * <br>
     * EEE MMM d HH:mm:ss yyyy
     * <br>
     * 
     * @param range (milliseconds, -1 means use default, 0 means right now)
     * @return byte[]
     */
    byte[] getASCIITimeAsBytes(long range);

    /**
     * Get the current time formatted for standard ASCII.
     * <br>
     * EEE MMM d HH:mm:ss yyyy
     * <br>
     * 
     * @return String
     */
    String getASCIITime();

    /**
     * Get the current time formatted for standard ASCII, this will use a cached
     * format value if that value is within the input range of the current time.
     * <br>
     * EEE MMM d HH:mm:ss yyyy
     * <br>
     * 
     * @param range (milliseconds, -1 means use default, 0 means right now)
     * @return String
     */
    String getASCIITime(long range);

    /**
     * Get an ASCII complaint date string based on the given Date object.
     * <br>
     * 
     * @param inDate
     * @return String
     */
    String getASCIITime(Date inDate);

    /**
     * Get the current time formatted for NCSA.
     * <br>
     * dd/MMM/yyyy:HH:mm:ss Z
     * <br>
     * 
     * @return byte[]
     */
    byte[] getNCSATimeAsBytes();

    /**
     * Get the current time formatted for NCSA, this will use a cached
     * format value if that value is within the input range of the current time.
     * <br>
     * dd/MMM/yyyy:HH:mm:ss Z
     * <br>
     * 
     * @param range (milliseconds, -1 means use default, 0 means right now)
     * @return byte[]
     */
    byte[] getNCSATimeAsBytes(long range);

    /**
     * Get the current time formatted for NCSA.
     * <br>
     * dd/MMM/yyyy:HH:mm:ss Z
     * <br>
     * 
     * @return String
     */
    String getNCSATime();

    /**
     * Get the current time formatted for NCSA, this will use a cached
     * format value if that value is within the input range of the current time.
     * <br>
     * dd/MMM/yyyy:HH:mm:ss Z
     * <br>
     * 
     * @param range (milliseconds, -1 means use default, 0 means right now)
     * @return String
     */
    String getNCSATime(long range);

    /**
     * Format the given Date object in the NCSA format.
     * <br>
     * 
     * @param inDate
     * @return String
     */
    String getNCSATime(Date inDate);

    /**
     * Parse in the input string into an RFC1123 Date object.
     * <br>
     * 
     * @param input
     * @return Date
     * @throws ParseException
     */
    Date parseRFC1123Time(String input) throws ParseException;

    /**
     * Parse the input string into an RFC1036 Date object.
     * <br>
     * 
     * @param input
     * @return Date
     * @throws ParseException
     */
    Date parseRFC1036Time(String input) throws ParseException;

    /**
     * Parse the input string into an RFC2109 Date object.
     * <br>
     * 
     * @param input
     * @return Date
     * @throws ParseException
     */
    Date parseRFC2109Time(String input) throws ParseException;

    /**
     * Parse the input string into an ASCII Date object.
     * <br>
     * 
     * @param input
     * @return Date
     * @throws ParseException
     */
    Date parseASCIITime(String input) throws ParseException;

    /**
     * Parse the input data into the matching Date format. It will try
     * RFC1123, then RFC1036, and finally ASCII until it finds a match, or
     * it throws a ParseException if there was no match.
     * <br>
     * 
     * @param input
     * @return Date
     * @throws ParseException
     */
    Date parseTime(String input) throws ParseException;

    /**
     * Parse the input data into the matching Date format. It will try
     * RFC1123, then RFC1036, and finally ASCII until it finds a match, or
     * it throws a ParseException if there was no match.
     * <br>
     * 
     * @param inBytes
     * @return Date
     * @throws ParseException
     */
    Date parseTime(byte[] inBytes) throws ParseException;

}
