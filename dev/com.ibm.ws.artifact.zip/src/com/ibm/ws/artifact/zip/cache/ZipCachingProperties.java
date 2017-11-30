/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2012, 2017
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.artifact.zip.cache;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.artifact.zip.cache.internal.ZipFileHandleImpl;
import com.ibm.wsspi.kernel.service.utils.SystemUtils;

/**
 * Zip caching properties.
 *
 * This class also provides static utility methods for debugging.
 *
 * Three sets of java custom properties are provided to configure
 * the zip caching service.
 *
 * (The zip caching service is a static singleton: Zip caching service
 * properties are read from system properties when the caching service
 * is initialization.  Changes to the system properties will have no
 * effect after the zip caching service are initialized.)
 *
 * Property Summaries:
 *
 * One properties are used to enable state debugging for the service:
 *
 * zip.cache.debug.state      true | false   [ false ]
 *
 * Three properties are used to configure the zip file handle cache
 * layer:
 *
 * zip.cache.handle.max       -1 | (>0)      [ 255 ]
 * zip.cache.entry.limit       0 | (>0)      [ 8192 ]
 * zip.cache.entry.max         0 | (>0)      [ 16 ]
 *
 * Three properties are used to configure the zip file cache layer:
 *
 * zip.reaper.max.pending     -1 | 0 | (>0)  [ 255 ]
 * zip.reaper.short.interval  (>0)           [ 100,000,000 ns ] [ 0.1s ]
 * zip.reaper.long.interval   (>0)           [ 200,000,000 ns ] [ 0.2s ]
 *
 * Property Details:
 *
 * zip.cache.debug.state      true | false   [ false ]
 *
 * This property enables state tracking of the zip file cache layer.
 * When this property is enabled, a history is created for each zip file
 * which is opened, and, as a shutdown step, the history information is
 * logged.
 *
 * Setting the state tracking property increases memory usage in proportion
 * to the number of unique zip files which are opened using the zip caching
 * service.  Usually, this reaches a maximum value during application startup.
 * Frequently replacement of applications with new unique zip files will
 * cause a steady increase of memory usage.
 *
 * This property places trace to the usual trace but with INFO type enablement.
 * A system property is used to enable state trace because the enablement has
 * a larger impact than usual trace enablement.
 *
 * The zip cache service has three cache layers: A cache of zip file handles,
 * a cache of data for small classes (per zip file handle), and a cache of zip
 * files.
 *
 * zip.cache.handle.max       -1 | (>0)      [ 255 ]
 *
 * This property sets the maximum number of zip file handles which are
 * held by the zip file handle cache layer.  Setting -1 allows an unlimited
 * number of zip file handles to be held.
 *
 * zip.cache.entry.limit       0 | (>0)      [ 8192 ]
 *
 * The zip file handle layer holds a cache of data for small classes and for
 * the zip file manifest.  The data is held per zip file handle.  The entry
 * limit property sets the size of entry which is held.  Setting 0 disables
 * the entry cache.
 *
 * zip.cache.entry.max         0 | (>0)      [ 16 ]
 *
 * The zip file handle layer holds a cache of data for small classes and
 * for the zip file manifest.  The data is held per zip file handle.  The
 * entry maximum property sets the number of entries for which data is held.
 * Setting 0 disables the entry cache.
 *
 * zip.reaper.max.pending     -1 | 0 | (>0)  [ 255 ]
 *
 * The zip file cache layer delays closes of zip files according to the
 * short and long interval settings.  That creates a queue of pending
 * closes.  The maximum pending property limits the number of closes
 * which are held by the queue of pending closes.  Adding a pending
 * close when the maximum size is reached causes the oldest pending
 * close to be performed immediately.
 *
 * Setting the maximum pending property to -1 allows an unlimited
 * number of pending closes to be held.  Setting the maximum pending
 * property to 0 disables the zip file caching layer, which will cause
 * all closes to be performed immediately.
 *
 * zip.reaper.short.interval  (>0)           [ 100,000,000 ns ] [ 0.1s ]
 * zip.reaper.long.interval   (>0)           [ 200,000,000 ns ] [ 0.2s ]
 *
 * The short interval and long interval properties determine how long a
 * close is held before performing the close.
 *
 * The meaning of the two interval values relates to the zip close mechanism:
 * Upon receipt of a close request, and following any processing of a close
 * which has been closed, the thread which performs closes is slept for
 * the minimum of the remaining time for pending closes.  That is, the long
 * interval value minus any time already spent waiting.  However, when
 * the close thread awakens, closes are performed on all pending closes
 * which have waited at least the short interval time.
 *
 * This use of two intervals is used to prevent cases where a sequence of
 * opens and closes is performed in close succession.  If a single interval
 * value was used, that would cause the thread which performs closes to
 * awaken, perform a close, then sleep for a very short duration before
 * moving to the next close.  This would add significant overhead to
 * management of the close thread.
 *
 * The short and long interval values are not used if the zip file cache is
 * disabled.  The short and long interval values must always be greater than
 * zero, and the long interval value must always be greater than the short
 * interval value.
 *
 * The short and long interval values are specified in nano-seconds (ns):
 * 1,000,000,000 ns is 1 s, 100,000,000 ns is 0.1 s.
 */
