/*******************************************************************************
 * Copyright (c) 2009, 2025 IBM Corporation and others.
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
package com.ibm.ws.http.internal;

import java.text.ParseException;
import java.text.ParsePosition;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.genericbnf.internal.GenericUtils;
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
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

    /** Cached RFC 1123 format timer */
    private final CachedTime c1123Time = new CachedTime("EEE, dd MMM uuuu HH:mm:ss z", true);
    /** Cached RFC 1036 format timer */
    private final CachedTime c1036Time = new CachedTime("EEEE, dd-MMM-uu HH:mm:ss z", true);
    /** Cached ASCII format timer */
    private final CachedTime cAsciiTime = new CachedTime("EEE MMM  d HH:mm:ss uuuu", true);
    /** Cached NCSA format timer */
    private final CachedTime cNCSATime = new CachedTime("dd/MMM/uuuu:HH:mm:ss Z", false);
    /** Cached RFC 2109 format timer */
    private final CachedTime c2109Time = new CachedTime("EEE, dd-MMM-uu HH:mm:ss z", true);

    private static class CachedFormattedTime {
        final long timeInMilliseconds;
        final long timeWithStrippedMillis;
        final String formattedTimeString;
        volatile byte[] bytes = null;

        CachedFormattedTime(long time, long timeWithoutMillis, String formattedString) {
            timeInMilliseconds = time;
            timeWithStrippedMillis = timeWithoutMillis;
            formattedTimeString = formattedString;
        }

        byte[] getBytes() {
            if (bytes == null) {
                byte[] ba = new byte[formattedTimeString.length()];
                for (int i = 0, length = ba.length; i < length; ++i) {
                    ba[i] = (byte) formattedTimeString.charAt(i);
                }
                bytes = ba;
            }
            return bytes;
        }
    }

    /**
     * Private class that wraps handling a specific date formatter and keeping
     * a stored formatted byte[]. If the current time is within the target
     * tolerance in milliseconds of the stored time, then the previous formatted
     * byte[] is returned, otherwise the current time is formatted and used for
     * the next tolerance range of time. This class is used at the threadlocal
     * level so no synchronization is required.
     *
     */
    private static class CachedTime {

        /** Ref to the GMT timezone */
        static final ZoneId gmt = ZoneId.of("GMT");

        static final Set<TemporalField> resolverFields;
        static {
            Set<TemporalField> fields = new HashSet<>();
            fields.add(ChronoField.YEAR);
            fields.add(ChronoField.MONTH_OF_YEAR);
            fields.add(ChronoField.DAY_OF_MONTH);
            fields.add(ChronoField.HOUR_OF_DAY);
            fields.add(ChronoField.MINUTE_OF_HOUR);
            fields.add(ChronoField.SECOND_OF_MINUTE);
            fields.add(ChronoField.INSTANT_SECONDS);
            fields.add(ChronoField.NANO_OF_SECOND);
            resolverFields = Collections.unmodifiableSet(fields);
        }

        private final AtomicReference<CachedFormattedTime> cachedTime = new AtomicReference<>();

        /** Stored formatter */
        final DateTimeFormatter formatter;

        /**
         * Create a cachedTime instance with the given format.
         * <br>
         *
         * @param pattern
         */
        CachedTime(String pattern, boolean gmtTimeZone) {
            DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder().parseCaseInsensitive().parseLenient().appendPattern(pattern);
            DateTimeFormatter dateFormatter = builder.toFormatter(Locale.US).withZone(gmtTimeZone ? gmt : ZoneId.systemDefault()).withResolverFields(resolverFields);
            formatter = dateFormatter;
        }

        /**
         * Utility method to get the current time and then call checkTime to determine whether
         * to use the cached time value or update to a newly formatted timestamp.
         *
         * @param tolerance
         */
        private CachedFormattedTime updateTime(long tolerance) {
            long now = HttpDispatcher.getApproxTime();
            return checkTime(now, tolerance, true);
        }

        /**
         * Utility method to determine whether to use the cached time value or
         * update to a newly formatted timestamp.
         *
         * @param time      time to be formatted
         * @param tolerance tolerance of timestamp from cached value
         * @param doUpdate  whether to update the cached version or not
         */
        private CachedFormattedTime checkTime(long time, long tolerance, boolean doUpdate) {

            // We only care about seconds, so remove the milliseconds from the time.
            long strippedMillis = time - (time % 1000);

            final CachedFormattedTime cachedFormattedTime = cachedTime.get();
            if (cachedFormattedTime != null) {
                if (strippedMillis == cachedFormattedTime.timeWithStrippedMillis) {
                    return cachedFormattedTime;
                }
                if (tolerance > 1000L) {
                    if ((time - cachedFormattedTime.timeInMilliseconds) <= tolerance) {
                        return cachedFormattedTime;
                    }
                }
            }

            // otherwise need to format the current time
            String sTime = formatter.format(Instant.ofEpochMilli(strippedMillis));

            CachedFormattedTime newCachedFormattedTime = new CachedFormattedTime(time, strippedMillis, sTime);

            if (doUpdate) {
                // Only update it if another thread hasn't already updated it.
                cachedTime.compareAndSet(cachedFormattedTime, newCachedFormattedTime);
            }

            return newCachedFormattedTime;
        }

        /**
         * Get a formatted version of the time as a byte[]. The input range is
         * the allowed difference in time from the cached snapshot that the
         * caller is willing to use. If that range is exceeded, then a new
         * snapshot is taken and formatted.
         * <br>
         *
         * @param tolerance -- milliseconds, -1 means use default 1000ms, a 0
         *                      means that this must be an exact match in time
         * @return byte[]
         */
        byte[] getTimeAsBytes(long tolerance) {
            return updateTime(tolerance).getBytes();
        }

        /**
         * Get a formatted version of the time as a String. The input range is
         * the allowed difference in time from the cached snapshot that the
         * caller is willing to use. If that range is exceeded, then a new
         * snapshot is taken and formatted.
         * <br>
         *
         * @param tolerance -- milliseconds, -1 means use default 1000ms, a 0
         *                      means that this must be an exact match in time
         * @return String
         */
        String getTimeAsString(long tolerance) {
            return updateTime(tolerance).formattedTimeString;
        }

        /**
         * Get a formatted version of the time as a String. The input range is
         * the allowed difference in time from the cached snapshot that the
         * caller is willing to use. If that range is exceeded, then a new
         * snapshot is taken and formatted.
         * <br>
         *
         * @param time          time to be formatted
         * @param tolerance     milliseconds, -1 means use default 1000ms, a 0
         *                          means that this must be an exact match in time
         * @param isCurrentTime if the time specified is the current time
         * @return String
         */
        String getTimeAsString(long time, long tolerance, boolean isCurrentTime) {
            return checkTime(time, tolerance, isCurrentTime).formattedTimeString;
        }

    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#getRFC1123TimeAsBytes()
     */
    @Override
    public byte[] getRFC1123TimeAsBytes() {
        return c1123Time.getTimeAsBytes(0L);
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#getRFC1123TimeAsBytes(long)
     */
    @Override
    public byte[] getRFC1123TimeAsBytes(long range) {
        return c1123Time.getTimeAsBytes(range);
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#getRFC1123Time()
     */
    @Override
    public String getRFC1123Time() {
        return c1123Time.getTimeAsString(0L);
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#getRFC1123Time(long)
     */
    @Override
    public String getRFC1123Time(long range) {
        return c1123Time.getTimeAsString(range);
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#getRFC1123Time(java.util.Date)
     */
    @Override
    public String getRFC1123Time(Date inDate) {
        return c1123Time.formatter.format(inDate.toInstant());
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#getRFC1036TimeAsBytes()
     */
    @Override
    public byte[] getRFC1036TimeAsBytes() {
        return c1036Time.getTimeAsBytes(0L);
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#getRFC1036TimeAsBytes(long)
     */
    @Override
    public byte[] getRFC1036TimeAsBytes(long range) {
        return c1036Time.getTimeAsBytes(range);
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#getRFC1036Time()
     */
    @Override
    public String getRFC1036Time() {
        return c1036Time.getTimeAsString(0L);
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#getRFC1036Time(long)
     */
    @Override
    public String getRFC1036Time(long range) {
        return c1036Time.getTimeAsString(range);
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#getRFC1036Time(java.util.Date)
     */
    @Override
    public String getRFC1036Time(Date inDate) {
        return c1036Time.formatter.format(inDate.toInstant());
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#getRFC2109TimeAsBytes()
     */
    @Override
    public byte[] getRFC2109TimeAsBytes() {
        return c2109Time.getTimeAsBytes(0L);
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#getRFC2109TimeAsBytes(long)
     */
    @Override
    public byte[] getRFC2109TimeAsBytes(long range) {
        return c2109Time.getTimeAsBytes(range);
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#getRFC2109Time()
     */
    @Override
    public String getRFC2109Time() {
        return c2109Time.getTimeAsString(0L);
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#getRFC2109Time(long)
     */
    @Override
    public String getRFC2109Time(long range) {
        return c2109Time.getTimeAsString(range);
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#getRFC2109Time(java.util.Date)
     */
    @Override
    public String getRFC2109Time(Date inDate) {
        return c2109Time.formatter.format(inDate.toInstant());
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#getASCIITimeAsBytes()
     */
    @Override
    public byte[] getASCIITimeAsBytes() {
        return cAsciiTime.getTimeAsBytes(0L);
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#getASCIITimeAsBytes(long)
     */
    @Override
    public byte[] getASCIITimeAsBytes(long range) {
        return cAsciiTime.getTimeAsBytes(range);
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#getASCIITime()
     */
    @Override
    public String getASCIITime() {
        return cAsciiTime.getTimeAsString(0L);
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#getASCIITime(long)
     */
    @Override
    public String getASCIITime(long range) {
        return cAsciiTime.getTimeAsString(range);
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#getASCIITime(java.util.Date)
     */
    @Override
    public String getASCIITime(Date inDate) {
        return cAsciiTime.formatter.format(inDate.toInstant());
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#getNCSATimeAsBytes()
     */
    @Override
    public byte[] getNCSATimeAsBytes() {
        return cNCSATime.getTimeAsBytes(0L);
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#getNCSATimeAsBytes(long)
     */
    @Override
    public byte[] getNCSATimeAsBytes(long range) {
        return cNCSATime.getTimeAsBytes(range);
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#getNCSATime()
     */
    @Override
    public String getNCSATime() {
        return cNCSATime.getTimeAsString(0L);
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#getNCSATime(long)
     */
    @Override
    public String getNCSATime(long range) {
        return cNCSATime.getTimeAsString(range);
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#getNCSATime(java.util.Date)
     */
    @Override
    public String getNCSATime(Date inDate) {
        return cNCSATime.formatter.format(inDate.toInstant());
    }

    /**
     * Get the NCSA format of the time provided.
     *
     * This method is not on the interface and is called internally by web container
     * specific code to get the benefit of the caching function.
     *
     * @param time          time to be formatted
     * @param isCurrentTime if the provided time is the current time
     * @return the NCSA format of the time provided
     */
    public String getNCSATime(long time, boolean isCurrentTime) {
        return cNCSATime.getTimeAsString(time, 0L, isCurrentTime);
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
    private Date attemptParse(DateTimeFormatter format, String input) {
        ParsePosition pos = new ParsePosition(0);
        try {
            TemporalAccessor accessor = format.parse(input, pos);
            if (0 == pos.getIndex() || pos.getIndex() != input.length()) {
                // invalid format matching
                return null;
            }
            return Date.from(Instant.from(accessor));
        } catch (DateTimeException e) {
            return null;
        }
    }

    /*
     * @see com.ibm.websphere.http.HttpDateFormat#parseRFC1123Time(java.lang.String)
     */
    @Override
    public Date parseRFC1123Time(String input) throws ParseException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "rfc1123 parsing [" + input + "]");
        }
        Date d = attemptParse(c1123Time.formatter, input);
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
        Date d = attemptParse(c1036Time.formatter, input);
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
        Date d = attemptParse(c2109Time.formatter, input);
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
        Date d = attemptParse(cAsciiTime.formatter, input);
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

        Date parsedDate = attemptParse(c1123Time.formatter, data);
        if (null == parsedDate) {
            parsedDate = attemptParse(c1036Time.formatter, data);
            if (null == parsedDate) {
                parsedDate = attemptParse(cAsciiTime.formatter, data);
                if (null == parsedDate) {
                    parsedDate = attemptParse(c2109Time.formatter, data);
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
