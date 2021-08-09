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
package com.ibm.ws.jca.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.security.AccessController;

import javax.resource.ResourceException;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.kernel.service.util.SecureAction;
import com.ibm.wsspi.kernel.service.location.VariableRegistry;
import com.ibm.wsspi.kernel.service.utils.OnErrorUtil;
import com.ibm.wsspi.kernel.service.utils.OnErrorUtil.OnError;

/**
 * Utility class.
 */
public class Utils {
    private static final TraceComponent tc = Tr.register(Utils.class);
    final static SecureAction priv = AccessController.doPrivileged(SecureAction.get());

    /**
     * Converts a Number value to a Integer, Long, Short, Byte, Double, or Float.
     * If unable to convert, the original value is returned.
     *
     * @param value a numeric value.
     * @param type the desired type, which should be one of (Integer, Long, Short, Byte, Double, Float).
     * @return converted value.
     */
    public static Number convert(Number value, Class<?> type) {
        if (int.class.equals(type) || Integer.class.equals(type))
            value = value.intValue();
        else if (long.class.equals(type) || Long.class.equals(type))
            value = value.longValue();
        else if (short.class.equals(type) || Short.class.equals(type))
            value = value.shortValue();
        else if (byte.class.equals(type) || Byte.class.equals(type))
            value = value.byteValue();
        else if (double.class.equals(type) || Double.class.equals(type))
            value = value.doubleValue();
        else if (float.class.equals(type) || Float.class.equals(type))
            value = value.floatValue();
        return value;
    }

    /**
     * Converts a String value to the specified type.
     *
     * @param str a String value.
     * @param type the desired type, which can be a primitive or primitive wrapper.
     * @return converted value.
     */
    public static Object convert(String str, Class<?> type) throws Exception {
        Object value;
        if (int.class.equals(type) || Integer.class.equals(type))
            value = Integer.parseInt(str);
        else if (boolean.class.equals(type) || Boolean.class.equals(type))
            value = Boolean.parseBoolean(str);
        else if (long.class.equals(type) || Long.class.equals(type))
            value = Long.parseLong(str);
        else if (short.class.equals(type) || Short.class.equals(type))
            value = Short.parseShort(str);
        else if (byte.class.equals(type) || Byte.class.equals(type))
            value = Byte.parseByte(str);
        else if (double.class.equals(type) || Double.class.equals(type))
            value = Double.parseDouble(str);
        else if (float.class.equals(type) || Float.class.equals(type))
            value = Float.parseFloat(str);
        else if (char.class.equals(type) || Character.class.equals(type))
            value = str.charAt(0);
        else
            value = type.getConstructor(String.class).newInstance(str);
        return value;
    }

    /**
     * Gets an NLS message.
     *
     * @param key the message key
     * @param params the message parameters
     * @return formatted message
     */
    @Trivial
    public static final String getMessage(String key, Object... params) {
        return TraceNLS.getFormattedMessage(Utils.class, tc.getResourceBundleName(), key, params, key);
    }

