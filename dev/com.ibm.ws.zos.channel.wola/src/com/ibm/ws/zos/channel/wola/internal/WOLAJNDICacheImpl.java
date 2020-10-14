/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.wola.internal;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.MetaDataSlot;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.ws.zos.channel.wola.internal.natv.CodepageUtils;

/**
 * This object implements a very simple JNDI cache for WOLA. Its purpose is to
 * avoid the cost of doing a JNDI lookup on each WOLA inbound call. We'll cache
 * from JNDI name to EJB here, and whenever the application state listener gets
 * called for any reason, we'll invalidate the cache and start over.
 */
public class WOLAJNDICacheImpl implements ApplicationStateListener, WOLAJNDICache {

    /**
     * This is the cache - from JNDI name to EJB.
     */
    private final Map<ByteArray, WOLAInboundTarget> cache = Collections.synchronizedMap(new HashMap<ByteArray, WOLAInboundTarget>());

    /**
     * A dummy CMD to put on the thread while doing JNDI lookups in order
     * to sleaze our way past JNDI's CMD checks.
     */
    private ComponentMetaData dummyCMD = null;

    /**
     * Lookup an EJB from a JNDI name.
     */
    @Override
    public WOLAInboundTarget jndiLookup(byte[] jndiNameBytes) throws NamingException {
        // See if we're in the cache first.
        WOLAInboundTarget target = cache.get(new ByteArray(jndiNameBytes));

        if (target == null) {
            try {
                // Convert our EBCDIC string to the correct codepage.
                String jndiName = new String(jndiNameBytes, CodepageUtils.EBCDIC);

                // Put a dummy CMD on the thread to placate JNDI, which checks for such things..
                ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().beginContext(getDummyComponentMetaData());
                Object bean = new InitialContext().lookup(jndiName);
                if (bean != null) {
                    Method executeMethod = bean.getClass().getMethod("execute", new Class[] { byte[].class });
                    target = new WOLAInboundTarget(bean, executeMethod);
                    cache.put(new ByteArray(jndiNameBytes), target);
                }
            } catch (UnsupportedEncodingException uee) {
                throw new IllegalArgumentException(uee);
            } catch (NoSuchMethodException nsme) {
                throw new IllegalArgumentException(nsme);
            } finally {
                ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().endContext();
            }
        }

        return target;
    }

    /**
     * @return a dummy CMD to put on the thread while doing the JNDI lookup
     *         so we can sleaze our way past JNDI's CMD checks.
     */
    private ComponentMetaData getDummyComponentMetaData() {

        if (dummyCMD == null) {

            dummyCMD = new ComponentMetaData() {
                @Override
                public ModuleMetaData getModuleMetaData() {
                    return null;
                }

                @Override
                public J2EEName getJ2EEName() {
                    return null;
                }

                @Override
                public String getName() {
                    return "WOLAJNDICache.DummyComponentMetaData";
                }

                @Override
                public void setMetaData(MetaDataSlot slot, Object metadata) {
                }

                @Override
                public Object getMetaData(MetaDataSlot slot) {
                    return null;
                }

                @Override
                public void release() {
                }
            };
        }

        return dummyCMD;
    }

    /**
     * We clear the cache on any app manager event.
     */
    @Override
    public void applicationStarting(ApplicationInfo appInfo) throws StateChangeException {
        cache.clear();
    }

    /**
     * We clear the cache on any app manager event.
     */
    @Override
    public void applicationStarted(ApplicationInfo appInfo) throws StateChangeException {
        cache.clear();
    }

    /**
     * We clear the cache on any app manager event.
     */
    @Override
    public void applicationStopping(ApplicationInfo appInfo) {
        cache.clear();
    }

    /**
     * We clear the cache on any app manager event.
     */
    @Override
    public void applicationStopped(ApplicationInfo appInfo) {
        cache.clear();
    }

    /**
     * Internal byte array object implementation, optimized for EBCDIC JNDI names.
     */
    private static class ByteArray {
        private final byte[] bytes;
        private int hashCode;

        private ByteArray(byte[] bytes) {
            if (bytes == null) {
                throw new IllegalArgumentException("Input byte array cannot be null");
            }

            // Compute the hash code.  The data bytes will be an EBCDIC string that is a
            // JNDI name.  The most common name format will be:
            //
            // java:global/some/path/SomeBean!com.ibm.websphere.ola.ExecuteLocalBusiness
            //
            // It makes no sense to hash on the java:global/ part.  It makes no sense to hash
            // on the end of the string (past the '!' character).  We'll arbitrarily hash on
            // the 12th thru 15th characters, unless the byte array is shorter than that.
            this.bytes = bytes;
            if (this.bytes.length >= 16) {
                int index = 12;
                hashCode = ((this.bytes[index + 3] & 0xFF) << 0) +
                           ((this.bytes[index + 2] & 0xFF) << 8) +
                           ((this.bytes[index + 1] & 0xFF) << 16) +
                           ((this.bytes[index + 0]) << 24);
            } else {
                hashCode = 0;
            }
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object that) {
            boolean equal = false;
            if ((that != null) && (that instanceof ByteArray)) {
                ByteArray thatByteArray = (ByteArray) that;
                if (this.hashCode == thatByteArray.hashCode) {
                    equal = Arrays.equals(this.bytes, thatByteArray.bytes);
                }
            }
            return equal;
        }
    }
}
