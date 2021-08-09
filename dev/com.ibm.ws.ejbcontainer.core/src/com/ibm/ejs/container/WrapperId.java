/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import com.ibm.ejs.container.util.EJSPlatformHelper;
import com.ibm.ejs.container.util.ByteArray;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Provides a unique wrapper identifier when multiple remote interfaces
 * may be present. <p>
 **/
public final class WrapperId
                extends com.ibm.ejs.util.ByteArray
{
    private static final TraceComponent tc = Tr.register(WrapperId.class,
                                                         "EJBContainer",
                                                         "com.ibm.ejs.container.container");

    /**
     * Static initialization of the header
     */
    private static final byte[] header = new byte[]
    {
     (byte) 0xAD, (byte) 0xAC,// MAGIC
     0x00, 0x02, // MAJOR_VERSION
     0x00, 0x01, // MINOR_VERSION
    };

    private static final int HEADER_LEN = header.length;

    /**
     * Business interface index; either remote or local. <p>
     * Will be a special value for aggregate local wrappers (all interfaces).
     */
    public int ivInterfaceIndex = -1;
    public String ivInterfaceClassName = null;

    public int ivBeanIdIndex = -1;

    /**
     * Create new WrapperId instance, based on the specified array of bytes. <p>
     * 
     * The specified byte array must contain a valid serialized WrapperId,
     * including a 'header', remote interface index, remote interface name,
     * and serialized BeanId. <p>
     */
    public WrapperId(byte[] bytes)
    {
        super(bytes);

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "<init> byte array length: " + bytes.length);

        // Match up the header.
        for (int i = 0; i < HEADER_LEN; ++i)
        {
            if (bytes[i] != header[i])
            {
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "Header mismatch, invalid WrapperId.");
                throw new RuntimeException("Header mismatch, invalid WrapperId.");
            }
        }

        int next = HEADER_LEN;

        if (EJSPlatformHelper.isZOS())
        {
            ivInterfaceIndex = ((bytes[next++] & 0xff) << 24) |
                               ((bytes[next++] & 0xff) << 16) |
                               ((bytes[next++] & 0xff) << 8) |
                               (bytes[next++] & 0xff);
        }
        else
        {
            ivInterfaceIndex = (bytes[next++] & 0xff) |
                               ((bytes[next++] & 0xff) << 8) |
                               ((bytes[next++] & 0xff) << 16) |
                               ((bytes[next++] & 0xff) << 24);
        }

        int interfaceNameLength = 0;

        if (EJSPlatformHelper.isZOS())
        {
            interfaceNameLength = ((bytes[next++] & 0xff) << 24) |
                                  ((bytes[next++] & 0xff) << 16) |
                                  ((bytes[next++] & 0xff) << 8) |
                                  (bytes[next++] & 0xff);
        }
        else
        {
            interfaceNameLength = (bytes[next++] & 0xff) |
                                  ((bytes[next++] & 0xff) << 8) |
                                  ((bytes[next++] & 0xff) << 16) |
                                  ((bytes[next++] & 0xff) << 24);
        }

        ivInterfaceClassName = new String(bytes, next, interfaceNameLength);

        ivBeanIdIndex = next + interfaceNameLength;

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "<init> : " + ivInterfaceIndex + " : " + ivInterfaceClassName);
    }

    /**
     * Create new WrapperId instance, containing the specified values that
     * uniquely identity a wrapper: BeanId, Business Interface, and
     * the corresponding index into the array of all Business
     * interfaces. <p>
     * 
     * The index is used to improve performance, but since the interface
     * name is included, the wrapper may still function as expected even
     * if other interfaces are added or removed from the EJB. <p>
     * 
     * @param beanId serialized BeanId corresponding to the wrapper.
     * @param interfaceName name of the specific business interface
     *            corresponding to the wrapper.
     * @param interfaceIndex index into the array of all remote business
     *            interfaces for the specified remote interface.
     **/
    public WrapperId(byte[] beanId, String interfaceName, int interfaceIndex)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
        {
            Tr.entry(tc, "<init>: " + interfaceIndex + ", " + interfaceName + ", " + beanId.length);
        }

        int beanIdLength = beanId.length;
        byte[] interfaceBytes = interfaceName.getBytes();
        int interfaceLength = interfaceBytes.length;
        int totalLength = HEADER_LEN + 8 + interfaceLength + beanIdLength;
        byte[] bytes = new byte[totalLength];

        int next = 0;
        for (int i = 0; i < HEADER_LEN; ++i)
        {
            bytes[next++] = header[i];
        }

        if (EJSPlatformHelper.isZOS())
        {
            bytes[next++] = (byte) (interfaceIndex >> 24);
            bytes[next++] = (byte) (interfaceIndex >> 16);
            bytes[next++] = (byte) (interfaceIndex >> 8);
            bytes[next++] = (byte) (interfaceIndex);
        }
        else
        {
            bytes[next++] = (byte) (interfaceIndex);
            bytes[next++] = (byte) (interfaceIndex >> 8);
            bytes[next++] = (byte) (interfaceIndex >> 16);
            bytes[next++] = (byte) (interfaceIndex >> 24);
        }

        if (EJSPlatformHelper.isZOS())
        {
            bytes[next++] = (byte) (interfaceLength >> 24);
            bytes[next++] = (byte) (interfaceLength >> 16);
            bytes[next++] = (byte) (interfaceLength >> 8);
            bytes[next++] = (byte) (interfaceLength);
        }
        else
        {
            bytes[next++] = (byte) (interfaceLength);
            bytes[next++] = (byte) (interfaceLength >> 8);
            bytes[next++] = (byte) (interfaceLength >> 16);
            bytes[next++] = (byte) (interfaceLength >> 24);
        }

        System.arraycopy(interfaceBytes, 0, bytes, next, interfaceLength);
        next += interfaceLength;

        ivInterfaceIndex = interfaceIndex; // d458325
        ivInterfaceClassName = interfaceName; // d458325
        ivBeanIdIndex = next; // d458325

        System.arraycopy(beanId, 0, bytes, next, beanIdLength);

        updateBytes(bytes);

        if (isTraceOn && tc.isEntryEnabled())
        {
            Tr.exit(tc, "<init> : " + ivInterfaceIndex + " : " + ivBeanIdIndex + " : " + ivInterfaceClassName);
        }
    }

    /**
     * Default constructor
     */
    protected WrapperId()
    {
        super();
    }

    /**
     * Copy constructor
     */
    protected WrapperId(WrapperId wId)
    {
        super(wId);
        ivInterfaceIndex = wId.ivInterfaceIndex;
        ivInterfaceClassName = wId.ivInterfaceClassName;
        ivBeanIdIndex = wId.ivBeanIdIndex;
    }

    /**
     * Returns a ByteArray containing the serialized BeanId that corresponds
     * to this WrapperId instance. <p>
     **/
    public ByteArray getBeanIdArray()
    {
        int beanIdLength = data.length - ivBeanIdIndex;
        byte[] beanIdBytes = new byte[beanIdLength];
        System.arraycopy(data, ivBeanIdIndex, beanIdBytes, 0, beanIdLength);
        return new ByteArray(beanIdBytes);
    }
}
