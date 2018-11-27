/*******************************************************************************
 * Copyright (c) 2012,2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.zip.cache;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.artifact.zip.cache.internal.ZipFileHandleImpl;
import com.ibm.ws.artifact.zip.internal.SystemUtils;

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
 * 
 * zip.reaper.quick.pend.min   (>=0)    [  10,000,000 ns ] [ 0.01s ]
 * zip.reaper.quick.pend.max   (>=0)    [  20,000,000 ns ] [ 0.02s ]
 * zip.reaper.slow.pend.min    (>0)     [ 100,000,000 ns ] [ 0.1s ]
 * zip.reaper.slow.pend.max    (>0)     [ 200,000,000 ns ] [ 0.2s ]
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
 * zip.reaper.quick.pend.min        (>=0)    [  10,000,000 ns ] [ 0.01s ]
 * zip.reaper.quick.pend.max        (>=0)    [  20,000,000 ns ] [ 0.02s ]
 *
 * zip.reaper.slow.pend.min         (>0)     [ 100,000,000 ns ] [ 0.1s ]
 * zip.reaper.slow.pend.max         (>0)     [ 200,000,000 ns ] [ 0.2s ]
 * 
 * The interval properties determine how long a close is held before
 * performing the close.
 *
 * Two sets of interval values are used: The "quick" property values are
 * used for the initial open of zip data.  The "slow" property values are
 * used when zip data is accessed within the slow short interval of the
 * initial open.
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

    //

    /**
     * Property for zip reaper state logging.
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
    public static final String ZIP_REAPDER_DEBUG_STATE_PROPERTY_NAME =
        "zip.reaper.debug.state";
    public static final boolean ZIP_REAPER_DEBUG_STATE_DEFAULT_VALUE = false;
    public static final boolean ZIP_REAPER_DEBUG_STATE;

    public static final String ZIP_REAPER_COLLECT_TIMINGS_PROPERTY_NAME =
        "zip.reaper.collect.timings";
    public static final boolean ZIP_REAPER_COLLECT_TIMINGS_DEFAULT_VALUE = false;
    public static final boolean ZIP_REAPER_COLLECT_TIMINGS;

    static {
        String methodName = "<static init>";

        ZIP_REAPER_COLLECT_TIMINGS = getProperty(methodName,
            ZIP_REAPER_COLLECT_TIMINGS_PROPERTY_NAME,
            ZIP_REAPER_COLLECT_TIMINGS_DEFAULT_VALUE);

        ZIP_REAPER_DEBUG_STATE = getProperty(methodName,
            ZIP_REAPDER_DEBUG_STATE_PROPERTY_NAME,
            ZIP_REAPER_DEBUG_STATE_DEFAULT_VALUE);
    }

    /**
     * Generally, the ZIP cache properties should be left to their
     * default values.
     *
     * The maximum pending value limits the growth of the pending
     * but unclosed zip files collection.  The max pending value does
     * not affect the zip files which are open and are not pending
     * closure.  Zip files which are legitimately still open are
     * left open until a close is requested.
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
    public static final String ZIP_CACHE_REAPER_QUICK_PEND_MIN_PROPERTY_NAME =
        "zip.reaper.quick.pend.min";
    public static final long ZIP_CACHE_REAPER_QUICK_PEND_MIN_DEFAULT_VALUE =
        ZipCachingProperties.NANO_IN_ONE / 100; // 0.01s
    public static final long ZIP_CACHE_REAPER_QUICK_PEND_MIN;

    /** The maximum time to wait before processing a pending close. */
    public static final String ZIP_CACHE_REAPER_QUICK_PEND_MAX_PROPERTY_NAME =
        "zip.reaper.quick.pend.max";
    public static final long ZIP_CACHE_REAPER_QUICK_PEND_MAX_DEFAULT_VALUE =
        ZipCachingProperties.NANO_IN_ONE / 50; // 0.02s
    public static final long ZIP_CACHE_REAPER_QUICK_PEND_MAX;

    /** The minimum time to wait before processing a pending close. */
    public static final String ZIP_CACHE_REAPER_SLOW_PEND_MIN_PROPERTY_NAME =
        "zip.reaper.slow.pend.min";
    public static final long ZIP_CACHE_REAPER_SLOW_PEND_MIN_DEFAULT_VALUE =
        ZipCachingProperties.NANO_IN_ONE / 10; // 0.1s
    public static final long ZIP_CACHE_REAPER_SLOW_PEND_MIN;

    /** The maximum time to wait before processing a pending close. */
    public static final String ZIP_CACHE_REAPER_SLOW_PEND_MAX_PROPERTY_NAME =
        "zip.reaper.slow.pend.max";
    public static final long ZIP_CACHE_REAPER_SLOW_PEND_MAX_DEFAULT_VALUE =
        ZipCachingProperties.NANO_IN_ONE / 5; // 0.2s
    public static final long ZIP_CACHE_REAPER_SLOW_PEND_MAX;

    static {
        String methodName = "<static init>";

        ZIP_CACHE_REAPER_MAX_PENDING = getProperty(methodName,
            ZIP_CACHE_REAPER_MAX_PENDING_PROPERTY_NAME,
            ZIP_CACHE_REAPER_MAX_PENDING_DEFAULT_VALUE);

        ZIP_CACHE_REAPER_QUICK_PEND_MIN = getProperty(methodName,
            ZIP_CACHE_REAPER_QUICK_PEND_MIN_PROPERTY_NAME,
            ZIP_CACHE_REAPER_QUICK_PEND_MIN_DEFAULT_VALUE);

        ZIP_CACHE_REAPER_QUICK_PEND_MAX = getProperty(methodName,
            ZIP_CACHE_REAPER_QUICK_PEND_MAX_PROPERTY_NAME,
            ZIP_CACHE_REAPER_QUICK_PEND_MAX_DEFAULT_VALUE);
        
        ZIP_CACHE_REAPER_SLOW_PEND_MIN = getProperty(methodName,
            ZIP_CACHE_REAPER_SLOW_PEND_MIN_PROPERTY_NAME,
            ZIP_CACHE_REAPER_SLOW_PEND_MIN_DEFAULT_VALUE);

        ZIP_CACHE_REAPER_SLOW_PEND_MAX = getProperty(methodName,
            ZIP_CACHE_REAPER_SLOW_PEND_MAX_PROPERTY_NAME,
            ZIP_CACHE_REAPER_SLOW_PEND_MAX_DEFAULT_VALUE);
    }

    //

    private static final String PAD = "0000000000";
    private static final int PAD_LEN = 10;

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
        if ( missing > PAD_LEN ) {
            missing = PAD_LEN;
        }

        return PAD.substring(0,  missing) + digits;
    }

    //

    public static final long NANO_IN_ONE = 1000 * 1000 * 1000;
    public static final long NANO_IN_MILLI = 1000 * 1000;
    public static final long MILLI_IN_ONE = 1000;

    public static final int NANO_IN_ONE_DIGITS = 9;
    public static final int NANO_IN_MILLI_DIGITS = 6;
    public static final int MILLI_IN_ONE_DIGITS = 3;

    public static final int PAD_LEFT = 6;
    public static final int PAD_RIGHT = 6;

    //

    /**
     * Display the difference between two nano-seconds values as a
     * decimal seconds value.
     *
     * Display six places after the decimal point, and left
     * the integer value with '0' to a minimum of six characters.
     *
     * @param baseNS The value to subtract from the second parameter.
     * @param actualNS The from which to subtract the QUICK parameter.
     *
     * @return The difference as a seconds value.
     */
    @Trivial
    public static String toRelSec(long baseNS, long actualNS) {
        return toAbsSec(actualNS - baseNS, PAD_LEFT);
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
        return toAbsSec(durationNS, PAD_LEFT);
    }

