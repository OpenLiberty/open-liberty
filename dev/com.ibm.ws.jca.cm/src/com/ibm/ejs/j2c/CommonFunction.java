/*******************************************************************************
 * Copyright (c) 2010, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.j2c;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.resource.ResourceException;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * Utility methods that are common to all locations (client, server, embeddable EJB container).
 */
public class CommonFunction {
    private static final TraceComponent tc = Tr.register(CommonFunction.class, J2CConstants.traceSpec, J2CConstants.messageFile);

    private static final TraceNLS NLS = TraceNLS.getTraceNLS(CommonFunction.class, J2CConstants.messageFile);

    public static final String nl = AccessController.doPrivileged(new PrivilegedAction<String>() {
        @Override
        public String run() {
            return System.getProperty("line.separator");
        }
    });

    /**
     * Utility method to hide passwords from a map structure.
     * Make a copy of the map and
     * <li> replace password values with ******
     * <li> replace map values by recursively invoking this method
     *
     * @param map collection of name/value pairs. Values might be passwords or submaps of
     *            name/value pairs that might contain more submaps or passwords.
     * @param depth depth of the map. A map without any submaps is depth 1.
     *
     * @return copy of the map with passwords hidden.
     */
    @SuppressWarnings("unchecked")
    public static final Map<?, ?> hidePasswords(Map<?, ?> map, int depth) 
    {
        if (map != null && depth > 0) {
            map = new HashMap<Object, Object>(map);

            for (@SuppressWarnings("rawtypes")
            Map.Entry entry : map.entrySet())
                if (entry.getKey() instanceof String && ((String) entry.getKey()).toUpperCase().contains("PASSWORD"))
                    entry.setValue("******");
                else if (entry.getValue() instanceof Map)
                    entry.setValue(hidePasswords((Map<?, ?>) entry.getValue(), depth - 1));
        }

        return map;
    }

    /**
     * Method which prints the stack trace of exception and any linked exceptions.
     *
     * @param ex the exception
     * @return the stack trace of the exception and any linked exceptions.
     */
    public static String exceptionList(Throwable ex) { 
        StringBuilder sb = new StringBuilder();

        sb.append("<=================================>");
        sb.append("Exception Message -> ");
        sb.append(ex.getMessage());
        sb.append(nl);
        if (ex instanceof ResourceException) {
            sb.append("  ResourceException Error Code -> ");
            sb.append(((ResourceException) ex).getErrorCode());
            sb.append(nl);
        }
        if (ex instanceof SQLException) {
            sb.append("  SQLException  Error Code -> ");
            sb.append(((SQLException) ex).getErrorCode());
            sb.append(nl);
            sb.append("  SQLException  SQLState -> ");
            sb.append(((SQLException) ex).getSQLState());
            sb.append(nl);
        }
        // Get stack trace of current exception
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        sb.append(sw.getBuffer());
        sb.append("<=================================>");

        // If exception supports linked exceptions, get linked exception
        Throwable ex2 = null;
        ex2 = ex.getCause();

        // If we got a linked exception, recursively call this method
        if (ex2 != null) {
            sb.append("Next Linked Exception:" + nl);
            sb.append(exceptionList(ex2));
        }

        return sb.toString();

    }
    
    /**
     * Retrieve a translated message from the J2C messages file.
     * If the message cannot be found, the key is returned.
     *
     * @param key a valid message key from the J2C messages file.
     * @param args a list of parameters to include in the translatable message.
     *
     * @return a translated message.
     */
    public static final String getNLSMessage(String key, Object... args) {
        return NLS.getFormattedMessage(key, args, key);
    }

    /**
     * Serialize an object to a byte array.
     *
     * @param pk the object
     * @throws IOException if an error occurs during the serialization process.
     */
    static byte[] serObjByte(Object pk) throws IOException {
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
            FFDCFilter.processException(e, CommonFunction.class.getName(), "203");
            if (trace && tc.isEntryEnabled())
                Tr.exit(tc, "serObjByte", new Object[] { "Unable to serialize: " + pk, e });
            throw e;
        } catch (Error e) {
            FFDCFilter.processException(e, CommonFunction.class.getName(), "210");
            if (trace && tc.isEntryEnabled())
                Tr.exit(tc, "serObjByte", new Object[] { "Unable to serialize: " + pk, e });
            throw e;
        }
        if (trace && tc.isEntryEnabled())
            Tr.exit(tc, "serObjByte");
        return b;
    }

    /**
     * Formats an exception's stack trace as a String.
     *
     * @param th a throwable object (Exception or Error)
     *
     * @return String containing the exception's stack trace.
     */
    static String stackTraceToString(Throwable th) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        for (int depth = 0; depth < 10 && th != null; depth++) {
            th.printStackTrace(pw);
            Throwable cause = th.getCause();
            if (cause != th) {
                th = cause;
                pw.append("-------- chained exception -------").append(nl);
            }
        }
        return sw.toString();
    }

    /**
     * Implements the standard toString method for objects on which toString has been
     * overridden.
     *
     * @param obj the Object to convert to a String.
     *
     * @return String equivalent to what would be returned by Object.toString(),
     *         or null, if the object is null.
     */
    public static String toString(Object obj) {
        if (obj == null)
            return null;

        return new StringBuffer(obj.getClass().getName()).append('@').append(Integer.toHexString(System.identityHashCode(obj))).toString();
    }
}