public class ZipCachingProperties {
    private static final TraceComponent tc = Tr.register(ZipFileHandleImpl.class);

    //

    @Trivial
    public static boolean getProperty(String methodName, String propertyName, boolean propertyDefaultValue) {
        String propertyText = SystemUtils.getProperty(propertyName);
        boolean propertyValue;
        boolean propertyDefaulted;
        if ( propertyDefaulted = (propertyText == null) ) {
            propertyValue = propertyDefaultValue;
        } else {
            propertyValue = Boolean.parseBoolean(propertyText);
        }
        debugProperty(methodName, propertyName, propertyValue, propertyDefaulted);
        return propertyValue;
    }

    @Trivial
    public static int getProperty(String methodName, String propertyName, int propertyDefaultValue) {
        String propertyText = SystemUtils.getProperty(propertyName);
        int propertyValue;
        boolean propertyDefaulted;
        if ( propertyDefaulted = (propertyText == null) ) {
            propertyValue = propertyDefaultValue;
        } else {
            propertyValue = Integer.parseInt(propertyText);
        }
        debugProperty(methodName, propertyName, propertyValue, propertyDefaulted);
        return propertyValue;
    }

    @Trivial
    public static long getProperty(String methodName, String propertyName, long propertyDefaultValue) {
        String propertyText = SystemUtils.getProperty(propertyName);
        long propertyValue;
        boolean propertyDefaulted;
        if ( propertyDefaulted = (propertyText == null) ) {
            propertyValue = propertyDefaultValue;
        } else {
            propertyValue = Long.parseLong(propertyText);
        }
        debugProperty(methodName, propertyName, propertyValue, propertyDefaulted);
        return propertyValue;
    }

    @Trivial
    public static String getProperty(String methodName, String propertyName, String propertyDefaultValue) {
        String propertyText = SystemUtils.getProperty(propertyName);
        String propertyValue;
        boolean propertyDefaulted;
        if ( propertyDefaulted = (propertyText == null) ) {
            propertyValue = propertyDefaultValue;
        } else {
            propertyValue = propertyText;
        }
        debugProperty(methodName, propertyName, propertyValue, propertyDefaulted);
        return propertyValue;
    }

    @Trivial
    public static void debugProperty(
            String methodName,
            String propertyName, boolean propertyValue, boolean defaulted) {
        debugProperty(methodName, propertyName, Boolean.valueOf(propertyValue), defaulted);
    }

    @Trivial
    public static void debugProperty(
            String methodName,
            String propertyName, int propertyValue, boolean defaulted) {
        debugProperty(methodName, propertyName, Integer.valueOf(propertyValue), defaulted);
    }

    @Trivial
    public static void debugProperty(
            String methodName,
            String propertyName, long propertyValue, boolean defaulted) {
        debugProperty(methodName, propertyName, Long.valueOf(propertyValue), defaulted);
    }

    public static final String SOURCE_DEFAULTED = "defaulted";
    public static final String SOURCE_PROPERTY = "system property";

    @Trivial
    public static void debugProperty(
            String methodName,
            String propertyName, Object propertyValue, boolean defaulted) {

        if ( tc.isDebugEnabled() ) {
            String propertyText =
                "Property [ " + propertyName + " ]" +
                " [ " + propertyValue + " ]" +
                " (" + (defaulted ? SOURCE_DEFAULTED : SOURCE_PROPERTY) + ")";
            Tr.debug(tc, methodName, propertyText);
        }
    }

    //

    /**
     * Property for zip file cache state logging enablement.
     *
     * Enabling state debug has three consequences:
     *
     * <ul>
     * <li>All zip data is retained forever.</li>
     * <li>A shutdown thread is created and registered as a shutdown thread.</li>
     * <li>The shutdown thread processes all zip data, forcing all to be
     * closed, and displaying lifetime statistics for all.</li>
     * </ul>
     */
    public static final String ZIP_CACHE_DEBUG_STATE_PROPERTY_NAME = "zip.cache.debug.state";
    public static final boolean ZIP_CACHE_DEBUG_STATE_DEFAULT_VALUE = false;
    public static final boolean ZIP_CACHE_DEBUG_STATE;

