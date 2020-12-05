package com.ibm.ws.jbatch.test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Simple convenience class wrapped around a ThreadLocal for obtaining a thread-specific
 * SimpleDateFormat for ser/deser dates in batch JSON data.
 * 
 * DateFormats are not thread safe; hence the ThreadLocal.
 */
public class BatchDateFormat extends SimpleDateFormat {
    
    private static final ThreadLocal<BatchDateFormat> ThreadSafe = new ThreadLocal<BatchDateFormat>() {
        @Override
        protected BatchDateFormat initialValue() {
            return new BatchDateFormat();
        }
    };
    
    /**
     * Private CTOR. Use the get() method to obtain a thread-safe instance. 
     */
    private BatchDateFormat() {
        super("yyyy/MM/dd HH:mm:ss.SSS Z");
    }
    
    /**
     * @return a thread-safe instance
     */
    public static BatchDateFormat get() {
        return ThreadSafe.get();
    }
    
    /**
     * @return the Date object parsed from the given string, or null if the string is null or empty.
     */
    public static Date parseDate(String dateString) {
        try {
            return (dateString == null || dateString.trim().length() == 0) ? null : get().parse(dateString);
        } catch (ParseException pe) {
            throw new RuntimeException(pe);
        }
    }
    
    /**
     * @return String representation of the given date, or "" if the date is null.
     */
    public static String formatDate(Date d) {
        return (d != null) ? get().format(d) : "";
    }

}
