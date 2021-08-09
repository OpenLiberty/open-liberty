/*******************************************************************************
 * Copyright (c) 2007, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container.util;

import java.io.IOException;

import javax.ejb.EJBHome;
import javax.ejb.EJBObject;

import com.ibm.ejs.container.BeanId;
import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.container.EJSContainer;
import com.ibm.ejs.container.EJSHome;
import com.ibm.ejs.container.EJSWrapperBase;
import com.ibm.ejs.container.EJSWrapperCommon;
import com.ibm.ejs.container.LocalBeanWrapper;
import com.ibm.ejs.container.LocalBeanWrapperProxy;
import com.ibm.ejs.container.WrapperId;
import com.ibm.ejs.container.WrapperInterface;
import com.ibm.ejs.container.WrapperManager;
import com.ibm.ejs.container.WrapperProxy;
import com.ibm.ejs.container.WrapperProxyState;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.util.EJBSerializer;

/**
 * Implementation of EJBSerializer abstract class.
 *
 * @see com.ibm.ws.ejbcontainer.util.EJBSerializer
 */
public class EJBSerializerImpl extends EJBSerializer
{
    private final static TraceComponent tc = Tr.register(EJBSerializerImpl.class,
                                                         "EJBContainer",
                                                         "com.ibm.ejs.container.container");

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.ejbcontainer.util.EJBSerializer#deserialize(byte[])
     */
    @Override
    public Object deserialize(byte[] idBytes) throws Exception
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            Tr.entry(tc, "deserialize");
        }

        EJSContainer container = EJSContainer.getDefaultContainer();
        Object retObj = null;
        WrapperManager wm = container.getWrapperManager();

        // The first bytes of serialized data is a header and
        // the first byte of header identifies it as either a
        // BeanId or a WrapperId. So examine first byte to
        // determine whether it is a BeanId or a WrapperId.
        if (idBytes[0] == (byte) 0xAC)
        {
            // First byte indicates it is a BeanId. Use the
            // WrapperManager to find the wrapper in wrapper cache.
            ByteArray byteArray = new ByteArray(idBytes);
            BeanId beanId = BeanId.getBeanId(byteArray, container);
            retObj = wm.getWrapper(beanId).getLocalObject();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            {
                Tr.debug(tc, "deserialized a BeanId = " + beanId
                             + ", component wrapper = " + retObj);
            }
        }
        else if (idBytes[0] == (byte) 0xAD) //d458325
        {
            // First byte indicates it is a WrapperId that was serialized.
            // Recreate the WrapperId and use it to create the wrapper
            // for the business interface in the WrapperId represents.
            WrapperId wrapperId = new WrapperId(idBytes);
            int index = wrapperId.ivInterfaceIndex;
            String interfaceName = wrapperId.ivInterfaceClassName;
            ByteArray byteArray = wrapperId.getBeanIdArray();
            BeanId beanId = BeanId.getBeanId(byteArray, container);
            BeanMetaData bmd = beanId.getBeanMetaData();
            Tr.debug(tc, "deserialized a WrapperId for BeanId = " + beanId
                         + ", primary key = " + beanId.getPrimaryKey());

            // Wrapper may be an Aggregate Local, Business Local or Business
            // Remote. Aggregate Local is easy to determine, otherwise assume
            // it is Business Local and finally resort to Business Remote last;
            // creating the wrapper based on type.                       F743-34304
            if (index == EJSWrapperBase.AGGREGATE_LOCAL_INDEX)
            {
                // Obtain the home and create the 'set' of wrappers for the
                // deserialized BeanId and then get the aggregate wrapper.
                EJSHome home = bmd.getHome();
                EJSWrapperCommon wc = home.internalCreateWrapper(beanId);
                retObj = wc.getAggregateLocalWrapper();
            }
            else
            {
                // Assume it is a BusinessLocalWrapper.
                // Verify index is still valid. If we fail over to a new server,
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

                // Yep, create a new instance of the BusinessLocalWrapper object.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                {
                    Tr.debug(tc, "deserialized a BusinessLocalWrapper, WrapperId = " + wrapperId);
                }
                EJSHome home = bmd.getHome();
                EJSWrapperCommon wc = home.internalCreateWrapper(beanId);

                retObj = wc.getLocalBusinessObject(index);
            }
        }
        else
        {
            throw new IOException("First byte of header not recognized");
        }

        // Return the wrapper
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            Tr.exit(tc, "deserialize returning: " + retObj);
        }
        return retObj;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.ejbcontainer.util.EJBSerializer#getObjectType(java.lang.Object)
     */
    @Override
    public ObjectType getObjectType(Object theObject)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            Tr.entry(tc, "getObjectType: " + theObject.getClass());
        }

        ObjectType objectType = ObjectType.NOT_AN_EJB;
        if (theObject instanceof javax.rmi.CORBA.Stub)
        {
            if (theObject instanceof EJBObject)
            {
                objectType = ObjectType.EJB_OBJECT;
            }
            else if (theObject instanceof EJBHome)
            {
                objectType = ObjectType.EJB_HOME;
            }
            else
            {
                objectType = ObjectType.CORBA_STUB;
            }
        }
        else if (theObject instanceof WrapperProxy) // F58064
        {
            if (theObject instanceof LocalBeanWrapperProxy)
            {
                // There is no sense in using heavyweight reflection to dig out the
                // WrapperProxyState since we can cheaply detect local bean.
                objectType = ObjectType.EJB_LOCAL_BEAN;
            }
            else
            {
                objectType = WrapperProxyState.getWrapperProxyState(theObject).getSerializerObjectType();
            }
        }
        else if (theObject instanceof EJSWrapperBase)
        {
            EJSWrapperBase wrapper = (EJSWrapperBase) theObject;
            WrapperInterface wInterface = wrapper.ivInterface;
            if (wInterface == WrapperInterface.BUSINESS_LOCAL)
            {
                objectType = ObjectType.EJB_BUSINESS_LOCAL;
            }
            else if (wInterface == WrapperInterface.LOCAL)
            {
                objectType = ObjectType.EJB_LOCAL_OBJECT;
            }
            else if (wInterface == WrapperInterface.LOCAL_HOME)
            {
                objectType = ObjectType.EJB_LOCAL_HOME;
            }
            else if (wInterface == WrapperInterface.BUSINESS_REMOTE)
            {
                objectType = ObjectType.EJB3_BUSINESS_REMOTE;
            }
            else if (wInterface == WrapperInterface.BUSINESS_RMI_REMOTE)
            {
                objectType = ObjectType.EJB3_BUSINESS_REMOTE;
            }
        }
        else if (theObject instanceof LocalBeanWrapper) // d609263
        {
            objectType = ObjectType.EJB_LOCAL_BEAN;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            Tr.exit(tc, "ObjectType = " + objectType);
        }

        return objectType;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.ejbcontainer.util.EJBSerializer#serialize(java.lang.Object)
     */
    @Override
    public byte[] serialize(Object theObject)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            Tr.entry(tc, "serialize = " + theObject.getClass());
        }

        byte[] idBytes = null;

        if (theObject instanceof WrapperProxy) // F58064
        {
            WrapperProxyState state = WrapperProxyState.getWrapperProxyState(theObject);
            idBytes = state.getSerializerBytes();

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "serialized a WrapperProxy for " + state);
        }
        else
        {
            EJSWrapperBase wrapper = null;
            if (theObject instanceof EJSWrapperBase)
            {
                wrapper = (EJSWrapperBase) theObject;
            }
            else if (theObject instanceof LocalBeanWrapper) // d609263
            {
                wrapper = EJSWrapperCommon.getLocalBeanWrapperBase((LocalBeanWrapper) theObject);
            }
            else
            {
                throw new IllegalArgumentException(" theObject parameter must be a EJB_LOCAL, EJB_LOCAL_HOME"
                                                   + ", EJB_BUSINESS_LOCAL, or EJB_BUSINESS_REMOTE ObjectType. Use the getObjectType"
                                                   + " to obtain the ObjectType.");
            }

            // Get the BeanId and WrapperInterface from the wrapper.
            BeanId beanId = wrapper.beanId;
            WrapperInterface wrapperInterface = wrapper.ivInterface;
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
                WrapperId wrapperId = new WrapperId(beanId.getByteArrayBytes() //d458325
                , interfaceName
                                , interfaceIndex);
                idBytes = wrapperId.getBytes();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                {
                    Tr.debug(tc, "serialized a WrapperId for BeanId = " + beanId
                                 + ", local business interface = " + interfaceName);
                }
            }
            else if (wrapperInterface == WrapperInterface.BUSINESS_REMOTE)
            {
                // Serialize a WrapperId for remote business interface.
                int interfaceIndex = wrapper.ivBusinessInterfaceIndex;
                Class<?> biClass = bmd.ivBusinessRemoteInterfaceClasses[interfaceIndex];
                String interfaceName = biClass.getName();
                WrapperId wrapperId = new WrapperId(beanId.getByteArrayBytes() //d458325
                , interfaceName
                                , interfaceIndex);
                idBytes = wrapperId.getBytes();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                {
                    Tr.debug(tc, "serialized a WrapperId for BeanId = " + beanId
                                 + ", remote business interface = " + interfaceName);
                }
            }
            else if (wrapperInterface == WrapperInterface.BUSINESS_RMI_REMOTE)
            {
                // Serialize a WrapperId for a RMI remote business interface.
                int interfaceIndex = wrapper.ivBusinessInterfaceIndex;
                Class<?> biClass = bmd.ivBusinessRemoteInterfaceClasses[interfaceIndex];
                String interfaceName = biClass.getName();
                WrapperId wrapperId = new WrapperId(beanId.getByteArrayBytes() //d458325
                , interfaceName
                                , interfaceIndex);
                idBytes = wrapperId.getBytes();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                {
                    Tr.debug(tc, "serialized a WrapperId for BeanId = " + beanId
                                 + ", RMI remote business interface = " + interfaceName);
                }
            }
            else
            {
                // Not a business interface, so must be a local or remote interface of
                // a 2.1 bean or a 2.1 view of a EJB 3 bean.  Either case, we only need
                // to save the BeanId for this kind of wrapper.
                idBytes = beanId.getByteArrayBytes(); //d466573
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                {
                    Tr.debug(tc, "serialized a BeanId = " + beanId);
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            Tr.exit(tc, "serialize");
        }

        return idBytes;
    }

}