    static {
        String methodName = "<static init>";

        ZIP_CACHE_DEBUG_STATE = getProperty(methodName,
            ZIP_CACHE_DEBUG_STATE_PROPERTY_NAME, ZIP_CACHE_DEBUG_STATE_DEFAULT_VALUE);
    }

    //

    /** The maximum number of zip file handles to keep in the cache. */
    public static final String ZIP_CACHE_HANDLE_MAX_PROPERTY_NAME = "zip.cache.handle.max";
    public static final int ZIP_CACHE_HANDLE_MAX_DEFAULT_VALUE = 255;
    public static final int ZIP_CACHE_HANDLE_MAX;

    /** The largest sized entry to keep in the per-handle entry cache. */
    public static final String ZIP_CACHE_ENTRY_LIMIT_PROPERTY_NAME = "zip.cache.entry.limit";
    public static final int ZIP_CACHE_ENTRY_LIMIT_DEFAULT_VALUE = 8192;
    public static final int ZIP_CACHE_ENTRY_LIMIT;

    /** The maximum number of entries to keep in the per-handle entry cache. */
    public static final String ZIP_CACHE_ENTRY_MAX_PROPERTY_NAME = "zip.cache.entry.max";
    public static final int ZIP_CACHE_ENTRY_MAX_DEFAULT_VALUE = 16;
    public static final int ZIP_CACHE_ENTRY_MAX;

    static {
        String methodName = "<static init>";

        ZIP_CACHE_HANDLE_MAX = getProperty(methodName,
            ZIP_CACHE_HANDLE_MAX_PROPERTY_NAME, ZIP_CACHE_HANDLE_MAX_DEFAULT_VALUE);
        ZIP_CACHE_ENTRY_LIMIT = getProperty(methodName,
            ZIP_CACHE_ENTRY_LIMIT_PROPERTY_NAME, ZIP_CACHE_ENTRY_LIMIT_DEFAULT_VALUE);
        ZIP_CACHE_ENTRY_MAX = getProperty(methodName,
            ZIP_CACHE_ENTRY_MAX_PROPERTY_NAME, ZIP_CACHE_ENTRY_MAX_DEFAULT_VALUE);
    }

    //

    /**
     * Generally, the ZIP cache properties should be left to their
     * default values.
     *
     * The maximum pending value limits the growth of the pending
     * but unclosed zip files collection.  The max pending value does
     * not affect the zip files which are open and are not pending
     * closure.  Zip files which are legitimately still open are
     * left open until a close is requested.
     *
     * The default intervals of 0.1 s to 0.2 s (1/10 s to 1/5 s) were
     * selected based on profiling of server startup.
     */

    /**
     * The maximum number of pending closes to allow.
     * Setting -1 sets no maximum; setting 0 disables the reaper.
     */
    public static final String ZIP_CACHE_REAPER_MAX_PENDING_PROPERTY_NAME =
        "zip.reaper.max.pending";
    public static final int ZIP_CACHE_REAPER_MAX_PENDING_DEFAULT_VALUE = 255;
    public static final int ZIP_CACHE_REAPER_MAX_PENDING;

    /** The minimum time to wait before processing a pending close. */
    public static final String ZIP_CACHE_REAPER_SHORT_INTERVAL_PROPERTY_NAME =
        "zip.reaper.short.interval";
    public static final long ZIP_CACHE_REAPER_SHORT_INTERVAL_DEFAULT_VALUE =
        ZipCachingProperties.ONE_SEC_IN_NANO_SEC / 10;
    public static final long ZIP_CACHE_REAPER_SHORT_INTERVAL;

    /** The maximum time to wait before processing a pending close. */
    public static final String ZIP_CACHE_REAPER_LONG_INTERVAL_PROPERTY_NAME =
        "zip.reaper.long.interval";
    public static final long ZIP_CACHE_REAPER_LONG_INTERVAL_DEFAULT_VALUE =
        ZipCachingProperties.ONE_SEC_IN_NANO_SEC / 5;
    public static final long ZIP_CACHE_REAPER_LONG_INTERVAL;

