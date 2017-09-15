/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container.activator;

import java.util.GregorianCalendar;

import com.ibm.ejs.container.BeanId;
import com.ibm.ejs.container.BeanMetaData;

/**
 * A <code>TimeoutKey</code wraps a (BeanId, timeout) pair with appropriate
 * hashCode() and equals() methods so the pair can be used as a key into the
 * EJB (BeanO) cache. <p>
 * 
 * This key is used for Read Only 'Interval' Entity EJBs that will timeout
 * either after a certain amount of time, or at a set daily time. The key
 * always represents the time at which the bean will expire. <p>
 * 
 * Setting the timeout to 0 indicates the bean never expires. <p>
 * 
 * Note: Setting the timeout type the special value CURRENT_TIME will
 * allow equals() to match on any timeout time that has not expired
 * or has expired, but is not in use. <p>
 */

public final class TimeoutKey
{
    /**
     * Constant for ivTimeoutType indicating that ivTimeout has been set to
     * the current time and equals() should match on any timeout value that
     * has not expired (i.e. greater than current time), or that has
     * expired, but is not in use.
     **/
    private static final int CURRENT_TIME = 0;

    /**
     * Constant for ivTimeoutType indicating that ivTimeout has been set to
     * the expiration time and equals() should match on only that specific
     * timeout value or if greater than the current time, or if not in use.
     **/
    private static final int TIMEOUT_TIME = 1;

    /**
     * Enumeration type to indicate whether this key represents an actual
     * object timeout, or the current time. <p>
     * 
     * <ul>
     * <li>CURRENT_TIME - indicates that the key is being used to search/find
     * objects that have not yet expired/timed out.
     * <li>TIMEOUT_TIME - indicates that the key represents a specific
     * object timeout.
     * </ul>
     **/
    private int ivTimeoutType;

    /**
     * BeanId this cache key is associated with.
     */
    private final BeanId ivBeanId;

    /**
     * Time in milliseconds when the associated bean will expire and
     * must be re-loaded, or the current time when being used to find
     * a bean that has not expired. <p>
     * 
     * See ivTimeoutType to determine exact meaning of this value.
     */
    private long ivTimeout;

    /**
     * True if the bean has been discarded, and my no longer be used. <p>
     * 
     * This typically ocurrs when an unchecked exception is thrown from the
     * bean instance represented by the key instance. <p>
     **/
    // d210290
    private boolean ivDiscarded;

    /**
     * True if it has been detected that the key has timed out or
     * the bean it represents has expired. <p>
     * 
     * This field is set to True if an equals method has been invoked
     * using a CURRENT_TIME key, and the equals method has determined
     * that the key has timed out. <p>
     * 
     * This field may also be set to true, if the bean instance that it
     * represents has been destroyed... and needed to be prematurely
     * timed out / expired. <p>
     * 
     * False does not necessarily mean that the key has not timed out,
     * just that time out has not been detected yet. <p>
     **/
    private boolean ivExpired;

    /**
     * True if the bean instance is actively being used by a thread/
     * transaction and is pinned in the EJB Cache. <p>
     * 
     * Note that if a bean instance has expired, it may not be reloaded
     * if it is still in use by any transaction. <p>
     **/
    // d210290
    private boolean ivInUse;

    /**
     * Hash value. Cached for performance.
     */
    private final int ivHashValue;

    /**
     * Create new <code>TimeoutKey</code> instance associated with
     * given bean id and the current time. <p>
     * 
     * This constructor should be used when performing a find/search of
     * the cache and any instance that hasn't timed out is acceptable. <p>
     * 
     * @param beanid bean identifier this key is associated with.
     * @param timeout time in milliseconds when the associated bean will expire.
     */
    TimeoutKey(BeanId beanId)
    {
        ivBeanId = beanId;
        ivTimeoutType = CURRENT_TIME;
        ivTimeout = System.currentTimeMillis();
        ivDiscarded = false; // d210290
        ivExpired = false;
        ivInUse = false; // d210290

        // When a TimeoutKey is created, it will generally be used for
        // at least two cache lookups, so cache the hash code value.
        ivHashValue = beanId.hashCode();
    } // TimeoutKey

    /**
     * Create new <code>TimeoutKey</code> instance associated with
     * given bean id and timeout / expiration. <p>
     * 
     * This constructor should be used when inserting into the cache, or
     * performing a find/search of the cache for a bean that times out
     * at a specific time. <p>
     * 
     * @param beanid bean identifier this key is associated with.
     * @param timeout time in milliseconds when the associated bean will expire.
     */
    TimeoutKey(BeanId beanId, long timeout)
    {
        ivBeanId = beanId;
        ivTimeoutType = TIMEOUT_TIME;
        ivTimeout = timeout;
        ivDiscarded = false; // d210290
        ivExpired = false;
        ivInUse = true; // d210290

        // When a TimeoutKey is created, it will generally be used for
        // at least two cache lookups, so cache the hash code value.
        ivHashValue = beanId.hashCode();
    } // TimeoutKey

