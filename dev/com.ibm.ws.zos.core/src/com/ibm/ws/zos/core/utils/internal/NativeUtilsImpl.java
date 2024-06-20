/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.core.utils.internal;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.TimeZone;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.zos.core.utils.NativeUtils;
import com.ibm.ws.zos.jni.NativeMethodManager;

/**
 * Provides store clock (STCK) related services.
 */
public class NativeUtilsImpl implements BundleActivator, NativeUtils {

    /**
     * TraceComponent for this class.
     */
    private final static TraceComponent tc = Tr.register(NativeUtilsImpl.class);

    /**
     * Time difference between the MVS and Java clock origins.
     */
    private static final long JAVA_MVS_ORIGIN_DIFFERENCE = calculateClockOriginDifference();

    /**
     * NativeMethodManager reference.
     */
    protected NativeMethodManager nativeMethodManager = null;

    /**
     * This service registration with OSGI.
     */
    private ServiceRegistration<NativeUtils> serviceRegistration;

    /** {@inheritDoc} */
    @Override
    public void start(BundleContext context) throws Exception {
        this.nativeMethodManager.registerNatives(NativeUtilsImpl.class);

        // Register the service with OSGI
        Dictionary<String, String> properties = new Hashtable<String, String>();
        properties.put(Constants.SERVICE_VENDOR, "IBM");
        serviceRegistration = context.registerService(NativeUtils.class, this, properties);
    }

    /** {@inheritDoc} */
    @Override
    public void stop(BundleContext context) throws Exception {
        serviceRegistration.unregister();
        serviceRegistration = null;
    }

    /**
     * Default constructor to enable extension in test and OSGi instantiation.
     */
    public NativeUtilsImpl() {
    }

    /**
     * Default constructor to enable extension in test and OSGi instantiation.
     */
    public NativeUtilsImpl(NativeMethodManager nativeMethodManager) {
        this.nativeMethodManager = nativeMethodManager;
    }

    /**
     * Calculates the time stamp for the MVS TOD clock origin relative to the Java clock
     * origin of January 1, 1970 00:00:00 GMT
     *
     * @return The calculated timestamp.
     */
    @Trivial
    private static long calculateClockOriginDifference() {
        Calendar calendar = Calendar.getInstance();

        // Origin for MVS TOD clock is January 1, 1900, 00:00:00 GMT
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        calendar.clear();
        calendar.set(1900, 0, 1);

        // Calculate the time stamp for the MVS TOD clock origin relative
        // to the Java clock origin of January 1, 1970 00:00:00 GMT
        return calendar.getTime().getTime();
    }

    /**
     * Convert a <code>STCK</code> value to milliseconds from the Java clock origin.
     *
     * @return a long containing milliseconds from the Java clock origin.
     */
    @Trivial
    public static long getStckMillis(long stck) {
        // Bit 51 is microseconds so shift out the low order 12 bits
        // and then convert to milliseconds
        long micros = stck >>> 12;
        long stckMillis = micros / 1000L;

        // Add the timestamp to the origin difference and convert
        return stckMillis + JAVA_MVS_ORIGIN_DIFFERENCE;
    }

    /** {@inheritDoc} */
    @Override
    public long getSTCK() {
        return ntv_getStck();
    }

    /** {@inheritDoc} */
    @Override
    public ByteBuffer mapDirectByteBuffer(final long address, final int size) {
        if (tc.isDebugEnabled()) {
            // you could argue that this trace point isn't needed because it's covered by the entry trace,
            // but i suspect that anybody who's interested in this information is going to want the
            // address in hex, hence the redundancy
            Tr.debug(tc, "Mapping direct byte buffer", new Object[] { Long.toHexString(address), size });
        }

        return ntv_mapDirectByteBuffer(address, size).asReadOnlyBuffer();
    }

    /** {@inheritDoc} */
    @Override
    public byte[] convertAsciiStringToFixedLengthEBCDIC(final String s, final int requiredLength) {

        // Declare the return value
        byte[] retVal = null;
        String blank = " ";
        String str = s;

        // If you didn't give us a parameter (null or empty string) just set up a single blank
        if ((str == null) || (str.length() == 0)) {
            str = blank;
        }

        str = pad(str, requiredLength);

        // Convert to EBCDIC so MVS can understand
        retVal = convertToEbcdic(str);

        return retVal;
    }

    /**
     * Internal helper functions
     */

    /**
     * Pad with blanks or truncate a string to a specified length.
     *
     * @param s   The String
     * @param len the required length
     * @return A string of len length
     */
    private static String pad(String s, int len) {
        if (s.length() > len) {
            s = s.substring(0, len);
        } else {

            StringBuffer buf = new StringBuffer(s);
            while (buf.length() < len) {
                buf.append(" ");
            }
            s = buf.toString();
        }
        return s;
    }

    /**
     * Convert the input string to EBCDIC and handle exceptions
     *
     * @param str A string to convert
     * @return The string, in EBCDIC or a null if conversion failed
     */
    private static byte[] convertToEbcdic(String str) {
        byte[] retVal = null;
        try {
            retVal = str.getBytes("Cp1047");
        } catch (UnsupportedEncodingException uee) {
            // FFDC automatically inserted, null return value already set
        }
        return retVal;
    }

    /** {@inheritDoc} */
    @Override
    public long getTaskId() {
        return ntv_getTaskId();
    }

    /** {@inheritDoc} */
    @Override
    public int getPid() {
        return ntv_getPid();
    }

    /** {@inheritDoc} */
    @Override
    public byte[] getSmfData() {
        return ntv_getSmfData();
    }

    /** {@inheritDoc} */
    @Override
    public byte[] getTimeusedData() {
        return ntv_getTimeusedData();
    }

    /** {@inheritDoc} */
    @Override
    public int getUmask() {
        return ntv_getUmask();
    }

    /**
     * Native Services
     */

    protected native long ntv_getStck();

    protected native ByteBuffer ntv_mapDirectByteBuffer(long address, int size);

    protected native long ntv_getTaskId();

    protected native int ntv_getPid();

    protected native byte[] ntv_getSmfData();

    protected native byte[] ntv_getTimeusedData();

    protected native int ntv_getUmask();
}