    /**
     * Utility method that gets the onError setting.
     * This method should be invoked every time it is needed in order to allow for
     * changes to the onError setting.
     *
     * @return the onError setting if configured. Otherwise the default value.
     */
    @Trivial
    private static final OnError ignoreWarnOrFail() {
        String value = null;
        BundleContext bundleContext = priv.getBundleContext(FrameworkUtil.getBundle(VariableRegistry.class));
        ServiceReference<VariableRegistry> ref = priv.getServiceReference(bundleContext, VariableRegistry.class);
        VariableRegistry variableRegistry = priv.getService(bundleContext, ref);
        try {
            String key = "${" + OnErrorUtil.CFG_KEY_ON_ERROR + "}";
            value = variableRegistry.resolveString(key);
            if (!key.equals(value))
                return OnError.valueOf(value.trim().toUpperCase());
        } catch (Exception x) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "onError: " + value, x);
        } finally {
            bundleContext.ungetService(ref);
        }
        return OnErrorUtil.getDefaultOnError();
    }

    /**
     * Ignore, warn, or fail when a configuration error occurs.
     * This is copied from Tim's code in tWAS and updated slightly to
     * override with the Liberty ignore/warn/fail setting.
     *
     * @param tc the TraceComponent from where the message originates
     * @param throwable an already created Throwable object, which can be used if the desired action is fail.
     * @param exceptionClassToRaise the class of the Throwable object to return
     * @param msgKey the NLS message key
     * @param objs list of objects to substitute in the NLS message
     * @return either null or the Throwable object
     */
    public static <T extends Throwable> T ignoreWarnOrFail(TraceComponent tc, Throwable throwable, Class<T> exceptionClassToRaise, String msgKey, Object... objs) {

        switch (ignoreWarnOrFail()) {
            case IGNORE:
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "ignoring error: " + msgKey, objs);
                return null;
            case WARN:
                Tr.warning(tc, msgKey, objs);
                return null;
            case FAIL:
                try {
                    if (throwable != null && exceptionClassToRaise.isInstance(throwable))
                        return exceptionClassToRaise.cast(throwable);

                    String message;
                    if (msgKey == null)
                        message = throwable.getMessage();
                    else
                        message = TraceNLS.getFormattedMessage(Utils.class, tc.getResourceBundleName(), msgKey, objs, null);

                    Constructor<T> con = exceptionClassToRaise.getConstructor(String.class);
                    return exceptionClassToRaise.cast(con.newInstance(message).initCause(throwable));
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
        }

        return null;
    }

    /**
     * Returns an exception message, stack, and cause formatted as a String.
     *
     * @param x exception or error.
     * @return an exception message, stack, and cause formatted as a String.
     */
    @Trivial
    public static final String toString(Throwable x) {
        StringWriter sw = new StringWriter();
        x.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    /**
     * Deserialize from an array of bytes.
     *
     * @param bytes serialized bytes.
     * @return deserialized object.
     */
    public static final Object deserialize(byte[] bytes) throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(tc, "deserialize");

        Object o;
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            ObjectInputStream oin = new ObjectInputStream(bis);
            o = oin.readObject();
            oin.close();
        } catch (IOException e) {
            FFDCFilter.processException(e, Utils.class.getName(), "305");
            if (trace && tc.isEntryEnabled())
                Tr.exit(tc, "deserialize", new Object[] { toString(bytes), e });
            throw e;
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(tc, "deserialize", o == null ? null : o.getClass());
        return o;
    }

    /**
     * Serialize an object to a byte array.
     *
     * @param pk the object
     * @throws IOException if an error occurs during the serialization process.
     */
    public static byte[] serObjByte(Object pk) throws IOException {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(tc, "serObjByte", pk == null ? null : pk.getClass());
        byte[] b;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(pk);
            out.flush();
            out.close();
            b = bos.toByteArray();
        } catch (IOException e) {
            FFDCFilter.processException(e, Utils.class.getName(), "336");
            if (trace && tc.isEntryEnabled())
                Tr.exit(tc, "serObjByte", new Object[] { "Unable to serialize: " + pk, e });
            throw e;
        } catch (Error e) {
            FFDCFilter.processException(e, Utils.class.getName(), "342");
            if (trace && tc.isEntryEnabled())
                Tr.exit(tc, "serObjByte", new Object[] { "Unable to serialize: " + pk, e });
            throw e;
        }
        if (trace && tc.isEntryEnabled())
            Tr.exit(tc, "serObjByte");
        return b;
    }

    /**
     * Format bytes as hexadecimal text. For example, 49 42 4D
     *
     * @param bytes array of bytes.
     * @return hexadecimal text.
     */
    private static final String toString(byte[] bytes) {
        if (bytes == null)
            return null;

        StringBuilder sb = new StringBuilder(bytes.length * 3);
        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i] < 0 ? 0x100 + bytes[i] : bytes[i];
            sb.append(Integer.toHexString(b / 0x10)).append(Integer.toHexString(b % 0x10)).append(' ');
        }

        return new String(sb);
    }

    /**
     * Check accessibility of the resource adapter from the application.
     *
     * @param resourceName The name of the resource
     * @param adapterName The name of the resource adapter
     * @param embeddedApp The name of the app in which the resource adapter is embedded
     * @param acessingApp The name of the app from which the resource is accessed
     * @param isEndpoint Whether the resource is an endpoint.
     * @throws ResourceException
     */
    public static void checkAccessibility(String resourceName, String adapterName, String embeddedApp, String accessingApp,
                                          boolean isEndpoint) throws ResourceException {
        if (embeddedApp != null) {
            if (!embeddedApp.equals(accessingApp)) {
                String msg = null;
                if (isEndpoint) {
                    if (accessingApp != null) {
                        msg = TraceNLS.getFormattedMessage(Utils.class, tc.getResourceBundleName(), "J2CA8810.embedded.activation.failed",
                                                           new Object[] { resourceName, adapterName, embeddedApp, accessingApp },
                                                           "J2CA8810.embedded.activation.failed");
                    } else {
                        msg = TraceNLS.getFormattedMessage(Utils.class, tc.getResourceBundleName(), "J2CA8812.embedded.activation.failed",
                                                           new Object[] { resourceName, adapterName, embeddedApp },
                                                           "J2CA8812.embedded.activation.failed");
                    }
                } else {
                    if (accessingApp != null) {
                        msg = TraceNLS.getFormattedMessage(Utils.class, tc.getResourceBundleName(), "J2CA8809.embedded.lookup.failed",
                                                           new Object[] { resourceName, adapterName, embeddedApp, accessingApp },
                                                           "J2CA8809.embedded.lookup.failed");
                    } else {
                        msg = TraceNLS.getFormattedMessage(Utils.class, tc.getResourceBundleName(), "J2CA8811.embedded.lookup.failed",
                                                           new Object[] { resourceName, adapterName, embeddedApp },
                                                           "J2CA8811.embedded.lookup.failed");
                    }
                }
                throw new ResourceException(msg);
            }
        }
    }
}
