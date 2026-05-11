/*
 * Copyright (c) 1998, 2025 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 1998, 2026 IBM Corporation and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0,
 * or the Eclipse Distribution License v. 1.0 which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: EPL-2.0 OR BSD-3-Clause
 */

// Contributors:
//     Oracle - initial API and implementation from Oracle TopLink
//     09/30/2014-2.6.0 Dalia Abo Sheasha
//       - 445546: NullPointerException thrown when an Array of Bytes contains null values
//     05/11/2020-2.7.0 Jody Grassel
//       - 538296: Wrong month is returned if OffsetDateTime is used in JPA 2.2 code
//     13/01/2022-4.0.0 Tomas Kraus
//       - 1391: JSON support in JPA
package org.eclipse.persistence.internal.helper;

import org.eclipse.persistence.config.SystemProperties;
import org.eclipse.persistence.exceptions.ConversionException;
import org.eclipse.persistence.exceptions.DatabaseException;
import org.eclipse.persistence.internal.core.helper.CoreClassConstants;
import org.eclipse.persistence.internal.core.helper.CoreConversionManager;
import org.eclipse.persistence.internal.security.PrivilegedAccessHelper;
import org.eclipse.persistence.logging.AbstractSessionLog;
import org.eclipse.persistence.logging.SessionLog;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.nio.ByteBuffer;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * <p>
 * <b>Purpose</b>: Contains the conversion routines for some common classes in the system.
 * Primarily used to convert objects from a given database type to a different type in Java.
 * Uses a singleton instance, this is also used from the platform.
 * <p>
 * <b>Responsibilities</b>:
 * <ul>
 * <li> Execute the appropriate conversion routine.
 *    </ul>
 */
public class ConversionManager extends CoreConversionManager implements Serializable, Cloneable {
    protected Map<Class<?>, Object> defaultNullValues;
    private static ZoneId defaultZoneOffset = null;

    /**
     * This flag is here if the Conversion Manager should use the class loader on the
     * thread when loading classes.
     */
    protected boolean shouldUseClassLoaderFromCurrentThread = false;
    protected static ConversionManager defaultManager;

    /** Allows the setting of a global default if no instance-level loader is set. */
    private static ClassLoader defaultLoader;
    protected ClassLoader loader;

    /** Store the list of Classes that can be converted to from the key. */
    protected Map<Object, List<Class<?>>> dataTypesConvertedFromAClass;

    /** Store the list of Classes that can be converted from to the key. */
    protected Map<Class<?>, List<Class<?>>> dataTypesConvertedToAClass;

    private static ZoneId getDefaultZoneOffset() {
        if (defaultZoneOffset == null) {
            ZoneId tzoneid = null;
            String tzone = null;
            try {
                tzone = PrivilegedAccessHelper.getSystemProperty(SystemProperties.CONVERSION_USE_TIMEZONE);
                if (tzone != null) {
                    try {
                        tzoneid = java.time.ZoneId.of(tzone);
                    } catch (Throwable t) {
                        // If an invalid time zone id is supplied, then fall back to checking for checking for using
                        // either UTC or the system's default time zone.
                        if (AbstractSessionLog.getLog().shouldLog(SessionLog.WARNING)) {
                            AbstractSessionLog.getLog().log(SessionLog.WARNING, "invalid_tzone", 
                                    SystemProperties.CONVERSION_USE_TIMEZONE, tzone);
                        }
                    }
                } 
            } catch (Exception e) {
                // Error occurred attempting to access this system property.  Fall back to the next property.
            }
            
            String propVal = null;
            try {
                if (tzoneid == null) {
                    propVal = PrivilegedAccessHelper.getSystemProperty(SystemProperties.CONVERSION_USE_DEFAULT_TIMEZONE, "false");
                    if (Boolean.parseBoolean(propVal)) {
                        tzoneid = java.time.ZoneId.systemDefault();
                    } else {
                        tzoneid = ZoneOffset.UTC;
                    }
                }
            } catch (Exception e) {
                // Error occurred attempting to access this system property.  Fall back to UTC.
                tzoneid = ZoneOffset.UTC;
                if (AbstractSessionLog.getLog().shouldLog(SessionLog.WARNING)) {
                    AbstractSessionLog.getLog().log(SessionLog.WARNING, "invalid_default_tzone", 
                            SystemProperties.CONVERSION_USE_DEFAULT_TIMEZONE, propVal);
                }
            }           
            
            defaultZoneOffset = tzoneid;
            
            if (AbstractSessionLog.getLog().shouldLog(SessionLog.FINER)) {
                AbstractSessionLog.getLog().log(SessionLog.FINER, "using_conversion_tzone", defaultZoneOffset);
            }
        }
        
        return defaultZoneOffset;        
    }
    
    public ConversionManager() {
        this.dataTypesConvertedFromAClass = new Hashtable<>();
        this.dataTypesConvertedToAClass = new Hashtable<>();
    }

