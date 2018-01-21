/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.cm;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;

import com.ibm.ejs.j2c.J2CConstants;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.logging.WsLevel;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.classloading.ClassLoaderIdentifierService;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereTransactionManager;
import com.ibm.wsspi.kernel.service.location.VariableRegistry;

/**
 * Interface for accessing core servies and utility methods that are common to connection management and jdbc
 * (which were originally in a single bundle and later split so that connection management could be used by jca
 * without causing jdbc to be loaded).
 */
public abstract class ConnectorService {
    private static final TraceComponent tc = Tr.register(ConnectorService.class, J2CConstants.traceSpec, J2CConstants.NLS_FILE);
    private static final TraceNLS NLS = TraceNLS.getTraceNLS(ConnectorService.class, J2CConstants.NLS_FILE);

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
            FFDCFilter.processException(e, ConnectorService.class.getName(), "151");
            if (trace && tc.isEntryEnabled())
                Tr.exit(tc, "deserialize", new Object[] { toString(bytes), e });
            throw e;
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(tc, "deserialize", o == null ? null : o.getClass());
        return o;
    }

    /**
     * @return the class loader identifier service.
     */
    public abstract ClassLoaderIdentifierService getClassLoaderIdentifierService();

    /**
     * Get a translated message from J2CAMessages file.
     *
     * @param key message key.
     * @param args message parameters
     * @return a translated message.
     */
    public static final String getMessage(String key, Object... args) {
        return NLS.getFormattedMessage(key, args, key);
    }

    /**
     * @return the common Liberty thread pool.
     */
    public abstract ExecutorService getLibertyThreadPool();

    /**
     * @return the transaction manager.
     */
    public abstract EmbeddableWebSphereTransactionManager getTransactionManager();

    /**
     * @return the variable registry.
     */
    public abstract VariableRegistry getVariableRegistry();

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
    public abstract <T extends Throwable> T ignoreWarnOrFail(TraceComponent tc, Throwable throwable, Class<T> exceptionClassToRaise, String msgKey, Object... objs);

    /**
     * Logs a message from the J2CAMessages file.
     *
     * @param key message key.
     * @param args message parameters
     */
    public static final void logMessage(Level level, String key, Object... args) {
        if (WsLevel.AUDIT.equals(level))
            Tr.audit(tc, key, args);
        else if (WsLevel.ERROR.equals(level))
            Tr.error(tc, key, args);
        else if (Level.INFO.equals(level))
            Tr.info(tc, key, args);
        else if (Level.WARNING.equals(level))
            Tr.warning(tc, key, args);
        else
            throw new UnsupportedOperationException(level.toString());
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
}
