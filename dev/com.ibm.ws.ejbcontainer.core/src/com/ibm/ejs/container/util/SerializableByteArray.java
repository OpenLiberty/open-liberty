/*******************************************************************************
 * Copyright (c) 2002, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container.util;

import java.io.Serializable;

import com.ibm.ejs.container.BeanId;
import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.container.EJSContainer;
import com.ibm.ejs.container.EJSHome;
import com.ibm.ejs.container.EJSWrapperBase;
import com.ibm.ejs.container.EJSWrapperCommon;
import com.ibm.ejs.container.WrapperId;
import com.ibm.ejs.container.WrapperInterface;
import com.ibm.ejs.container.WrapperManager;
import com.ibm.ejs.container.WrapperProxyState;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Used as a replacement object during passivation of a SFSB
 * to serialize either a local home, local component, or local
 * business interface (EJSLocalWrapper or a BusinessLocalWrapper).
 */
public class SerializableByteArray implements Serializable
{
    private static final long serialVersionUID = 8311919298341109455L;

    private final static TraceComponent tc = Tr.register(SerializableByteArray.class,
                                                         "EJBContainer",
                                                         "com.ibm.ejs.container.container");

    /**
     * Ship to customer with DEBUG_ON set to false so that the
     * verbose debug information is not traced. Set to true
     * when debugging internally to get more verbose information.
     */
    private final static boolean DEBUG_ON = false; // d458325

    /**
     * Serialized bytes of the BeanId or a WrapperId object.
     */
    private byte[] ivIdBytes;

    /**
     * Create a SerializableByteArray object to hold the serialized
     * data for a specified EJSWrapperBase object. <p>
     *
     * @param wrapper is the wrapper being serialized.
     */
    public SerializableByteArray(EJSWrapperBase wrapper)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
        {
            Tr.entry(tc, "SerializableByteArray for wrapper = " +
                         wrapper.getClass().getName() + '@' +
                         Integer.toHexString(wrapper.hashCode()));
        }

        // Get serialized bytes of the BeanId and the WrapperInterface
        // for this wrapper (which is a enum that indicates wrapper type).
        BeanId beanId = wrapper.beanId;
        WrapperInterface wrapperInterface = wrapper.ivInterface;

        // Save Business interface class name if this wrapper is
        // for either a local or remote business interface.
        BeanMetaData bmd = wrapper.bmd;

        // Use type of wrapper interface to determine whether to
        // serialize a WrapperId or a BeanId object.
        if (wrapperInterface == WrapperInterface.BUSINESS_LOCAL)
        {
            // Serialize a WrapperId for local business interface.
            int interfaceIndex = wrapper.ivBusinessInterfaceIndex;
            String interfaceName;
            if (interfaceIndex == EJSWrapperBase.AGGREGATE_LOCAL_INDEX) { // F743-34304
                interfaceName = EJSWrapperBase.AGGREGATE_EYE_CATCHER; // d677413
            } else {
                Class<?> biClass = bmd.ivBusinessLocalInterfaceClasses[interfaceIndex];
                interfaceName = biClass.getName();
            }
            if (isTraceOn && tc.isDebugEnabled())
            {
                Tr.debug(tc, "serializing a WrapperId for BeanId = " + beanId
                             + ", local business interface = " + interfaceName
                             + ", primary key = " + beanId.getPrimaryKey());
            }

            WrapperId wrapperId = new WrapperId(beanId.getByteArrayBytes() // d458325
            , interfaceName
                            , interfaceIndex);

            ivIdBytes = wrapperId.getBytes();

            if (DEBUG_ON) // d458325
            {
                if (isTraceOn && tc.isDebugEnabled())
                {
                    Tr.debug(tc, "serialized WrapperId bytes[" + ivIdBytes.length + "] = "
                                 + wrapperId.toString());
                    Tr.debug(tc, "WrapperId.ivInterfaceIndex = " + wrapperId.ivInterfaceIndex
                                 + ", WrapperId.ivInterfaceClassName = " + wrapperId.ivInterfaceClassName
                                 + ", WrapperId.ivBeanIdIndex = " + wrapperId.ivBeanIdIndex);
                }
            }
        }
        else
        {
            // Not a local business interface, so must be a local or local home interface of
            // a 2.1 bean or a 2.1 view of a EJB 3 bean.  Either case, we only need
            // to save the BeanId for this kind of wrapper.
            if (isTraceOn && tc.isDebugEnabled())
            {
                Tr.debug(tc, "serializing a BeanId = " + beanId
                             + ", primary key = " + beanId.getPrimaryKey());
            }

            ivIdBytes = beanId.getByteArrayBytes(); //d466573

            if (DEBUG_ON) // d458325
            {
                if (isTraceOn && tc.isDebugEnabled())
                {
                    Tr.debug(tc, "serialized BeanId bytes = " + beanId.getByteArray().toString());
                }
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
        {
            Tr.exit(tc, "SerializableByteArray");
        }
    }

    public SerializableByteArray(WrapperProxyState state) // F58064
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "SerializableByteArray for wrapper proxy state = " + state);