    static {
        String methodName = "<static init>";

        ZIP_CACHE_REAPER_MAX_PENDING = getProperty(methodName,
            ZIP_CACHE_REAPER_MAX_PENDING_PROPERTY_NAME,
            ZIP_CACHE_REAPER_MAX_PENDING_DEFAULT_VALUE);

        ZIP_CACHE_REAPER_SHORT_INTERVAL = getProperty(methodName,
            ZIP_CACHE_REAPER_SHORT_INTERVAL_PROPERTY_NAME,
            ZIP_CACHE_REAPER_SHORT_INTERVAL_DEFAULT_VALUE);

        ZIP_CACHE_REAPER_LONG_INTERVAL = getProperty(methodName,
            ZIP_CACHE_REAPER_LONG_INTERVAL_PROPERTY_NAME,
            ZIP_CACHE_REAPER_LONG_INTERVAL_DEFAULT_VALUE);
    }

    //

    private static final String PAD = "0000000000";

    @Trivial
    public static String toCount(int count) {
        return toCount(count, 6);
    }

    @Trivial
    public static String toCount(int count, int pad) {
        String digits = Integer.toString(count);
        int numDigits = digits.length();
        if ( numDigits >= pad ) {
            return digits;
        }

        int missing = pad - numDigits;
        if ( missing > PAD.length() ) {
            missing = PAD.length();
        }

        return PAD.substring(0,  missing) + digits;
    }

    //

    public static final long ONE_SEC_IN_NANO_SEC = 1000 * 1000 * 1000;
    public static final long ONE_MILLI_SEC_IN_NANO_SEC = 1000 * 1000;
    public static final long ONE_SEC_IN_MILLI_SEC = 1000;

    //

    /**
     * Display the difference between two nano-seconds values as a
     * decimal seconds value.
     *
     * Display three places after the decimal point, and left
     * the integer value with '0' to a minimum of six characters.
     *
     * @param baseNS The value to subtract from the second parameter.
     * @param actualNS The from which to subtract the first parameter.
     *
     * @return The difference as a seconds value.
     */
    @Trivial
    public static String toRelSec(long baseNS, long actualNS) {
        return toAbsSec(actualNS - baseNS, 6);
    }

    /**
     * Display the difference between two nano-seconds values as a
     * decimal seconds value.
     *
     * Display three places after the decimal point, and left
     * the integer value with '0' to a minimum of the specified
     * number of characters.
     *
     * @param baseNS The value to subtract from the second parameter.
     * @param actualNS The from which to subtract the first parameter.
     * @param pad The number of '0' character to pad to the integer
     *    part of the value.
     *
     * @return The value as a seconds value.
     */
    @Trivial
    public static String toRelSec(long baseNS, long actualNS, int pad) {
        return toAbsSec(actualNS - baseNS, pad);
    }

    /**
     * Display a nano-seconds value as a decimal seconds value.
     *
     * Display three places after the decimal point, and left
     * the integer value with '0' to a minimum of six characters.
     *
     * Negative values are not handled.
     *
     * @param durationNS The duration in nano-seconds to display
     *    as a seconds value.
     * @param pad The number of '0' character to pad to the integer
     *    part of the value.
     * @return The value as a seconds value.
     */
    @Trivial
    public static String toAbsSec(long durationNS) {
        return toAbsSec(durationNS, 6);
    }

    /**
     * Display a nano-seconds value as a decimal seconds value.
     *
     * Display three places after the decimal point, and left
     * the integer value with '0' to a minimum of the specified
     * number of pad characters.
     *
     * Negative values are not handled.
     *
     * @param durationNS The duration in nano-seconds to display
     *    as a seconds value.
     * @param pad The number of '0' character to pad to the integer
     *    part of the value.
     * @return The value as a seconds value.
     */
    @Trivial
    public static String toAbsSec(long durationNS, int pad) {
        String nanoSec = Long.toString(durationNS);
        int nanoDigits = nanoSec.length();
        if ( nanoDigits > 9 ) { // 1 000 000 000 == 1.000 sec
            int secDigits = nanoDigits - 9;
            if ( secDigits >= pad ) {
                return nanoSec.substring(0, secDigits) + "." + nanoSec.substring(secDigits, secDigits + 3);
            } else {
                int padDigits = pad - secDigits;
                if ( padDigits > PAD.length() ) {
                    padDigits = PAD.length();
                }
                return PAD.substring(0, padDigits) + nanoSec.substring(0, secDigits) + "." + nanoSec.substring(secDigits, secDigits + 3);
            }

        } else if ( nanoDigits > 6 ) {
            if ( pad > PAD.length() ) {
                pad = PAD.length();
            }
            int missingDigits = 3 - (nanoDigits - 6);
            return PAD.substring(0, pad) + "." + PAD.substring(0, missingDigits) + nanoSec.substring(0, nanoDigits - 6);

        } else {
            if ( pad > PAD.length() ) {
                pad = PAD.length();
            }
            return PAD.substring(0, pad) + "." + "000";
        }
    }
}