// TODO: Move this into a unit test.
//    public static void main(String[] args) {
//        long[] testValues = {
//            0L, 1L,
//            998L, 999L, 1000L, 1001L,
//            999998L, 999999L, 1000000L, 1000001L,
//            999999998L, 999999999L, 1000000000L, 1000000001,
//            10000000000L
//        };
//
//        for ( long testValue : testValues ) {
//            System.out.println("Test [ " + Long.toString(testValue) + " ]");
//            System.out.println("     [ " + toAbsSec(testValue) + " ]");
//        }
//    }

    /**
     * Display a nano-seconds value as a decimal seconds value.
     *
     * Display six places after the decimal point, and left
     * the integer value with '0' to a minimum of the specified
     * number of pad characters.
     *
     * Negative values are handled by prepending '-' to the left of the
     * display value of the positive nano-seconds value.  This will result
     * in one extra character in the display output.
     *
     * @param nano The duration in nano-seconds to display
     *    as a seconds value.
     * @param padLeft The number of '0' character to pad to the integer
     *    part of the value.
     * @return The value as a seconds value.
     */
    @Trivial
    public static String toAbsSec(long nano, int padLeft) {
        if ( nano < 0 ) {
            return "-" + toAbsSec(-1 * nano, padLeft);
        } else if ( nano == 0 ) {
            return PAD.substring(0, padLeft) + "." + PAD.substring(PAD_RIGHT);
        }

        String nanoText = Long.toString(nano);
        int nanoDigits = nanoText.length();

        if ( nanoDigits > NANO_IN_ONE_DIGITS ) { // greater than 999,999,999
            int secDigits = nanoDigits - NANO_IN_ONE_DIGITS;
            if ( secDigits >= padLeft ) {
                return nanoText.substring(0, secDigits) +
                       "." +
                       nanoText.substring(secDigits, secDigits + PAD_RIGHT);
            } else {
                int padDigits = padLeft - secDigits;
                if ( padDigits > PAD_LEN ) {
                    padDigits = PAD_LEN;
                }
                return PAD.substring(0, padDigits) + nanoText.substring(0, secDigits) +
                       "." +
                       nanoText.substring(secDigits, secDigits + PAD_RIGHT);
            }

        } else if ( nanoDigits > (NANO_IN_ONE_DIGITS - PAD_RIGHT) ) { // less than 1,000,000,000 and greater than 999
            if ( padLeft > PAD_LEN ) {
                padLeft = PAD_LEN;
            }
            int missingDigits = NANO_IN_ONE_DIGITS - nanoDigits;
            return PAD.substring(0, padLeft) +
                   "." +
                   PAD.substring(0, missingDigits) + nanoText.substring(0, PAD_RIGHT - missingDigits);

        } else { // less than 1,000, but greater than 0.
            if ( padLeft > PAD_LEN ) {
                padLeft = PAD_LEN;
            }
            return PAD.substring(0, padLeft) +
                   "." +
                   PAD.substring(PAD_RIGHT - 1) + "*";
        }
    }

    //

    // Not the best place for this property ... but the zip file container uses the zip caching
    // service and uses zip file handles.

    public static final String ARTIFACT_ZIP_USE_EXTRA_PATH_CACHE_PROPERTY_NAME =
        "artifact.zip.use.extra.path.cache";
    public static final boolean ARTIFACT_ZIP_USE_EXTRA_PATH_CACHE_DEFAULT_VALUE = true;
    public static final boolean ARTIFACT_ZIP_USE_EXTRA_PATH_CACHE;

    public static final String ARTIFACT_ZIP_COLLECT_TIMINGS_PROPERTY_NAME =
        "artifact.zip.collect.timings";
    public static final boolean ARTIFACT_ZIP_COLLECT_TIMINGS_DEFAULT_VALUE = false;
    public static final boolean ARTIFACT_ZIP_COLLECT_TIMINGS;

    static {
        String methodName = "<static init>";

        ARTIFACT_ZIP_USE_EXTRA_PATH_CACHE = getProperty(methodName,
            ARTIFACT_ZIP_USE_EXTRA_PATH_CACHE_PROPERTY_NAME,
            ARTIFACT_ZIP_USE_EXTRA_PATH_CACHE_DEFAULT_VALUE);

        ARTIFACT_ZIP_COLLECT_TIMINGS = getProperty(methodName,
            ARTIFACT_ZIP_COLLECT_TIMINGS_PROPERTY_NAME,
            ARTIFACT_ZIP_COLLECT_TIMINGS_DEFAULT_VALUE);
    }

    public static long initialTiming;

    static {
        initialTiming = (( ZIP_REAPER_COLLECT_TIMINGS || ARTIFACT_ZIP_COLLECT_TIMINGS) ? System.nanoTime() : -1L );
    }

    @Trivial
    public static String toRelMilliSec(long baseNS, long actualNS, int pad) {
        return toAbsSec((actualNS - baseNS) * 1000, pad);
    }

    @Trivial
    public static String toAbsMilliSec(long eventAt, int pad) {
        return ZipCachingProperties.toAbsSec(eventAt * 1000, pad);
    }

    @Trivial
    public static String toRelMilliSec(long baseNS, long actualNS) {
        return toAbsSec((actualNS - baseNS) * 1000);
    }

    @Trivial
    public static String toAbsMilliSec(long eventAt) {
        return ZipCachingProperties.toAbsSec(eventAt * 1000);
    }

    @Trivial
    public static String dualTiming(long eventAt, long initialAt) {
        return " [ " + toAbsMilliSec(eventAt) + "ms ]" +
               " [ " + toAbsMilliSec(eventAt - initialAt) + "ms ]";
    }

    @Trivial
    public static String dualTiming(long eventAt) {
        return " [ " + toAbsMilliSec(eventAt) + "ms ]" +
               " [ " + toAbsMilliSec(eventAt - initialTiming) + "ms ]";
    }
}