        ivIdBytes = state.getSerializerBytes();

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "SerializableByteArray");
    }

    /**
     * Get the wrapper object by deserializing the bytes
     * stored in this object.
     *
     * @return deserialized wrapper object.
     *
     * @throws Exception if one occurs during deserialization.
     */
    public Object getWrapper() throws Exception
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
        {
            Tr.entry(tc, "getWrapper");
        }

        EJSContainer container = EJSContainer.getDefaultContainer();
        Object retObj = null;
        WrapperManager wm = container.getWrapperManager();

        // The first bytes of serialized data is a header and
        // the first byte of header identifies it as either a
        // BeanId or a WrapperId. So examine first byte to
        // determine whether it is a BeanId or a WrapperId.
        if (ivIdBytes[0] == (byte) 0xAC)
        {
            // First byte indicates it is a BeanId. Use the
            // WrapperManager to find the wrapper in wrapper cache.
            ByteArray byteArray = new ByteArray(ivIdBytes);

            if (DEBUG_ON) // d458325
            {
                if (isTraceOn && tc.isDebugEnabled())
                {
                    Tr.debug(tc, "serialized BeanId bytes = " + byteArray.toString());
                }
            }

            BeanId beanId = BeanId.getBeanId(byteArray, container);

            if (isTraceOn && tc.isDebugEnabled())
            {
                Tr.debug(tc, "deserialized a BeanId = " + beanId
                             + ", primary key = " + beanId.getPrimaryKey());
            }

            retObj = wm.getWrapper(beanId).getLocalObject();
        }
        else if (ivIdBytes[0] == (byte) 0xAD) // d458325
        {
            // First byte indicates it is a WrapperId that was serialized.
            // Recreate the WrapperId and use it to get BeanId, local business
            // interface name, and local business interface index.
            WrapperId wrapperId = new WrapperId(ivIdBytes);
            int index = wrapperId.ivInterfaceIndex;
            String interfaceName = wrapperId.ivInterfaceClassName;

            if (DEBUG_ON) // d458325
            {
                if (isTraceOn && tc.isDebugEnabled())
                {
                    Tr.debug(tc, "serialized WrapperId bytes[" + ivIdBytes.length + "] = "
                                 + wrapperId.toString());
                    Tr.debug(tc, "WrapperId.ivInterfaceIndex = " + index
                                 + ", WrapperId.ivInterfaceClassName = " + interfaceName
                                 + ", WrapperId.ivBeanIdIndex = " + wrapperId.ivBeanIdIndex);
                }
            }

            ByteArray byteArray = wrapperId.getBeanIdArray();
            BeanId beanId = BeanId.getBeanId(byteArray, container);
            BeanMetaData bmd = beanId.getBeanMetaData();

            Tr.debug(tc, "deserialized a WrapperId for BeanId = " + beanId
                         + ", primary key = " + beanId.getPrimaryKey());

            // Obtain the home and create the 'set' of wrappers for the
            // deserialized BeanId.
            EJSHome home = bmd.getHome();
            EJSWrapperCommon wc = home.internalCreateWrapper(beanId);

            // Finally, decide which proxy is needed from the set. This may
            // be for an aggregate local wrapper (all business local interfaces)
            // or it may be for a specific interface, in which case the index
            // must be verified                                          F743-34304
            if (index == EJSWrapperBase.AGGREGATE_LOCAL_INDEX)
            {
                retObj = wc.getAggregateLocalWrapper();
            }
            else
            {
                // Now verify index is still valid. If we fail over to a new server,
                // the index for the interface may be different than original server.
                // In that case, we need to determine the new index.
                Class<?>[] bInterfaces = bmd.ivBusinessLocalInterfaceClasses;
                int numberOfLocalInterfaces = bInterfaces.length;
                String wrapperInterfaceName = null;
                if (index < numberOfLocalInterfaces)
                {
                    wrapperInterfaceName = bInterfaces[index].getName();
                }

                // Is the BusinessLocalWrapper for the correct interface name?
                if ((wrapperInterfaceName == null) || (!wrapperInterfaceName.equals(interfaceName)))
                {
                    // Nope, index must be invalid, so we need to get an updated index
                    // that matches the desired interface name.
                    // Fix up the WrapperId with index that matches this server.
                    index = bmd.getRequiredLocalBusinessInterfaceIndex(interfaceName);
                }

                // Now that we have correct index, obtain the corresponding
                // proxy object.
                retObj = wc.getLocalBusinessObject(index);
            }
        }

        // Return the wrapper
        if (isTraceOn && tc.isEntryEnabled())
        {
            Tr.exit(tc, "getWrapper returning: " +
                        (retObj == null ? retObj
                                        : (retObj.getClass().getName() +
                                        '@' + Integer.toHexString(retObj.hashCode()))));
        }
        return retObj;
    }

}
