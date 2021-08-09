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
package com.ibm.ws.http.internal;

import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.genericbnf.internal.GenericUtils;
import com.ibm.wsspi.http.HttpDateFormat;

/**
 * Class to handle formatting and parsing of dates in the various allowed
 * HTTP formats.
 * <br>
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM" })
public class HttpDateFormatImpl implements HttpDateFormat {

    /** RAS trace variable */
    private static final TraceComponent tc = Tr.register(HttpDateFormatImpl.class);

    /**
     * Other classes within the package obtain this service: they must never be null,
     * and can easily outlive the lifecycle of any one HttpDispatcher instance.
     * Use a static class holder to defer loading as long as possible.. though
     * the class will be instantiated when a dispatcher is activated.
     */
    private static class HttpDateFormatHolder {
        private static HttpDateFormat dateFormatSvc = new HttpDateFormatImpl();
    }

    public static HttpDateFormat getInstance() {
        return HttpDateFormatHolder.dateFormatSvc;
    }

    /** Thread local storage of format wrapper class */
    private static final ThreadLocal<HttpLocalFormat> threadStorage = new ThreadLocal<HttpLocalFormat>();

    /**
     * Get access to the format wrapper class that is local to this particular
     * worker thread.
     * <br>
     * 
     * @return HttpLocalFormat
     */
    private HttpLocalFormat getFormat() {
        HttpLocalFormat format = threadStorage.get();
        if (null == format) {
            format = new HttpLocalFormat();
            threadStorage.set(format);
        }
        return format;
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#getRFC1123TimeAsBytes()
     */
    @Override
    public byte[] getRFC1123TimeAsBytes() {
        return getFormat().get1123TimeAsBytes(0L);
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#getRFC1123TimeAsBytes(long)
     */
    @Override
    public byte[] getRFC1123TimeAsBytes(long range) {
        return getFormat().get1123TimeAsBytes(range);
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#getRFC1123Time()
     */
    @Override
    public String getRFC1123Time() {
        return getFormat().get1123TimeAsString(0L);
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#getRFC1123Time(long)
     */
    @Override
    public String getRFC1123Time(long range) {
        return getFormat().get1123TimeAsString(range);
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#getRFC1123Time(java.util.Date)
     */
    @Override
    public String getRFC1123Time(Date inDate) {
        return getFormat().get1123Format().format(inDate);
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#getRFC1036TimeAsBytes()
     */
    @Override
    public byte[] getRFC1036TimeAsBytes() {
        return getFormat().get1036TimeAsBytes(0L);
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#getRFC1036TimeAsBytes(long)
     */
    @Override
    public byte[] getRFC1036TimeAsBytes(long range) {
        return getFormat().get1036TimeAsBytes(range);
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#getRFC1036Time()
     */
    @Override
    public String getRFC1036Time() {
        return getFormat().get1036TimeAsString(0L);
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#getRFC1036Time(long)
     */
    @Override
    public String getRFC1036Time(long range) {
        return getFormat().get1036TimeAsString(range);
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#getRFC1036Time(java.util.Date)
     */
    @Override
    public String getRFC1036Time(Date inDate) {
        return getFormat().get1036Format().format(inDate);
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#getRFC2109TimeAsBytes()
     */
    @Override
    public byte[] getRFC2109TimeAsBytes() {
        return getFormat().get2109TimeAsBytes(0L);
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#getRFC2109TimeAsBytes(long)
     */
    @Override
    public byte[] getRFC2109TimeAsBytes(long range) {
        return getFormat().get2109TimeAsBytes(range);
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#getRFC2109Time()
     */
    @Override
    public String getRFC2109Time() {
        return getFormat().get2109TimeAsString(0L);
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#getRFC2109Time(long)
     */
    @Override
    public String getRFC2109Time(long range) {
        return getFormat().get2109TimeAsString(range);
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#getRFC2109Time(java.util.Date)
     */
    @Override
    public String getRFC2109Time(Date inDate) {
        return getFormat().get2109Format().format(inDate);
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#getASCIITimeAsBytes()
     */
    @Override
    public byte[] getASCIITimeAsBytes() {
        return getFormat().getAsciiTimeAsBytes(0L);
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#getASCIITimeAsBytes(long)
     */
    @Override
    public byte[] getASCIITimeAsBytes(long range) {
        return getFormat().getAsciiTimeAsBytes(range);
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#getASCIITime()
     */
    @Override
    public String getASCIITime() {
        return getFormat().getAsciiTimeAsString(0L);
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#getASCIITime(long)
     */
    @Override
    public String getASCIITime(long range) {
        return getFormat().getAsciiTimeAsString(range);
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#getASCIITime(java.util.Date)
     */
    @Override
    public String getASCIITime(Date inDate) {
        return getFormat().getAsciiFormat().format(inDate);
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#getNCSATimeAsBytes()
     */
    @Override
    public byte[] getNCSATimeAsBytes() {
        return getFormat().getNCSATimeAsBytes(0L);
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#getNCSATimeAsBytes(long)
     */
    @Override
    public byte[] getNCSATimeAsBytes(long range) {
        return getFormat().getNCSATimeAsBytes(range);
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#getNCSATime()
     */
    @Override
    public String getNCSATime() {
        return getFormat().getNCSATimeAsString(0L);
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#getNCSATime(long)
     */
    @Override
    public String getNCSATime(long range) {
        return getFormat().getNCSATimeAsString(range);
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#getNCSATime(java.util.Date)
     */
    @Override
    public String getNCSATime(Date inDate) {
        return getFormat().getNCSAFormat().format(inDate);
    }

    /**
     * Parse the input value against the formatter but do not throw an exception
     * if it fails to match, instead just return null.
     * <br>
     * 
     * @param format
     * @param input
     * @return Date
     */
    private Date attemptParse(SimpleDateFormat format, String input) {
        ParsePosition pos = new ParsePosition(0);
        Date d = format.parse(input, pos);
        if (0 == pos.getIndex() || pos.getIndex() != input.length()) {
            // invalid format matching
            return null;
        }
        return d;
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#parseRFC1123Time(java.lang.String)
     */
    @Override
    public Date parseRFC1123Time(String input) throws ParseException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "rfc1123 parsing [" + input + "]");
        }
        Date d = attemptParse(getFormat().get1123Parse(), input);
        if (null == d) {
            throw new ParseException("Unparseable [" + input + "]", 0);
        }
        return d;
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#parseRFC1036Time(java.lang.String)
     */
    @Override
    public Date parseRFC1036Time(String input) throws ParseException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "rfc1036 parsing [" + input + "]");
        }
        Date d = attemptParse(getFormat().get1036Parse(), input);
        if (null == d) {
            throw new ParseException("Unparseable [" + input + "]", 0);
        }
        return d;
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#parseRFC2109Time(java.lang.String)
     */
    @Override
    public Date parseRFC2109Time(String input) throws ParseException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "rfc2109 parsing [" + input + "]");
        }
        Date d = attemptParse(getFormat().get2109Parse(), input);
        if (null == d) {
            throw new ParseException("Unparseable [" + input + "]", 0);
        }
        return d;
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#parseASCIITime(java.lang.String)
     */
    @Override
    public Date parseASCIITime(String input) throws ParseException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "ascii parsing [" + input + "]");
        }
        Date d = attemptParse(getFormat().getAsciiParse(), input);
        if (null == d) {
            throw new ParseException("Unparseable [" + input + "]", 0);
        }
        return d;
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#parseTime(java.lang.String)
     */
    @Override
    public Date parseTime(String input) throws ParseException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "parseTime parsing [" + input + "]");
        }
        String data = input;
        int i = data.indexOf(';', 0);
        // PK20062 - check for excess data following the date value
        if (-1 != i) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Ignoring excess data following semi-colon in date");
            }
            // strip off trailing whitespace before semi-colon
            for (; i > 20; i--) {
                char c = data.charAt(i - 1);
                if (' ' != c && '\t' != c) {
                    break;
                }
            }
            if (20 >= i) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Not enough data left to make a valid date");
                }
                throw new ParseException("Invalid date [" + input + "]", 0);
            }
            data = input.substring(0, i);
        }

        Date parsedDate = attemptParse(getFormat().get1123Parse(), data);
        if (null == parsedDate) {
            parsedDate = attemptParse(getFormat().get1036Parse(), data);
            if (null == parsedDate) {
                parsedDate = attemptParse(getFormat().getAsciiParse(), data);
                if (null == parsedDate) {
                    parsedDate = attemptParse(getFormat().get2109Parse(), data);
                    if (null == parsedDate) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Time does not match supported formats");
                        }
                        throw new ParseException("Unparseable [" + data + "]", 0);
                    }
                }
            }
        }
        return parsedDate;
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#parseTime(byte[])
     */
    @Override
    public Date parseTime(byte[] inBytes) throws ParseException {
        return parseTime(GenericUtils.getEnglishString(inBytes));
    }

}