    /**
     * Changes the timeout for this key from the 'current time' to a
     * specific time out value by applying the specified interval to the
     * current time or re-calculates a specific time out value for a
     * key that has expired/timed out and will be reused (re-loaded). <p>
     * 
     * This method may be used to convert a key that contains the current
     * time, to a key with a specific time out setting for the
     * corresponding bean. <p>
     * 
     * This method is provided for performance, to allow one instance to
     * be created with the 'current time' to perform a cache search, and
     * if no object is found, to then convert the key to a 'time out' key
     * to actually use to insert the bean into the cache. <p>
     * 
     * Also for performance, this method allows a key and bean instance
     * that has timed out, but is not in use, to be reset and reloaded. <p>
     * 
     * Caution: This method is NOT synchronized, and should only be used
     * when the caller has exclusive access to the instance. <p>
     * 
     * @param type enumeration type defining the meaning of the specified
     *            interval value.
     * @param interval time in milliseconds to be added to the current time
     *            to determine when the associated bean will expire.
     * 
     * @exception IllegalStateException if the key does not currently represent
     *                the current time, or has not expired and is not in use.
     **/
    public void setTimeoutByInterval(int type, long interval)
    {
        if (ivTimeoutType != CURRENT_TIME)
        {
            // If the keys has expired, and is not in use, then allow the
            // time out time to be reset, and the bean re-loaded.           d210290
            if (ivExpired == true &&
                ivDiscarded == false &&
                ivInUse == false)
            {
                ivExpired = false;
                ivTimeout = System.currentTimeMillis();
            }
            else
                throw new IllegalStateException("timeout already set");
        }

        long now = ivTimeout; // Save the 'current' time

        ivTimeoutType = TIMEOUT_TIME;
        ivInUse = true; // d210290

        switch (type)
        {
        // The specified 'interval' is the basic interval specification
        // indicating to 'reload the bean every so many minutes'...
        // which means that the bean must be loaded the first time it
        // is accessed in any given interval, where the start of the
        // first interval is midnight, January 1, 1970 UTC, and each
        // interval duration is the specified interval value.
        // By determining the bean timeout in this way, rather than just
        // adding the interval to the current time, consistent data access
        // may be obtained across cluster members.
        // Also, to avoid all beans with the same interval value ending up
        // with the same timeout, vary it by adding in an offset that is
        // reproducable across servers... simply use the hash code to
        // obtain an index over the interval in milliseconds.            d217712
            case BeanMetaData.CACHE_RELOAD_INTERVAL:
                if (interval > 0)
                {
                    long currOffset = now % interval;
                    long hashOffset = (ivBeanId.hashCode() & 0x7FFFFFFF) % interval;
                    ivTimeout = now + (hashOffset - currOffset);
                    if (ivTimeout <= now) // d232681
                        ivTimeout += interval;
                }
                else
                {
                    ivTimeout = Long.MAX_VALUE;
                }
                break;

            // The specified 'interval' is for reloading the bean at a specific
            // time every day... and the 'interval' value is the number of
            // milliseconds from midnight to that time, so just add it to
            // the time in milliseconds representing midnight.
            case BeanMetaData.CACHE_RELOAD_DAILY:
                GregorianCalendar today = new GregorianCalendar();
                today.set(GregorianCalendar.HOUR_OF_DAY, 0);
                today.set(GregorianCalendar.MINUTE, 0);
                today.set(GregorianCalendar.SECOND, 0);
                today.set(GregorianCalendar.MILLISECOND, 0);
                long midnight = today.getTime().getTime();
                ivTimeout = midnight + interval;

                // If this calculated timeout has already passed, add 24 hours to it.
                // For example, if DAILY indicates "3am" and "now" is "3:15am", the
                // entry will live until 3am tomorrow.
                if (ivTimeout <= now) // d232681
                {
                    ivTimeout += 24 * 60 * 60 * 1000; // 24 hours in milliseconds
                }
                break;

            // The specified 'interval' is for reloading the bean at a specific
            // time and day of every week... and the 'interval' value is the
            // number of milliseconds from midnight on Sunday to that time,
            // so just add it to the time in milliseconds representing midnight
            // on Sunday (i.e. early Sunday morning).
            case BeanMetaData.CACHE_RELOAD_WEEKLY:
                GregorianCalendar sunday = new GregorianCalendar();
                sunday.set(GregorianCalendar.DAY_OF_WEEK, GregorianCalendar.SUNDAY);
                sunday.set(GregorianCalendar.HOUR_OF_DAY, 0);
                sunday.set(GregorianCalendar.MINUTE, 0);
                sunday.set(GregorianCalendar.SECOND, 0);
                sunday.set(GregorianCalendar.MILLISECOND, 0);
                long midnightSunday = sunday.getTime().getTime();
                ivTimeout = midnightSunday + interval;

                // If this calculated timeout has already passed, add 7 days to it.
                // For example, if WEEKLY indicates "Monday 3am" and "now" is
                // "Tuesday 2:30am", the entry will live until next Monday 3am.
                if (ivTimeout <= now) // d232681
                {
                    ivTimeout += 7 * 24 * 60 * 60 * 1000; // 7 days in milliseconds
                }
                break;

            default:
                throw new IllegalArgumentException("Interval LoadPolicy not specified");
        }
    }