    /**
     * INTERNAL:
     */
    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException exception) {
            return null;
        }
    }

    /**
     * Convert the object to the appropriate type by invoking the appropriate
     * ConversionManager method
     * @param sourceObject the object that must be converted
     * @param javaClass the class that the object must be converted to
     * @exception ConversionException all exceptions will be thrown as this type.
     * @return the newly converted object
     */
    @Override
    @SuppressWarnings({"unchecked"})
    public <T> T convertObject(Object sourceObject, Class<T> javaClass) throws ConversionException {
        if (sourceObject == null) {
            // Check for default null conversion.
            // i.e. allow for null to be defaulted to "", or 0 etc.
            if (javaClass != null ) {
                return getDefaultNullValue(javaClass);
            } else {
                return null;
            }
        }
        if (sourceObject.getClass() == javaClass || javaClass == null || javaClass == CoreClassConstants.OBJECT
                || javaClass == ClassConstants.BLOB || javaClass == ClassConstants.CLOB
                // JSON has its own default converter registered. Direct jakarta.json class reference can't be used in core.
                || javaClass.getName().contains("json")) {
            return (T) sourceObject;
        }

        try {
            if (javaClass == CoreClassConstants.STRING) {
                return (T) convertObjectToString(sourceObject);
            } else if (javaClass == CoreClassConstants.UTILDATE) {
                return (T) convertObjectToUtilDate(sourceObject);
            } else if (javaClass == ClassConstants.SQLDATE) {
                return (T) convertObjectToDate(sourceObject);
            } else if (javaClass == ClassConstants.TIME) {
                return (T) convertObjectToTime(sourceObject);
            } else if (javaClass == ClassConstants.TIMESTAMP) {
                return (T) convertObjectToTimestamp(sourceObject);
            } else if (javaClass == ClassConstants.TIME_INSTANT) {
                return (T) convertObjectToInstant(sourceObject);
            } else if (javaClass == ClassConstants.TIME_LDATE) {
                return (T) convertObjectToLocalDate(sourceObject);
            } else if (javaClass == ClassConstants.TIME_LDATETIME) {
                return (T) convertObjectToLocalDateTime(sourceObject);
            } else if (javaClass == ClassConstants.TIME_LTIME) {
                return (T) convertObjectToLocalTime(sourceObject);
            } else if (javaClass == ClassConstants.TIME_ODATETIME) {
                return (T) convertObjectToOffsetDateTime(sourceObject);
            } else if (javaClass == ClassConstants.TIME_OTIME) {
                return (T) convertObjectToOffsetTime(sourceObject);
            } else if (javaClass == ClassConstants.TIME_YEAR) {
                return (T) convertObjectToYear(sourceObject);
            } else if ((javaClass == CoreClassConstants.CALENDAR) || (javaClass == CoreClassConstants.GREGORIAN_CALENDAR)) {
                return (T) convertObjectToCalendar(sourceObject);
            } else if ((javaClass == CoreClassConstants.CHAR) || (javaClass == CoreClassConstants.PCHAR && !(sourceObject instanceof Character))) {
                return (T) convertObjectToChar(sourceObject);
            } else if ((javaClass == CoreClassConstants.INTEGER) || (javaClass == CoreClassConstants.PINT && !(sourceObject instanceof Integer))) {
                return (T) convertObjectToInteger(sourceObject);
            } else if ((javaClass == CoreClassConstants.DOUBLE) || (javaClass == CoreClassConstants.PDOUBLE && !(sourceObject instanceof Double))) {
                return (T) convertObjectToDouble(sourceObject);
            } else if ((javaClass == CoreClassConstants.FLOAT) || (javaClass == CoreClassConstants.PFLOAT && !(sourceObject instanceof Float))) {
                return (T) convertObjectToFloat(sourceObject);
            } else if ((javaClass == CoreClassConstants.LONG) || (javaClass == CoreClassConstants.PLONG && !(sourceObject instanceof Long))) {
                return (T) convertObjectToLong(sourceObject);
            } else if ((javaClass == CoreClassConstants.SHORT) || (javaClass == CoreClassConstants.PSHORT && !(sourceObject instanceof Short))) {
                return (T) convertObjectToShort(sourceObject);
            } else if ((javaClass == CoreClassConstants.BYTE) || (javaClass == CoreClassConstants.PBYTE && !(sourceObject instanceof Byte))) {
                return (T) convertObjectToByte(sourceObject);
            } else if (javaClass == CoreClassConstants.BIGINTEGER) {
                return (T) convertObjectToBigInteger(sourceObject);
            } else if (javaClass == CoreClassConstants.BIGDECIMAL) {
                return (T) convertObjectToBigDecimal(sourceObject);
            } else if (javaClass == CoreClassConstants.NUMBER) {
                return (T) convertObjectToNumber(sourceObject);
            } else if ((javaClass == CoreClassConstants.BOOLEAN) || (javaClass == CoreClassConstants.PBOOLEAN  && !(sourceObject instanceof Boolean))) {
                return (T) convertObjectToBoolean(sourceObject);
            } else if (javaClass == CoreClassConstants.APBYTE) {
                return (T) convertObjectToByteArray(sourceObject);
            } else if (javaClass == CoreClassConstants.ABYTE) {
                return (T) convertObjectToByteObjectArray(sourceObject);
            } else if (javaClass == CoreClassConstants.APCHAR) {
                return (T) convertObjectToCharArray(sourceObject);
            } else if (javaClass == ClassConstants.ACHAR) {
                return (T) convertObjectToCharacterArray(sourceObject);
            } else if ((sourceObject.getClass() == CoreClassConstants.STRING) && (javaClass == CoreClassConstants.CLASS)) {
                return (T) convertObjectToClass(sourceObject);
            } else if(javaClass == CoreClassConstants.URL_Class) {
                return (T) convertObjectToUrl(sourceObject);
            } else if(javaClass == CoreClassConstants.UUID) {
                return (T) convertObjectToUUID(sourceObject);
            }
        } catch (ConversionException ce) {
            throw ce;
        } catch (Exception e) {
            throw ConversionException.couldNotBeConverted(sourceObject, javaClass, e);
        }

        // Check if object is instance of the real class for the primitive class.
        if ((((javaClass == CoreClassConstants.PBOOLEAN) && (sourceObject instanceof Boolean)  ) ||
            ((javaClass == CoreClassConstants.PLONG) && (sourceObject instanceof Long) ) ||
            ((javaClass == CoreClassConstants.PINT) && (sourceObject instanceof Integer)  ) ||
            ((javaClass == CoreClassConstants.PFLOAT) && (sourceObject instanceof Float)) ||
            ((javaClass == CoreClassConstants.PDOUBLE) &&  (sourceObject instanceof Double) ) ||
            ((javaClass == CoreClassConstants.PBYTE) &&  (sourceObject instanceof Byte)) ||
            ((javaClass == CoreClassConstants.PCHAR) &&  (sourceObject instanceof Character)) ||
            ((javaClass == CoreClassConstants.PSHORT) &&  (sourceObject instanceof Short)))) {
            return (T) sourceObject;
        }

        // Delay this check as poor performance.
        if (javaClass.isInstance(sourceObject)) {
            return (T) sourceObject;
        }
        if (ClassConstants.NOCONVERSION.isAssignableFrom(javaClass)) {
            return (T) sourceObject;
        }

        throw ConversionException.couldNotBeConverted(sourceObject, javaClass);
    }

    /**
     * Build a valid instance of BigDecimal from the given sourceObject
     *    @param sourceObject    Valid instance of String, BigInteger, any Number
     */
    protected BigDecimal convertObjectToBigDecimal(Object sourceObject) throws ConversionException {
        BigDecimal bigDecimal = null;

        try {
            if (sourceObject instanceof String) {
                bigDecimal = new BigDecimal((String)sourceObject);
            } else if (sourceObject instanceof BigInteger) {
                bigDecimal = new BigDecimal((BigInteger)sourceObject);
            } else if (sourceObject instanceof Number) {
                // Doubles do not maintain scale, because of this it is
                // impossible to distinguish between 1 and 1.0.  In order to
                // maintain backwards compatibility both 1 and 1.0 will be
                // treated as BigDecimal(1).
                String numberString = String.valueOf(sourceObject);
                if(numberString.endsWith(".0") || numberString.contains(".0E+")) {
                    bigDecimal = new BigDecimal(((Number)sourceObject).doubleValue());
                } else {
                    bigDecimal = new BigDecimal(numberString);
                }
            } else {
                throw ConversionException.couldNotBeConverted(sourceObject, CoreClassConstants.BIGDECIMAL);
            }
        } catch (NumberFormatException exception) {
            throw ConversionException.couldNotBeConverted(sourceObject, CoreClassConstants.BIGDECIMAL, exception);
        }
        return bigDecimal;
    }

    /**
     * Build a valid instance of BigInteger from the provided sourceObject.
     *    @param sourceObject    Valid instance of String, BigDecimal, or any Number
     */
    protected BigInteger convertObjectToBigInteger(Object sourceObject) throws ConversionException {
        BigInteger bigInteger = null;

        try {
            if (sourceObject instanceof BigInteger) {
                bigInteger = (BigInteger)sourceObject;
            } else if (sourceObject instanceof String) {
                bigInteger = new BigInteger((String)sourceObject);
            } else if (sourceObject instanceof BigDecimal) {
                bigInteger = ((BigDecimal)sourceObject).toBigInteger();
            } else if (sourceObject instanceof Number) {
                bigInteger = new BigInteger(String.valueOf(((Number)sourceObject).longValue()));
            } else if (sourceObject instanceof Byte[] objectBytes) {
                byte[] bytes = new byte[objectBytes.length];
                for (int index = 0; index < objectBytes.length; index++) {
                    bytes[index] = objectBytes[index];
                }
                bigInteger = new BigInteger(bytes);
            } else if (sourceObject instanceof byte[]) {
                bigInteger = new BigInteger((byte[]) sourceObject);
            } else {
                throw ConversionException.couldNotBeConverted(sourceObject, CoreClassConstants.BIGINTEGER);
            }
        } catch (NumberFormatException exception) {
            throw ConversionException.couldNotBeConverted(sourceObject, CoreClassConstants.BIGINTEGER, exception);
        }

        return bigInteger;
    }

    /**
     *    Build a valid instance of Boolean from the source object.
     *    't', 'T', "true", "TRUE", 1,'1'             -&gt; Boolean(true)
     *    'f', 'F', "false", "FALSE", 0 ,'0'        -&gt; Boolean(false)
     */
    protected Boolean convertObjectToBoolean(Object sourceObject) {
        if (sourceObject instanceof Character) {
            switch (Character.toLowerCase((Character) sourceObject)) {
            case '1':
            case 't':
                return Boolean.TRUE;
            case '0':
            case 'f':
                return Boolean.FALSE;
            }
        }
        if (sourceObject instanceof String) {
            String stringValue = ((String)sourceObject).toLowerCase();
            if (stringValue.equals("t") || stringValue.equals("true") || stringValue.equals("1")) {
                return Boolean.TRUE;
            } else if (stringValue.equals("f") || stringValue.equals("false") || stringValue.equals("0")) {
                return Boolean.FALSE;
            }
        }
        if (sourceObject instanceof Number) {
            int intValue = ((Number)sourceObject).intValue();
            if (intValue != 0) {
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }
        throw ConversionException.couldNotBeConverted(sourceObject, CoreClassConstants.BOOLEAN);
    }

    /**
     * Build a valid instance of Byte from the provided sourceObject
     * @param sourceObject    Valid instance of String or any Number
     * @throws ConversionException   The Byte(String) constructor throws a
     *     NumberFormatException if the String does not contain a parsable byte.
     *
     */
    protected Byte convertObjectToByte(Object sourceObject) throws ConversionException {
        try {
            if (sourceObject instanceof String) {
                return Byte.valueOf((String)sourceObject);
            }
            if (sourceObject instanceof Number) {
                return ((Number) sourceObject).byteValue();
            }
        } catch (NumberFormatException exception) {
            throw ConversionException.couldNotBeConverted(sourceObject, CoreClassConstants.BYTE, exception);
        }

        throw ConversionException.couldNotBeConverted(sourceObject, CoreClassConstants.BYTE);
    }

    /**
      * Build a valid instance of a byte array from the given object.
      * This method does hex conversion of the string values.  Some
      * databases have problems with storing blobs unless the blob
      * is stored as a hex string.
      */
    protected byte[] convertObjectToByteArray(Object sourceObject) throws ConversionException {
        //Bug#3128838 Used when converted to Byte[]
        if (sourceObject instanceof byte[]) {
            return (byte[])sourceObject;
            //Related to Bug#3128838.  Add support to convert to Byte[]
        } else if (sourceObject instanceof Byte[] objectBytes) {
            byte[] bytes = new byte[objectBytes.length];
            for (int index = 0; index < objectBytes.length; index++) {
                Byte value = objectBytes[index];
                if (value != null) {
                    bytes[index] = value;
                }
            }
            return bytes;
        } else if (sourceObject instanceof String) {
            return HexFormat.of().parseHex((String)sourceObject);
        } else if (sourceObject instanceof Blob blob) {
            try {
                return blob.getBytes(1L, (int)blob.length());
            } catch (SQLException exception) {
                throw DatabaseException.sqlException(exception);
            }
        } else if (sourceObject instanceof InputStream inputStream) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try {
                int tempInt = inputStream.read();
                while (tempInt != -1) {
                    outputStream.write(tempInt);
                    tempInt = inputStream.read();
                }
                return outputStream.toByteArray();
            } catch (IOException ioException) {
                throw ConversionException.couldNotBeConverted(sourceObject, CoreClassConstants.APBYTE, ioException);
            }
        } else if (sourceObject instanceof BigInteger) {
            return ((BigInteger)sourceObject).toByteArray();
        } else if (sourceObject instanceof UUID uuid) {
            ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
            bb.putLong(uuid.getMostSignificantBits());
            bb.putLong(uuid.getLeastSignificantBits());
            return bb.array();
        }

        throw ConversionException.couldNotBeConverted(sourceObject, CoreClassConstants.APBYTE);
    }

    /**
      * Build a valid instance of a Byte array from the given object.
      * This method does hex conversion of the string values.  Some
      * databases have problems with storing blobs unless the blob
      * is stored as a hex string.
      */
    protected Byte[] convertObjectToByteObjectArray(Object sourceObject) throws ConversionException {
        byte[] bytes = convertObjectToByteArray(sourceObject);
        Byte[] objectBytes = new Byte[bytes.length];
        for (int index = 0; index < bytes.length; index++) {
            objectBytes[index] = bytes[index];
        }
        return objectBytes;
    }

    /**
     * Build a valid instance of java.util.Calendar from the given source object.
     *    @param sourceObject    Valid instance of java.util.Date, String, java.sql.Timestamp, or Long
     */
    protected Calendar convertObjectToCalendar(Object sourceObject) throws ConversionException {
        if (sourceObject instanceof Calendar) {
            return (Calendar)sourceObject;
        } else if (sourceObject instanceof java.util.Date) {
            // PERF: Avoid double conversion for date subclasses.
            return Helper.calendarFromUtilDate((java.util.Date)sourceObject);
        }
        return Helper.calendarFromUtilDate(convertObjectToUtilDate(sourceObject));
    }

    /**
     * Build a valid instance of Character from the provided sourceObject.
     *    @param sourceObject    Valid instance of String or any Number
     */
    protected Character convertObjectToChar(Object sourceObject) throws ConversionException {
        if (sourceObject instanceof String) {
            if (((String) sourceObject).isEmpty()) {
                // ELBug336192 - Return default null value of char instead of returning null.
                return getDefaultNullValue(CoreClassConstants.PCHAR);
            }
            return ((String) sourceObject).charAt(0);
        }

        if (sourceObject instanceof Number) {
            return (char) ((Number) sourceObject).byteValue();
        }

        throw ConversionException.couldNotBeConverted(sourceObject, CoreClassConstants.CHAR);
    }

    /**
      * Build a valid instance of a Character array from the given object.
      */
    protected Character[] convertObjectToCharacterArray(Object sourceObject) throws ConversionException {
        String stringValue = convertObjectToString(sourceObject);
        Character[] chars = new Character[stringValue.length()];
        for (int index = 0; index < stringValue.length(); index++) {
            chars[index] = stringValue.charAt(index);
        }
        return chars;
    }

    /**
      * Build a valid instance of a char array from the given object.
      */
    protected char[] convertObjectToCharArray(Object sourceObject) throws ConversionException {
        if (sourceObject instanceof Character[] objectChars) {
            char[] chars = new char[objectChars.length];
            for (int index = 0; index < objectChars.length; index++) {
                chars[index] = objectChars[index];
            }
            return chars;
        }
        String stringValue = convertObjectToString(sourceObject);
        char[] chars = new char[stringValue.length()];
        for (int index = 0; index < stringValue.length(); index++) {
            chars[index] = stringValue.charAt(index);
        }
        return chars;
    }

    /**
     * Build a valid Class from the string that is passed in
     *    @param sourceObject    Valid instance of String
     */
    @SuppressWarnings({"unchecked"})
    protected <T> Class<T> convertObjectToClass(Object sourceObject) throws ConversionException {
        Class<?> theClass = null;
        if (!(sourceObject instanceof String)) {
            throw ConversionException.couldNotBeConverted(sourceObject, CoreClassConstants.CLASS);
        }
        try {
            // bug # 2799318
            theClass = getPrimitiveClass((String)sourceObject);
            if (theClass == null) {
                theClass = Class.forName((String)sourceObject, true, getLoader());
            }
        } catch (Exception exception) {
            throw ConversionException.couldNotBeConvertedToClass(sourceObject, CoreClassConstants.CLASS, exception);
        }
        return (Class<T>) theClass;
    }

    /**
      * Convert the object to an instance of java.sql.Date.
      *    @param    sourceObject Object of type java.sql.Timestamp, java.util.Date, String or Long
      */
    protected java.sql.Date convertObjectToDate(Object sourceObject) throws ConversionException {
        java.sql.Date date = null;

        if (sourceObject instanceof java.sql.Date) {
            date = (java.sql.Date)sourceObject;//Helper date is not caught on class check.
        } else if (sourceObject instanceof java.sql.Timestamp) {
            date = Helper.dateFromTimestamp((java.sql.Timestamp)sourceObject);
        } else if (sourceObject.getClass() == CoreClassConstants.UTILDATE) {
            date = Helper.sqlDateFromUtilDate((java.util.Date)sourceObject);
        } else if (sourceObject instanceof Calendar) {
            return Helper.dateFromCalendar((Calendar)sourceObject);
        } else if (sourceObject instanceof String) {
            date = Helper.dateFromString((String)sourceObject);
        } else if (sourceObject instanceof Long) {
            date = Helper.dateFromLong((Long)sourceObject);
        } else {
            throw ConversionException.couldNotBeConverted(sourceObject, ClassConstants.SQLDATE);
        }
        return date;
    }

    /**
      * Convert the object to an instance of Double.
      * @param                    sourceObject Object of type String or Number.
      * @throws ConversionException The Double(String) constructor throws a
      *         NumberFormatException if the String does not contain a parsable double.
      */
    protected Double convertObjectToDouble(Object sourceObject) throws ConversionException {
        try {
            if (sourceObject instanceof String) {
                return Double.valueOf((String)sourceObject);
            }
            if (sourceObject instanceof Number) {
                return ((Number) sourceObject).doubleValue();
            }
        } catch (NumberFormatException exception) {
            throw ConversionException.couldNotBeConverted(sourceObject, CoreClassConstants.DOUBLE, exception);
        }
        throw ConversionException.couldNotBeConverted(sourceObject, CoreClassConstants.DOUBLE);
    }

    /**
     * Build a valid Float instance from a String or another Number instance.
     * @throws ConversionException The Float(String) constructor throws a
     *         NumberFormatException if the String does not contain a parsable Float.
     */
    protected Float convertObjectToFloat(Object sourceObject) throws ConversionException {
        try {
            if (sourceObject instanceof String) {
                return Float.valueOf((String)sourceObject);
            }
            if (sourceObject instanceof Number) {
                return ((Number) sourceObject).floatValue();
            }
        } catch (NumberFormatException exception) {
            throw ConversionException.couldNotBeConverted(sourceObject, CoreClassConstants.FLOAT, exception);
        }

        throw ConversionException.couldNotBeConverted(sourceObject, CoreClassConstants.FLOAT);
    }

    /**
     * Build a valid Integer instance from a String or another Number instance.
     * @throws ConversionException The Integer(String) constructor throws a
     *         NumberFormatException if the String does not contain a parsable integer.
     */
    protected Integer convertObjectToInteger(Object sourceObject) throws ConversionException {
        try {
            if (sourceObject instanceof String) {
                return Integer.valueOf((String)sourceObject);
            }

            if (sourceObject instanceof Number) {
                return ((Number) sourceObject).intValue();
            }

            if (sourceObject instanceof Boolean) {
                if ((Boolean) sourceObject) {
                    return 1;
                } else {
                    return 0;
                }
            }
        } catch (NumberFormatException exception) {
            throw ConversionException.couldNotBeConverted(sourceObject, CoreClassConstants.INTEGER, exception);
        }

        throw ConversionException.couldNotBeConverted(sourceObject, CoreClassConstants.INTEGER);
    }

    /**
      * Build a valid Long instance from a String or another Number instance.
      * @throws ConversionException  The Long(String) constructor throws a
      *         NumberFormatException if the String does not contain a parsable long.
      *
      */
    protected Long convertObjectToLong(Object sourceObject) throws ConversionException {
        try {
            if (sourceObject instanceof String) {
                return Long.valueOf((String)sourceObject);
            }
            if (sourceObject instanceof Number) {
                return ((Number) sourceObject).longValue();
            }
            if (sourceObject instanceof java.util.Date) {
                return ((java.util.Date) sourceObject).getTime();
            }
            if (sourceObject instanceof java.util.Calendar) {
                return ((Calendar) sourceObject).getTimeInMillis();
            }

            if (sourceObject instanceof Boolean) {
                if ((Boolean) sourceObject) {
                    return 1L;
                } else {
                    return 0L;
                }
            }
        } catch (NumberFormatException exception) {
            throw ConversionException.couldNotBeConverted(sourceObject, CoreClassConstants.LONG, exception);
        }

        throw ConversionException.couldNotBeConverted(sourceObject, CoreClassConstants.LONG);
    }

    /**
     * INTERNAL:
     * Build a valid BigDecimal instance from a String or another
     * Number instance.  BigDecimal is the most general type so is
     * must be returned when an object is converted to a number.
     * @throws ConversionException The BigDecimal(String) constructor throws a
     *     NumberFormatException if the String does not contain a parsable BigDecimal.
     */
    protected BigDecimal convertObjectToNumber(Object sourceObject) throws ConversionException {
        try {
            if (sourceObject instanceof String) {
                return new BigDecimal((String)sourceObject);
            }

            if (sourceObject instanceof Number) {
                return new BigDecimal(((Number)sourceObject).doubleValue());
            }

            if (sourceObject instanceof Boolean) {
                if ((Boolean) sourceObject) {
                    return BigDecimal.valueOf(1);
                } else {
                    return BigDecimal.valueOf(0);
                }
            }
        } catch (NumberFormatException exception) {
            throw ConversionException.couldNotBeConverted(sourceObject, CoreClassConstants.NUMBER, exception);
        }

        throw ConversionException.couldNotBeConverted(sourceObject, CoreClassConstants.NUMBER);
    }

    /**
     * INTERNAL:
     * Build a valid Short instance from a String or another Number instance.
     * @throws ConversionException The Short(String) constructor throws a
     *     NumberFormatException if the String does not contain a parsable short.
     */
    protected Short convertObjectToShort(Object sourceObject) throws ConversionException {
        try {
            if (sourceObject instanceof String) {
                return Short.valueOf((String)sourceObject);
            }

            if (sourceObject instanceof Number) {
                return ((Number) sourceObject).shortValue();
            }

            if (sourceObject instanceof Boolean) {
                if ((Boolean) sourceObject) {
                    return (short) 1;
                } else {
                    return (short) 0;
                }
            }
        } catch (Exception exception) {
            throw ConversionException.couldNotBeConverted(sourceObject, CoreClassConstants.SHORT, exception);
        }

        throw ConversionException.couldNotBeConverted(sourceObject, CoreClassConstants.SHORT);
    }

    /**
     * INTERNAL:
     * Converts objects to their string representations.  java.util.Date
     * is converted to a timestamp first and then to a string.  An array
     * of bytes is converted to a hex string.
     */
    protected String convertObjectToString(Object sourceObject) throws ConversionException {

        Class<?> sourceObjectClass = sourceObject.getClass();

        if (sourceObject instanceof java.lang.Number) {
            return sourceObject.toString();
        } else if (sourceObjectClass == CoreClassConstants.BOOLEAN) {
            return sourceObject.toString();
        } else if (sourceObjectClass == CoreClassConstants.UTILDATE) {
            return Helper.printTimestamp(Helper.timestampFromDate((java.util.Date)sourceObject));
        } else if (sourceObject instanceof java.util.Calendar) {
            return Helper.printCalendar((Calendar)sourceObject);
        } else if (sourceObjectClass == ClassConstants.TIMESTAMP) {
            return Helper.printTimestamp((java.sql.Timestamp)sourceObject);
        } else if (sourceObject instanceof java.sql.Date) {
            return Helper.printDate((java.sql.Date)sourceObject);
        } else if (sourceObject instanceof java.sql.Time) {
            return Helper.printTime((java.sql.Time)sourceObject);
        } else if (sourceObjectClass == CoreClassConstants.APBYTE) {
            return HexFormat.of().formatHex((byte[])sourceObject);
            //Bug#3854296 Added support to convert Byte[], char[] and Character[] to String correctly
        } else if (sourceObjectClass == CoreClassConstants.ABYTE) {
            return HexFormat.of().formatHex(convertObjectToByteArray(sourceObject));
        } else if (sourceObjectClass == CoreClassConstants.APCHAR) {
            return new String((char[])sourceObject);
        } else if (sourceObjectClass == ClassConstants.ACHAR) {
            return new String(convertObjectToCharArray(sourceObject));
        } else if (sourceObject instanceof Class) {
            return ((Class<?>)sourceObject).getName();
        } else if (sourceObjectClass == CoreClassConstants.CHAR) {
            return sourceObject.toString();
        } else if (sourceObject instanceof Clob clob) {
            try {
                return clob.getSubString(1L, (int)clob.length());
            } catch (SQLException exception) {
                throw DatabaseException.sqlException(exception);
            }
        }

        return sourceObject.toString();
    }

    /**
     * INTERNAL:
     * Build a valid instance of java.sql.Time from the given source object.
     * @param    sourceObject    Valid instance of java.sql.Time, String, java.util.Date, java.sql.Timestamp, or Long
     */
    protected java.sql.Time convertObjectToTime(Object sourceObject) throws ConversionException {
        java.sql.Time time = null;

        if (sourceObject instanceof java.sql.Time) {
            return (java.sql.Time)sourceObject;//Helper timestamp is not caught on class check.
        }

        if (sourceObject instanceof String) {
            time = Helper.timeFromString((String)sourceObject);
        } else if (sourceObject.getClass() == CoreClassConstants.UTILDATE) {
            time = Helper.timeFromDate((java.util.Date)sourceObject);
        } else if (sourceObject instanceof java.sql.Timestamp) {
            time = Helper.timeFromTimestamp((java.sql.Timestamp)sourceObject);
        } else if (sourceObject instanceof Calendar) {
            return Helper.timeFromCalendar((Calendar)sourceObject);
        } else if (sourceObject instanceof Long) {
            time = Helper.timeFromLong((Long)sourceObject);
        } else if (sourceObject.getClass() == ClassConstants.TIME_LTIME) {
            time = java.sql.Time.valueOf((java.time.LocalTime) sourceObject);
        } else if (sourceObject.getClass() == ClassConstants.TIME_OTIME) {
            time = java.sql.Time.valueOf(((java.time.OffsetTime) sourceObject).toLocalTime());
        } else {
            throw ConversionException.couldNotBeConverted(sourceObject, ClassConstants.TIME);
        }
        return time;
    }

    /**
     * INTERNAL:
     * Build a valid instance of java.sql.Timestamp from the given source object.
     * @param sourceObject    Valid object of class java.sql.Timestamp, String, java.util.Date, or Long
     */
    protected java.sql.Timestamp convertObjectToTimestamp(Object sourceObject) throws ConversionException {
        java.sql.Timestamp timestamp = null;

        if (sourceObject instanceof java.sql.Timestamp) {
            return (java.sql.Timestamp)sourceObject;// Helper timestamp is not caught on class check.
        }

        if (sourceObject instanceof String) {
            timestamp = Helper.timestampFromString((String)sourceObject);
        } else if (sourceObject instanceof java.util.Date) {// This handles all date and subclasses, sql.Date, sql.Time conversions.
            timestamp = Helper.timestampFromDate((java.util.Date)sourceObject);
        } else if (sourceObject instanceof Calendar) {
            return Helper.timestampFromCalendar((Calendar)sourceObject);
        } else if (sourceObject instanceof Long) {
            timestamp = Helper.timestampFromLong((Long)sourceObject);
        } else if (sourceObject instanceof LocalDateTime) {
            timestamp = Timestamp.valueOf((LocalDateTime) sourceObject);
        } else {
            throw ConversionException.couldNotBeConverted(sourceObject, ClassConstants.TIMESTAMP);
        }
        return timestamp;
    }

    /**
     * INTERNAL: Build a valid instance of java.time.Instant from the given
     * source object.
     *
     * @param sourceObject
     *            Valid object of class java.sql.Timestamp, String,
     *            java.util.Date, or Long
     */
    protected java.time.Instant convertObjectToInstant(Object sourceObject) throws ConversionException {
        java.time.Instant instant = null;

        if (sourceObject instanceof java.time.Instant) {
            return (java.time.Instant) sourceObject;
        }

        if (sourceObject instanceof String) {
            instant = java.time.Instant.parse((String) sourceObject);
        } else if (sourceObject instanceof java.sql.Date) {
            instant = Instant.ofEpochMilli(((java.sql.Date) sourceObject).getTime());
        } else if (sourceObject instanceof java.sql.Timestamp) {
            instant = ((java.sql.Timestamp) sourceObject).toInstant();
        } else if (sourceObject instanceof java.util.Date) {
            // handles sql.Time too
            instant = ((java.util.Date) sourceObject).toInstant();
        } else if (sourceObject instanceof Calendar cal) {
            instant = cal.toInstant();
        } else if (sourceObject instanceof java.time.LocalDateTime) {
            instant = ((LocalDateTime) sourceObject).toInstant(ZoneOffset.UTC);
        } else if (sourceObject instanceof Long) {
            instant = java.time.Instant.ofEpochSecond((Long) sourceObject);
        } else {
            throw ConversionException.couldNotBeConverted(sourceObject, ClassConstants.TIME_LDATE);
        }
        return instant;
    }

    /**
     * INTERNAL: Build a valid instance of java.time.LocalDate from the given
     * source object.
     *
     * @param sourceObject
     *            Valid object of class java.sql.Timestamp, String,
     *            java.util.Date, or Long
     */
    protected java.time.LocalDate convertObjectToLocalDate(Object sourceObject) throws ConversionException {
        java.time.LocalDate localDate = null;

        if (sourceObject instanceof java.time.LocalDate) {
            return (java.time.LocalDate) sourceObject;
        }

        if (sourceObject instanceof String) {
            localDate = java.time.LocalDate.parse(((String) sourceObject).replace(' ', 'T'), Helper.getDefaultDateTimeFormatter());
        } else if (sourceObject instanceof java.sql.Date) {
            localDate = ((java.sql.Date) sourceObject).toLocalDate();
        } else if (sourceObject instanceof java.sql.Timestamp) {
            localDate = ((java.sql.Timestamp) sourceObject).toLocalDateTime().toLocalDate();
        } else if (sourceObject instanceof java.util.Date) {
            // handles sql.Time too
            localDate = ((java.util.Date) sourceObject).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        } else if (sourceObject instanceof Calendar cal) {
            localDate = java.time.LocalDate.of(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
        } else if (sourceObject instanceof Long) {
            localDate = java.time.LocalDate.ofEpochDay((Long) sourceObject);
        } else {
            throw ConversionException.couldNotBeConverted(sourceObject, ClassConstants.TIME_LDATE);
        }

        return localDate;
    }

    /**
     * INTERNAL: Build a valid instance of java.time.LocalTime from the given
     * source object.
     *
     * @param sourceObject
     *            Valid object of class java.sql.Timestamp, String,
     *            java.util.Date, or Long
     */
    protected java.time.LocalTime convertObjectToLocalTime(Object sourceObject) throws ConversionException {
        java.time.LocalTime localTime = null;

        if (sourceObject instanceof java.time.LocalTime) {
            return (java.time.LocalTime) sourceObject;
        }

        if (sourceObject instanceof String) {
            localTime = java.time.LocalTime.parse(((String) sourceObject).replace(' ', 'T'), Helper.getDefaultDateTimeFormatter());
        } else if (sourceObject instanceof java.sql.Timestamp) {
            localTime = ((java.sql.Timestamp) sourceObject).toLocalDateTime().toLocalTime();
        } else if (sourceObject instanceof java.sql.Time) {
            localTime = ((java.sql.Time) sourceObject).toLocalTime();
        } else if (sourceObject instanceof java.util.Date) {
            // handles sql.Date
            Calendar cal = Helper.allocateCalendar();
            cal.setTime((java.util.Date) sourceObject);
            localTime = java.time.LocalTime.of(
                    cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND), cal.get(Calendar.MILLISECOND));
            Helper.releaseCalendar(cal);
        } else if (sourceObject instanceof Calendar cal) {
            localTime = java.time.LocalTime.of(
                    cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND), cal.get(Calendar.MILLISECOND));
        } else if (sourceObject instanceof Long) {
            localTime = java.time.LocalTime.ofSecondOfDay((Long) sourceObject);
        } else {
            throw ConversionException.couldNotBeConverted(sourceObject, ClassConstants.TIME_LTIME);
        }

        return localTime;
    }

    /**
     * INTERNAL: Build a valid instance of java.time.LocalDateTime from the given
     * source object.
     *
     * @param sourceObject
     *            Valid object of class java.sql.Timestamp, String,
     *            java.util.Date, or Long
     */
    protected java.time.LocalDateTime convertObjectToLocalDateTime(Object sourceObject) throws ConversionException {
        java.time.LocalDateTime localDateTime = null;

        if (sourceObject instanceof java.time.LocalDateTime) {
            return (java.time.LocalDateTime) sourceObject;
        }

        if (sourceObject instanceof String) {
            localDateTime = java.time.LocalDateTime.parse(((String) sourceObject).replace(' ', 'T'), Helper.getDefaultDateTimeFormatter());
        } else if (sourceObject instanceof java.sql.Timestamp) {
            localDateTime = ((java.sql.Timestamp) sourceObject).toLocalDateTime();
        } else if (sourceObject instanceof java.util.Date) {
            // handles sql.Time, sql.Date
            Calendar cal = Helper.allocateCalendar();
            cal.setTime((java.util.Date) sourceObject);
            localDateTime = java.time.LocalDateTime.of(
                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH),
                    cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND), cal.get(Calendar.MILLISECOND));
            Helper.releaseCalendar(cal);
        } else if (sourceObject instanceof Calendar cal) {
            localDateTime = java.time.LocalDateTime.of(
                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH),
                    cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND), cal.get(Calendar.MILLISECOND));
        } else if (sourceObject instanceof Long) {
            localDateTime = java.time.LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochSecond((Long) sourceObject), getDefaultZoneOffset());
        } else {
            throw ConversionException.couldNotBeConverted(sourceObject, ClassConstants.TIME_LDATETIME);
        }

        return localDateTime;
    }

    /**
     * INTERNAL: Build a valid instance of java.time.OffsetDateTime from the given
     * source object.
     *
     * @param sourceObject
     *            Valid object of class java.sql.Timestamp, String,
     *            java.util.Date, or Long
     */
    protected java.time.OffsetDateTime convertObjectToOffsetDateTime(Object sourceObject) throws ConversionException {
        java.time.OffsetDateTime offsetDateTime = null;

        if (sourceObject instanceof java.time.OffsetDateTime) {
            return (java.time.OffsetDateTime) sourceObject;
        }

        if (sourceObject instanceof String) {
            java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(((String) sourceObject).replace(' ', 'T'), Helper.getDefaultDateTimeFormatter());
            return ldt.atZone(java.time.ZoneId.systemDefault()).toOffsetDateTime();
        } else if (sourceObject instanceof java.util.Date) {
            // handles sql.Time, sql.Date, sql.Timestamp
            Calendar cal = Helper.allocateCalendar();
            cal.setTime((java.util.Date) sourceObject);
            offsetDateTime = java.time.OffsetDateTime.of(
                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH),
                    cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND), cal.get(Calendar.MILLISECOND) * 1000000,
                    java.time.ZoneOffset.ofTotalSeconds((cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET)) / 1000));
            Helper.releaseCalendar(cal);
        } else if (sourceObject instanceof Calendar cal) {
            offsetDateTime = java.time.OffsetDateTime.of(
                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH),
                    cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND), cal.get(Calendar.MILLISECOND)  * 1000000,
                    java.time.ZoneOffset.ofTotalSeconds((cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET)) / 1000));
        } else if (sourceObject instanceof Long) {
            offsetDateTime = java.time.OffsetDateTime.ofInstant(
                    java.time.Instant.ofEpochSecond((Long) sourceObject), getDefaultZoneOffset());
        } else {
            throw ConversionException.couldNotBeConverted(sourceObject, ClassConstants.TIME_ODATETIME);
        }

        return offsetDateTime;
    }

    /**
     * INTERNAL: Build a valid instance of java.time.OffsetTime from the given
     * source object.
     *
     * @param sourceObject
     *            Valid object of class java.sql.Timestamp, String,
     *            java.util.Date, or Long
     */
    protected java.time.OffsetTime convertObjectToOffsetTime(Object sourceObject) throws ConversionException {
        java.time.OffsetTime offsetTime = null;

        if (sourceObject instanceof java.time.OffsetTime) {
            return (java.time.OffsetTime) sourceObject;
        }

        if (sourceObject instanceof String) {
            try {
                offsetTime = java.time.OffsetTime.parse(((String) sourceObject).replace(' ', 'T'), Helper.getDefaultDateTimeFormatter());
            } catch (Exception e) {
                java.time.LocalTime localTime = java.time.LocalTime.parse(((String) sourceObject).replace(' ', 'T'), Helper.getDefaultDateTimeFormatter());
                offsetTime = java.time.OffsetTime.of(localTime, java.time.OffsetDateTime.now().getOffset());
            }
        } else if (sourceObject instanceof java.util.Date) {
            // handles sql.Time, sql.Date, sql.Timestamp
            Calendar cal = Helper.allocateCalendar();
            cal.setTime((java.util.Date) sourceObject);
            offsetTime = java.time.OffsetTime.of(
                    cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND), cal.get(Calendar.MILLISECOND),
                    java.time.ZoneOffset.ofTotalSeconds((cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET)) / 1000));
            Helper.releaseCalendar(cal);
        } else if (sourceObject instanceof Calendar cal) {
            offsetTime = java.time.OffsetTime.of(
                    cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND), cal.get(Calendar.MILLISECOND),
                    java.time.ZoneOffset.ofTotalSeconds((cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET)) / 1000));
        } else if (sourceObject instanceof Long) {
            offsetTime = java.time.OffsetTime.ofInstant(
                    java.time.Instant.ofEpochSecond((Long) sourceObject), getDefaultZoneOffset());
        } else {
            throw ConversionException.couldNotBeConverted(sourceObject, ClassConstants.TIME_OTIME);
        }

        return offsetTime;
    }

    /**
     * INTERNAL: Build a valid instance of java.time.Year from the given
     * source object.
     *
     * @param sourceObject
     *            Valid object of class java.sql.Timestamp, String,
     *            java.util.Date or Int
     */
    protected java.time.Year convertObjectToYear(Object sourceObject) throws ConversionException {
        java.time.Year year = null;

        if (sourceObject instanceof java.time.Year) {
            return (java.time.Year) sourceObject;
        }

        if (sourceObject instanceof String) {
            try {
                year = java.time.Year.of(Integer.valueOf((String) sourceObject));
            } catch (Exception e) {
                throw ConversionException.couldNotBeConverted(sourceObject, ClassConstants.TIME_YEAR);
            }
        } else if (sourceObject instanceof java.util.Date) {
            // handles sql.Time, sql.Date, sql.Timestamp
            Calendar cal = Helper.allocateCalendar();
            cal.setTime((java.util.Date) sourceObject);
            year = Year.of(cal.get(Calendar.YEAR));
            Helper.releaseCalendar(cal);
        } else if (sourceObject instanceof Calendar cal) {
            year = Year.of(cal.get(Calendar.YEAR));
        } else if (sourceObject instanceof Integer) {
            year = Year.of((Integer)sourceObject);
        } else if (sourceObject instanceof Long) {
            //Not 100% safe, but JDBC should return Long, but java.time.Year keeps value internally in int variable.
            year = Year.of(((Long)sourceObject).intValue());
        } else if (sourceObject instanceof Number) {
            // Handle BigDecimal and other Number types
            year = Year.of(((Number)sourceObject).intValue());
        } else {
            throw ConversionException.couldNotBeConverted(sourceObject, ClassConstants.TIME_YEAR);
        }

        return year;
    }

    /**
     * INTERNAL:
     * Build a valid instance of java.net.URL from the given source object.
     * @param sourceObject    Valid instance of java.net.URL, or String
     */
    protected URL convertObjectToUrl(Object sourceObject) throws ConversionException {
        if(sourceObject.getClass() == CoreClassConstants.URL_Class) {
            return (URL) sourceObject;
        } else if (sourceObject.getClass() == CoreClassConstants.STRING) {
            try {
                return new URL((String) sourceObject);
            } catch(Exception e) {
                throw ConversionException.couldNotBeConverted(sourceObject, CoreClassConstants.URL_Class, e);
            }
        } else {
            throw ConversionException.couldNotBeConverted(sourceObject, CoreClassConstants.URL_Class);
        }
    }

    /**
     * INTERNAL:
     * Build a valid instance of java.util.UUID from the given source object.
     * @param sourceObject    Valid instance of java.util.UUID, or String
     */
    protected UUID convertObjectToUUID(Object sourceObject) throws ConversionException {
        if(sourceObject.getClass() == CoreClassConstants.UUID) {
            return (UUID) sourceObject;
        } else if (sourceObject.getClass() == CoreClassConstants.STRING) {
            try {
                return UUID.fromString((String) sourceObject);
            } catch(Exception e) {
                throw ConversionException.couldNotBeConverted(sourceObject, CoreClassConstants.UUID, e);
            }
        } else if (sourceObject.getClass() == CoreClassConstants.APBYTE) {
            ByteBuffer byteBuffer = ByteBuffer.wrap((byte[]) sourceObject);
            long high = byteBuffer.getLong();
            long low = byteBuffer.getLong();
            return new UUID(high, low);
        }
        throw ConversionException.couldNotBeConverted(sourceObject, CoreClassConstants.UUID);
    }

    /**
     * INTERNAL:
     * Build a valid instance of java.util.Date from the given source object.
     * @param sourceObject    Valid instance of java.util.Date, String, java.sql.Timestamp, or Long
     */
    protected java.util.Date convertObjectToUtilDate(Object sourceObject) throws ConversionException {
        java.util.Date date = null;

        if (sourceObject.getClass() == java.util.Date.class) {
            date = (java.util.Date)sourceObject;//used when converting util.Date to Calendar
        } else if (sourceObject instanceof java.sql.Date) {
            date = Helper.utilDateFromSQLDate((java.sql.Date)sourceObject);
        } else if (sourceObject instanceof java.sql.Time) {
            date = Helper.utilDateFromTime((java.sql.Time)sourceObject);
        } else if (sourceObject instanceof String) {
            date = Helper.utilDateFromTimestamp(Helper.timestampFromString((String)sourceObject));
        } else if (sourceObject instanceof java.sql.Timestamp) {
            date = Helper.utilDateFromTimestamp((java.sql.Timestamp)sourceObject);
        } else if (sourceObject instanceof Calendar) {
            return ((Calendar)sourceObject).getTime();
        } else if (sourceObject instanceof Long) {
            date = Helper.utilDateFromLong((Long)sourceObject);
        } else if (sourceObject instanceof java.util.Date) {
            date = new java.util.Date(((java.util.Date) sourceObject).getTime());
        } else if (sourceObject instanceof LocalDateTime) {
            date = Helper.utilDateFromTimestamp(java.sql.Timestamp.valueOf((LocalDateTime) sourceObject));
        } else {
            throw ConversionException.couldNotBeConverted(sourceObject, CoreClassConstants.UTILDATE);
        }
        return date;
    }

    /**
     * PUBLIC:
     * Resolve the given String className into a class using this
     * ConversionManager's classloader.
     */
    public <T> Class<T> convertClassNameToClass(String className) throws ConversionException {
        return convertObjectToClass(className);
    }

    /**
     * A singleton conversion manager is used to handle generic conversions.
     * This should not be used for conversion under the session context, these must go through the platform.
     * This allows for the singleton to be customized through setting the default to a user defined subclass.
     */
    public static ConversionManager getDefaultManager() {
        if (defaultManager == null) {
            setDefaultManager(new ConversionManager());
            defaultManager.setShouldUseClassLoaderFromCurrentThread(true);
        }
        return defaultManager;
    }

    /**
     * INTERNAL:
     * Allow for the null values for classes to be defaulted in one place.
     * Any nulls read from the database to be converted to the class will be given the specified null value.
     */
    @SuppressWarnings({"unchecked"})
    public <T> T getDefaultNullValue(Class<T> theClass) {
        if (this.defaultNullValues == null) return null;
        return (T) getDefaultNullValues().get(theClass);
    }

    /**
     * INTERNAL:
     * Allow for the null values for classes to be defaulted in one place.
     * Any nulls read from the database to be converted to the class will be given the specified null value.
     */
    public Map<Class<?>, Object> getDefaultNullValues() {
        return defaultNullValues;
    }

    /**
     * INTERNAL:
     */
    @Override
    public ClassLoader getLoader() {
        if (shouldUseClassLoaderFromCurrentThread()) {
            return PrivilegedAccessHelper.callDoPrivileged(
                    () -> PrivilegedAccessHelper.getContextClassLoader(Thread.currentThread())
            );
        }
        if (loader == null) {
            if (defaultLoader == null) {
                //CR 2621
                final ClassLoader loader = PrivilegedAccessHelper.callDoPrivileged(
                        () -> PrivilegedAccessHelper.getClassLoaderForClass(ClassConstants.ConversionManager_Class)
                );
                setLoader(loader);
            } else {
                setLoader(getDefaultLoader());
            }
        }
        return loader;
    }

    /**
     * INTERNAL
     */
    public boolean hasDefaultNullValues(){
        return this.defaultNullValues != null;
    }

    /**
     * INTERNAL:
     * Load the class using the default managers class loader.
     * This is a thread based class loader by default.
     * This should be used to load all classes as Class.forName can only
     * see classes on the same classpath as the eclipselink.jar.
     */
    @SuppressWarnings({"unchecked"})
    public static <T> Class<T> loadClass(String className) {
        return (Class<T>) getDefaultManager().convertObject(className, CoreClassConstants.CLASS);
    }

    /**
     * INTERNAL:
     * This is used to determine the wrapper class for a primitive.
     */
    @SuppressWarnings({"unchecked"})
    public static <T> Class<T> getObjectClass(Class<?> javaClass) {
        // Null means unknown always for classifications.
        if (javaClass == null) {
            return null;
        }

        if (javaClass.isPrimitive()) {
            if (javaClass == CoreClassConstants.PCHAR) {
                return (Class<T>) CoreClassConstants.CHAR;
            }
            if (javaClass == CoreClassConstants.PINT) {
                return (Class<T>) CoreClassConstants.INTEGER;
            }
            if (javaClass == CoreClassConstants.PDOUBLE) {
                return (Class<T>) CoreClassConstants.DOUBLE;
            }
            if (javaClass == CoreClassConstants.PFLOAT) {
                return (Class<T>) CoreClassConstants.FLOAT;
            }
            if (javaClass == CoreClassConstants.PLONG) {
                return (Class<T>) CoreClassConstants.LONG;
            }
            if (javaClass == CoreClassConstants.PSHORT) {
                return (Class<T>) CoreClassConstants.SHORT;
            }
            if (javaClass == CoreClassConstants.PBYTE) {
                return (Class<T>) CoreClassConstants.BYTE;
            }
            if (javaClass == CoreClassConstants.PBOOLEAN) {
                return (Class<T>) CoreClassConstants.BOOLEAN;
            }
            } else if (javaClass == CoreClassConstants.APBYTE) {
                return (Class<T>) CoreClassConstants.APBYTE;
            } else if (javaClass == CoreClassConstants.APCHAR) {
                return (Class<T>) CoreClassConstants.APCHAR;
            } else {
                return (Class<T>) javaClass;
            }

        return (Class<T>) javaClass;
    }

    /**
     * INTERNAL:
     * Returns a class based on the passed in string.
     */
    @SuppressWarnings({"unchecked"})
    public static <T> Class<T> getPrimitiveClass(String classType) {
        return switch (classType) {
            case "int" -> (Class<T>) Integer.TYPE;
            case "boolean" -> (Class<T>) Boolean.TYPE;
            case "char" -> (Class<T>) Character.TYPE;
            case "short" -> (Class<T>) Short.TYPE;
            case "byte" -> (Class<T>) Byte.TYPE;
            case "float" -> (Class<T>) Float.TYPE;
            case "double" -> (Class<T>) Double.TYPE;
            case "long" -> (Class<T>) Long.TYPE;
            default -> null;
        };

    }

    /**
     * A singleton conversion manager is used to handle generic conversions.
     * This should not be used for conversion under the session context, these must go through the platform.
     * This allows for the singleton to be customized through setting the default to a user defined subclass.
     */
    public static void setDefaultManager(ConversionManager theManager) {
        defaultManager = theManager;
    }

    /**
     * INTERNAL:
     * Allow for the null values for classes to be defaulted in one place.
     * Any nulls read from the database to be converted to the class will be given the specified null value.
     * Primitive null values should be set to the wrapper class.
     */
    public void setDefaultNullValue(Class<?> theClass, Object theValue) {
        if (this.defaultNullValues == null){
            this.defaultNullValues = new HashMap<>(5);
        }
        getDefaultNullValues().put(theClass, theValue);
    }

    /**
     * INTERNAL:
     * Allow for the null values for classes to be defaulted in one place.
     * Any nulls read from the database to be converted to the class will be given the specified null value.
     */
    public void setDefaultNullValues(Map<Class<?>, Object> defaultNullValues) {
        this.defaultNullValues = defaultNullValues;
    }

    /**
     * INTERNAL:
     */
    public void setLoader(ClassLoader classLoader) {
        shouldUseClassLoaderFromCurrentThread = false;
        loader = classLoader;
    }

    /**
     * INTERNAL:
     * Set the default class loader to use if no instance-level loader is set
     */
    public static void setDefaultLoader(ClassLoader classLoader) {
        defaultLoader = classLoader;
    }

    /**
     * INTERNAL:
     * Get the default class loader to use if no instance-level loader is set
     * @return java.lang.ClassLoader
     */
    public static ClassLoader getDefaultLoader() {
        return defaultLoader;
    }

    /**
     * ADVANCED:
     * This flag should be set if the current thread classLoader should be used.
     * This is the case in certain Application Servers were the class loader must be
     * retrieved from the current Thread.  If classNotFoundExceptions are being thrown then set
     * this flag.  In certain cases it will resolve the problem
     */
    public void setShouldUseClassLoaderFromCurrentThread(boolean useCurrentThread) {
        this.shouldUseClassLoaderFromCurrentThread = useCurrentThread;
    }

    /**
     * ADVANCED:
     *  This flag should be set if the current thread classLoader should be used.
     * This is the case in certain Application Servers were the class loader must be
     * retrieved from the current Thread.  If classNotFoundExceptions are being thrown then set
     * this flag.  In certain cases it will resolve the problem
     */
    public boolean shouldUseClassLoaderFromCurrentThread() {
        return this.shouldUseClassLoaderFromCurrentThread;
    }

    /**
     * PUBLIC:
     * Return the list of Classes that can be converted to from the passed in javaClass.
     * @param javaClass - the class that is converted from
     * @return - a vector of classes
     */
    public List<Class<?>> getDataTypesConvertedFrom(Class<?> javaClass) {
        if (dataTypesConvertedFromAClass.isEmpty()) {
            buildDataTypesConvertedFromAClass();
        }
        return dataTypesConvertedFromAClass.get(javaClass);
    }

    /**
     * PUBLIC:
     * Return the list of Classes that can be converted from to the passed in javaClass.
     * @param javaClass - the class that is converted to
     * @return - a vector of classes
     */
    public List<Class<?>> getDataTypesConvertedTo(Class<?> javaClass) {
        if (dataTypesConvertedToAClass.isEmpty()) {
            buildDataTypesConvertedToAClass();
        }
        return dataTypesConvertedToAClass.get(javaClass);
    }

    protected List<Class<?>> buildNumberVec() {
        List<Class<?>> vec = new CopyOnWriteArrayList<>();
        vec.add(BigInteger.class);
        vec.add(BigDecimal.class);
        vec.add(Byte.class);
        vec.add(Double.class);
        vec.add(Float.class);
        vec.add(Integer.class);
        vec.add(Long.class);
        vec.add(Short.class);
        vec.add(Number.class);
        return vec;
    }

    protected List<Class<?>> buildDateTimeVec() {
        List<Class<?>> vec = new CopyOnWriteArrayList<>();
        vec.add(java.util.Date.class);
        vec.add(Timestamp.class);
        vec.add(Calendar.class);
        return vec;
    }

    protected void buildDataTypesConvertedFromAClass() {
        dataTypesConvertedFromAClass.put(BigDecimal.class, buildFromBigDecimalVec());
        dataTypesConvertedFromAClass.put(BigInteger.class, buildFromBigIntegerVec());
        dataTypesConvertedFromAClass.put(Blob.class, buildFromBlobVec());
        dataTypesConvertedFromAClass.put(Boolean.class, buildFromBooleanVec());
        dataTypesConvertedFromAClass.put(byte[].class, buildFromByteArrayVec());
        dataTypesConvertedFromAClass.put(Byte.class, buildFromByteVec());
        dataTypesConvertedFromAClass.put(Calendar.class, buildFromCalendarVec());
        dataTypesConvertedFromAClass.put(Character.class, buildFromCharacterVec());
        dataTypesConvertedFromAClass.put(Clob.class, buildFromClobVec());
        dataTypesConvertedFromAClass.put(java.sql.Date.class, buildFromDateVec());
        dataTypesConvertedFromAClass.put(Double.class, buildFromDoubleVec());
        dataTypesConvertedFromAClass.put(Float.class, buildFromFloatVec());
        dataTypesConvertedFromAClass.put(Integer.class, buildFromIntegerVec());
        dataTypesConvertedFromAClass.put(Long.class, buildFromLongVec());
        dataTypesConvertedFromAClass.put(Number.class, buildFromNumberVec());
        dataTypesConvertedFromAClass.put(Short.class, buildFromShortVec());
        dataTypesConvertedFromAClass.put(String.class, buildFromStringVec());
        dataTypesConvertedFromAClass.put(Timestamp.class, buildFromTimestampVec());
        dataTypesConvertedFromAClass.put(Time.class, buildFromTimeVec());
        dataTypesConvertedFromAClass.put(java.util.Date.class, buildFromUtilDateVec());
        dataTypesConvertedFromAClass.put(Byte[].class, buildFromByteObjectArraryVec());
        dataTypesConvertedFromAClass.put(char[].class, buildFromCharArrayVec());
        dataTypesConvertedFromAClass.put(Character[].class, buildFromCharacterArrayVec());
    }

    protected List<Class<?>> buildFromBooleanVec() {
        List<Class<?>> vec = new CopyOnWriteArrayList<>();
        vec.add(String.class);
        vec.add(Boolean.class);
        vec.add(Integer.class);
        vec.add(Long.class);
        vec.add(Short.class);
        vec.add(Number.class);
        vec.add(Character[].class);
        vec.add(char[].class);
        vec.add(boolean.class);
        vec.add(int.class);
        vec.add(long.class);
        vec.add(short.class);
        return vec;
    }

    protected List<Class<?>> buildFromNumberVec() {
        List<Class<?>> vec = buildNumberVec();
        vec.add(String.class);
        vec.add(Character.class);
        vec.add(Boolean.class);
        vec.add(Character[].class);
        vec.add(char[].class);
        vec.add(char.class);
        vec.add(int.class);
        vec.add(double.class);
        vec.add(float.class);
        vec.add(long.class);
        vec.add(short.class);
        vec.add(byte.class);
        vec.add(boolean.class);
        return vec;
    }

    protected List<Class<?>> buildFromBigDecimalVec() {
        return buildFromNumberVec();
    }

    protected List<Class<?>> buildFromBigIntegerVec() {
        return buildFromNumberVec();
    }

    protected List<Class<?>> buildFromIntegerVec() {
        return buildFromNumberVec();
    }

    protected List<Class<?>> buildFromFloatVec() {
        return buildFromNumberVec();
    }

    protected List<Class<?>> buildFromDoubleVec() {
        return buildFromNumberVec();
    }

    protected List<Class<?>> buildFromShortVec() {
        return buildFromNumberVec();
    }

    protected List<Class<?>> buildFromByteVec() {
        return buildFromNumberVec();
    }

    protected List<Class<?>> buildFromLongVec() {
        List<Class<?>> vec = buildFromNumberVec();
        vec.addAll(buildDateTimeVec());
        vec.add(java.sql.Date.class);
        vec.add(Time.class);
        return vec;
    }

    protected List<Class<?>> buildFromStringVec() {
        List<Class<?>> vec = buildFromLongVec();
        vec.add(Byte[].class);
        vec.add(byte[].class);
        vec.add(Clob.class);
        return vec;
    }

    protected List<Class<?>> buildFromCharacterVec() {
        List<Class<?>> vec = new CopyOnWriteArrayList<>();
        vec.add(String.class);
        vec.add(Boolean.class);
        vec.add(Character[].class);
        vec.add(Character.class);
        vec.add(char[].class);
        vec.add(char.class);
        vec.add(boolean.class);
        return vec;
    }

    protected List<Class<?>> buildFromByteArrayVec() {
        List<Class<?>> vec = new CopyOnWriteArrayList<>();
        vec.add(String.class);
        vec.add(byte[].class);
        vec.add(Byte[].class);
        vec.add(Character[].class);
        vec.add(char[].class);
        return vec;
    }

    protected List<Class<?>> buildFromClobVec() {
        List<Class<?>> vec = new CopyOnWriteArrayList<>();
        vec.add(String.class);
        vec.add(Character[].class);
        vec.add(char[].class);
        return vec;
    }

    protected List<Class<?>> buildFromBlobVec() {
        List<Class<?>> vec = new CopyOnWriteArrayList<>();
        vec.add(String.class);
        vec.add(Byte[].class);
        vec.add(byte[].class);
        vec.add(Character[].class);
        vec.add(char[].class);
        return vec;
    }

    protected List<Class<?>> buildFromUtilDateVec() {
        List<Class<?>> vec = buildDateTimeVec();
        vec.add(String.class);
        vec.add(Long.class);
        vec.add(java.sql.Date.class);
        vec.add(Time.class);
        vec.add(long.class);
        vec.add(Character[].class);
        vec.add(char[].class);
        return vec;
    }

    protected List<Class<?>>buildFromTimestampVec() {
        return buildFromUtilDateVec();
    }

    protected List<Class<?>> buildFromCalendarVec() {
        return buildFromUtilDateVec();
    }

    protected List<Class<?>> buildFromDateVec() {
        List<Class<?>> vec = buildDateTimeVec();
        vec.add(String.class);
        vec.add(Long.class);
        vec.add(java.sql.Date.class);
        vec.add(long.class);
        vec.add(Character[].class);
        vec.add(char[].class);
        return vec;
    }

    protected List<Class<?>> buildFromTimeVec() {
        List<Class<?>> vec = buildDateTimeVec();
        vec.add(String.class);
        vec.add(Long.class);
        vec.add(Time.class);
        vec.add(long.class);
        vec.add(Character[].class);
        vec.add(char[].class);
        return vec;
    }

    protected List<Class<?>> buildFromByteObjectArraryVec() {
        List<Class<?>> vec = new CopyOnWriteArrayList<>();
        vec.add(Blob.class);
        vec.add(byte[].class);
        return vec;
    }

    protected List<Class<?>> buildFromCharArrayVec() {
        List<Class<?>> vec = new CopyOnWriteArrayList<>();
        vec.add(Clob.class);
        return vec;
    }

    protected List<Class<?>> buildFromCharacterArrayVec() {
        List<Class<?>> vec = new CopyOnWriteArrayList<>();
        vec.add(Clob.class);
        return vec;
    }

    protected void buildDataTypesConvertedToAClass() {
        dataTypesConvertedToAClass.put(BigDecimal.class, buildToBigDecimalVec());
        dataTypesConvertedToAClass.put(BigInteger.class, buildToBigIntegerVec());
        dataTypesConvertedToAClass.put(Boolean.class, buildToBooleanVec());
        dataTypesConvertedToAClass.put(Byte.class, buildToByteVec());
        dataTypesConvertedToAClass.put(byte[].class, buildToByteArrayVec());
        dataTypesConvertedToAClass.put(Byte[].class, buildToByteObjectArrayVec());
        dataTypesConvertedToAClass.put(Calendar.class, buildToCalendarVec());
        dataTypesConvertedToAClass.put(Character.class, buildToCharacterVec());
        dataTypesConvertedToAClass.put(Character[].class, buildToCharacterArrayVec());
        dataTypesConvertedToAClass.put(char[].class, buildToCharArrayVec());
        dataTypesConvertedToAClass.put(java.sql.Date.class, buildToDateVec());
        dataTypesConvertedToAClass.put(Double.class, buildToDoubleVec());
        dataTypesConvertedToAClass.put(Float.class, buildToFloatVec());
        dataTypesConvertedToAClass.put(Integer.class, buildToIntegerVec());
        dataTypesConvertedToAClass.put(Long.class, buildToLongVec());
        dataTypesConvertedToAClass.put(Number.class, buildToNumberVec());
        dataTypesConvertedToAClass.put(Short.class, buildToShortVec());
        dataTypesConvertedToAClass.put(String.class, buildToStringVec());
        dataTypesConvertedToAClass.put(Timestamp.class, buildToTimestampVec());
        dataTypesConvertedToAClass.put(Time.class, buildToTimeVec());
        dataTypesConvertedToAClass.put(java.util.Date.class, buildToUtilDateVec());
        dataTypesConvertedToAClass.put(Clob.class, buildToClobVec());
        dataTypesConvertedToAClass.put(Blob.class, buildToBlobVec());
    }

    protected List<Class<?>> buildAllTypesToAClassVec() {
        List<Class<?>> vec = new CopyOnWriteArrayList<>();
        vec.add(String.class);
        vec.add(Integer.class);
        vec.add(java.util.Date.class);
        vec.add(java.sql.Date.class);
        vec.add(Time.class);
        vec.add(Timestamp.class);
        vec.add(Calendar.class);
        vec.add(Character.class);
        vec.add(Double.class);
        vec.add(Float.class);
        vec.add(Long.class);
        vec.add(Short.class);
        vec.add(Byte.class);
        vec.add(BigInteger.class);
        vec.add(BigDecimal.class);
        vec.add(Number.class);
        vec.add(Boolean.class);
        vec.add(Character[].class);
        vec.add(Blob.class);
        vec.add(Clob.class);
        return vec;
    }

    protected List<Class<?>> buildToBigDecimalVec() {
        List<Class<?>> vec = buildNumberVec();
        vec.add(String.class);
        return vec;
    }

    protected List<Class<?>> buildToBigIntegerVec() {
        return buildToBigDecimalVec();
    }

    protected List<Class<?>> buildToBooleanVec() {
        List<Class<?>> vec = buildToBigDecimalVec();
        vec.add(Character.class);
        vec.add(Boolean.class);
        return vec;
    }

    protected List<Class<?>> buildToByteVec() {
        return buildToBigDecimalVec();
    }

    protected List<Class<?>> buildToDoubleVec() {
        return buildToBigDecimalVec();
    }

    protected List<Class<?>> buildToFloatVec() {
        return buildToBigDecimalVec();
    }

    protected List<Class<?>> buildToIntegerVec() {
        List<Class<?>> vec = buildToBigDecimalVec();
        vec.add(Boolean.class);
        return vec;
    }

    protected List<Class<?>> buildToLongVec() {
        List<Class<?>> vec = buildToIntegerVec();
        vec.add(Calendar.class);
        vec.add(java.util.Date.class);
        return vec;
    }

    protected List<Class<?>> buildToNumberVec() {
        return buildToIntegerVec();
    }

    protected List<Class<?>> buildToShortVec() {
        return buildToIntegerVec();
    }

    protected List<Class<?>> buildToByteArrayVec() {
        List<Class<?>> vec = new CopyOnWriteArrayList<>();
        vec.add(String.class);
        vec.add(Blob.class);
        vec.add(byte[].class);
        vec.add(Byte[].class);
        return vec;
    }

    protected List<Class<?>> buildToByteObjectArrayVec() {
        List<Class<?>> vec = buildToByteArrayVec();
        vec.add(Byte[].class);
        return vec;
    }

    protected List<Class<?>> buildToCharacterVec() {
        List<Class<?>> vec = buildToBigDecimalVec();
        vec.add(Character.class);
        return vec;
    }

    protected List<Class<?>> buildToCharacterArrayVec() {
        return buildAllTypesToAClassVec();
    }

    protected List<Class<?>> buildToCharArrayVec() {
        return buildAllTypesToAClassVec();
    }

    protected List<Class<?>> buildToStringVec() {
        return buildAllTypesToAClassVec();
    }

    protected List<Class<?>> buildToCalendarVec() {
        List<Class<?>> vec = buildDateTimeVec();
        vec.add(String.class);
        vec.add(Long.class);
        vec.add(java.sql.Date.class);
        vec.add(Time.class);
        return vec;
    }

    protected List<Class<?>> buildToTimestampVec() {
        return buildToCalendarVec();
    }

    protected List<Class<?>> buildToUtilDateVec() {
        return buildToCalendarVec();
    }

    protected List<Class<?>> buildToDateVec() {
        List<Class<?>> vec = buildDateTimeVec();
        vec.add(String.class);
        vec.add(Long.class);
        vec.add(java.sql.Date.class);
        return vec;
    }

    protected List<Class<?>> buildToTimeVec() {
        List<Class<?>> vec = buildDateTimeVec();
        vec.add(String.class);
        vec.add(Long.class);
        vec.add(Time.class);
        return vec;
    }

    protected List<Class<?>> buildToBlobVec() {
        List<Class<?>> vec = new CopyOnWriteArrayList<>();
        vec.add(Byte[].class);
        vec.add(byte[].class);
        return vec;
    }

    protected List<Class<?>> buildToClobVec() {
        List<Class<?>> vec = new CopyOnWriteArrayList<>();
        vec.add(String.class);
        vec.add(char[].class);
        vec.add(Character[].class);
        return vec;
    }
}