    /**
     * Marks the key to indicate that the bean it represents has been
     * destroyed, and the key should be considered timed out. <p>
     * 
     * Invoking this method will prevent EJB Cache finds from locating
     * the bean instance this key represents using a CURRENT_TIME key.
     * Those transactions which have this bean instance enlisted will
     * continue to find this bean in the EJB Cache. <p>
     **/
    public void setDiscarded()
    {
        ivDiscarded = true; // d210290
        ivExpired = true;
    }

    /**
     * Marks the key to indicate that the associated bean instance is either
     * in use by a transaction/thread (true) or no longer in use (false). <p>
     * 
     * Invoking this method with 'true' will prevent the associated bean
     * instance from being reloaded. <p>
     * 
     * @param inUse true indicates the bean is in use, false indicates the
     *            bean is no longer in use and may be reloaded or evicted.
     **/
    // d210290
    public void setInUse(boolean inUse)
    {
        ivInUse = inUse;
    }

    /**
     * Returns true if an equals method invocation has determined that
     * this key has timed out, or the bean instance that this key
     * represents has been discarded. <p>
     * 
     * For performance reasons, this method does not actively compare
     * this key's timeout against the current time. A return value of
     * False does not mean that the key has not yet timed out, just that
     * the time out has not been detected. <p>
     **/
    public boolean expirationDetected()
    {
        return ivExpired;
    }

    /**
     * Return hash value appropriate for this (BeanId, Timeout) pair. <p>
     * 
     * Note that the timeout value cannot really be part of the hash
     * value, in order to allow cache searches to find instances that
     * have not timed out using the special value, CURRENT_TIME. <p>
     */
    public final int hashCode()
    {
        return ivHashValue;
    } // hashCode

    /**
     * Returns true if the supplied object wraps the same (BeanId, Timeout)
     * pair, or if one key represents the 'current time' and the other timeout
     * value is greater than the current time (i.e. it has not expired yet),
     * or has expired but is not in use. <p>
     * 
     * This override of the default Object.equals is required, even though
     * there are type specific overloads, in case the caller does not have
     * (or know) the parameter as the specific type. <p>
     */
    public final boolean equals(Object o)
    {
        // Shortcut in case they are exactly the same object... This is likely
        // to be true most of the time when both keys are timeout keys (not
        // current time keys) because the key is cached on the BeanO.
        if (this == o)
            return true;

        // If it is the correct type, then insure the BeanIds match, and if
        // so, handle the timeout values, incorporating the special value,
        // CURRENT_TIME.
        if (o instanceof TimeoutKey)
        {
            TimeoutKey key = (TimeoutKey) o;

            if (ivBeanId.equals(key.ivBeanId))
            {
                // If both keys contain timeout values (not current time) then
                // the timeout values just need to be equal.  Being expired or
                // timed out is not considered. Normal equals functionality.
                if (ivTimeoutType == TIMEOUT_TIME &&
                    key.ivTimeoutType == TIMEOUT_TIME)
                {
                    if (ivTimeout == key.ivTimeout)
                        return true;
                }

                // If the key parameter contains a current time, and 'this' object
                // contains a timeout value, then a search is being done using
                // 'key' for an entry that has not timed out.  The 'this' object
                // is considered a match if it has not timed out or been discarded
                // (i.e. the 'this' time value is greater than the current time).
                else if (key.ivTimeoutType == CURRENT_TIME &&
                         ivTimeoutType == TIMEOUT_TIME)
                {
                    if (!ivExpired)
                    {
                        if (ivTimeout > key.ivTimeout)
                            return true;

                        ivExpired = true;

                        // Also match if expired, but not in use or discarded,
                        // which allows the bean to just be reloaded.  Note that this
                        // is only allowed when expiration is detected (i.e. first
                        // set to true), otherwise multiple instances of the same
                        // bean instance could be activated.                   d210290
                        if (!ivInUse && !ivDiscarded)
                            return true;
                    }
                }

                // If 'this' object contains a current time, and the key parameter
                // contains a timeout value, then a search is being done using
                // 'this' for an entry that has not timed out.  The key parameter
                // is considered a match if it has not timed out or been discarded
                // (i.e. the key time value is greater than the current time).
                else if (ivTimeoutType == CURRENT_TIME &&
                         key.ivTimeoutType == TIMEOUT_TIME)
                {
                    if (!key.ivExpired)
                    {
                        if (key.ivTimeout > ivTimeout)
                            return true;

                        key.ivExpired = true;

                        // Also match if expired, but not in use or discarded,
                        // which allows the bean to just be reloaded.  Note that this
                        // is only allowed when expiration is detected (i.e. first
                        // set to true), otherwise multiple instances of the same
                        // bean instance could be activated.                   d210290
                        if (!key.ivInUse && !key.ivDiscarded)
                            return true;
                    }
                }

                // If 'none of the above' (i.e. both current time), then
                // this is really unexpected and the keys are considered
                // NOT being equal.
            }
        }

        return false;
    } // equals

    /**
     * Returns true if the supplied object wraps the same (BeanId, Timeout)
     * pair, or if one key represents the 'current time' and the other timeout
     * value is greater than the current time (i.e. it has not expired yet),
     * or has expired but is not in use. <p>
     * 
     * This type specific version is provided for performance, and avoids
     * any instanceof or casting. <p>
     */
    public final boolean equals(TimeoutKey key)
    {
        // Shortcut in case they are exactly the same object...  This is likely
        // to be true most of the time when both keys are timeout keys (not
        // current time keys) because the key is cached on the BeanO.
        if (this == key)
            return true;

        // Handle null, to avoid NullPointerException below.               d205816
        if (key == null)
            return false;

        // Insure the BeanIds match, and if so, handle the timeout values,
        // incorporating the special value, CURRENT_TIME.
        if (ivBeanId.equals(key.ivBeanId))
        {
            // If both keys contain timeout values (not current time) then
            // the timeout values just need to be equal.  Being expired or
            // timed out is not considered. Normal equals functionality.
            if (ivTimeoutType == TIMEOUT_TIME &&
                key.ivTimeoutType == TIMEOUT_TIME)
            {
                if (ivTimeout == key.ivTimeout)
                    return true;
            }

            // If the key parameter contains a current time, and 'this' object
            // contains a timeout value, then a search is being done using
            // 'key' for an entry that has not timed out.  The 'this' object
            // is considered a match if it has not timed out or been discarded
            // (i.e. the 'this' time value is greater than the current time).
            else if (key.ivTimeoutType == CURRENT_TIME &&
                     ivTimeoutType == TIMEOUT_TIME)
            {
                if (!ivExpired)
                {
                    if (ivTimeout > key.ivTimeout)
                        return true;

                    ivExpired = true;

                    // Also match if expired, but not in use or discarded,
                    // which allows the bean to just be reloaded.  Note that this
                    // is only allowed when expiration is detected (i.e. first
                    // set to true), otherwise multiple instances of the same
                    // bean instance could be activated.                      d210290
                    if (!ivInUse && !ivDiscarded)
                        return true;
                }
            }

            // If 'this' object contains a current time, and the key parameter
            // contains a timeout value, then a search is being done using
            // 'this' for an entry that has not timed out.  The key parameter
            // is considered a match if it has not timed out or been discarded
            // (i.e. the key time value is greater than the current time).
            else if (ivTimeoutType == CURRENT_TIME &&
                     key.ivTimeoutType == TIMEOUT_TIME)
            {
                if (!key.ivExpired)
                {
                    if (key.ivTimeout > ivTimeout)
                        return true;

                    key.ivExpired = true;

                    // Also match if expired, but not in use or discarded,
                    // which allows the bean to just be reloaded.  Note that this
                    // is only allowed when expiration is detected (i.e. first
                    // set to true), otherwise multiple instances of the same
                    // bean instance could be activated.                      d210290
                    if (!key.ivInUse && !key.ivDiscarded)
                        return true;
                }
            }

            // If 'none of the above' (i.e. both current time), then
            // this is really unexpected and the keys are considered
            // NOT being equal.
        }

        return false;
    } // equals

    public String toString()
    {
        StringBuffer result = new StringBuffer("TimeoutKey(");
        result.append(ivBeanId);

        if (ivTimeoutType == TIMEOUT_TIME) // d205816
            result.append(", Timeout: ");
        else
            result.append(", Current: ");
        result.append(ivTimeout);

        if (ivDiscarded) // d210290
            result.append(", discarded");

        if (ivExpired) // d205816
            result.append(", expired");

        if (ivInUse) // d210290
            result.append(", inUse");

        result.append(")");

        return result.toString();
    } // toString

} // TimeoutKey
