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
package com.ibm.ws.cdi.ejb.impl;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jboss.weld.ejb.spi.BusinessInterfaceDescriptor;
import org.jboss.weld.ejb.spi.EjbDescriptor;

import com.ibm.ejs.container.EJBConfigurationException;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.CDIException;
import com.ibm.ws.ejbcontainer.EJBEndpoint;
import com.ibm.ws.ejbcontainer.EJBReferenceFactory;
import com.ibm.ws.ejbcontainer.EJBType;

public class EjbDescriptorImpl<T> implements WebSphereEjbDescriptor<T>
{
    private static final TraceComponent tc = Tr.register(EjbDescriptorImpl.class);

    private final Class<T> beanClass;
    private final EJBReferenceFactory referenceFactory;
    private Collection<BusinessInterfaceDescriptor<?>> localBusinessInterfaces;
    private Collection<BusinessInterfaceDescriptor<?>> remoteBusinessInterfaces;
    private String ejbName;
    private boolean isStateless;
    private boolean isSingleton;
    private boolean isStateful;
    private boolean isMessageDriven;
    private boolean isPassivationCapable;
    private final J2EEName ejbJ2EEName;
    private final String ejbJ2EENameString;

    private Collection<Method> removeMethods;

    private EjbDescriptorImpl(Class<T> beanClass, EJBEndpoint ejb, ClassLoader classLoader) throws CDIException
    {
        this.beanClass = beanClass;
        this.referenceFactory = ejb.getEJBType().isSession() ? ejb.getReferenceFactory() : null;
        this.ejbJ2EEName = ejb.getJ2EEName();
        this.ejbJ2EENameString = ejbJ2EEName.toString();
        init(ejb, classLoader);
    }

    public static EjbDescriptor<?> newInstance(EJBEndpoint ejb, ClassLoader classLoader) throws CDIException
    {
        EjbDescriptor<?> descriptor = null;
        String beanClassName = ejb.getClassName();
        try {
            Class<?> beanClass = classLoader.loadClass(beanClassName);
            descriptor = newInstance(beanClass, ejb, classLoader);
        } catch (ClassNotFoundException e) {
            throw new CDIException(e);
        }
        return descriptor;
    }

    public static <K> EjbDescriptor<K> newInstance(Class<K> beanClass, EJBEndpoint ejb, ClassLoader classLoader)
                    throws CDIException
    {
        return new EjbDescriptorImpl<K>(beanClass, ejb, classLoader);
    }

    private void init(EJBEndpoint ejb, ClassLoader classLoader) throws CDIException
    {
        try {
            List<String> localInterfaceNames = ejb.getLocalBusinessInterfaceNames();
            this.localBusinessInterfaces = new ArrayList<BusinessInterfaceDescriptor<?>>(localInterfaceNames.size() + 1);
            for (String interfaceName : localInterfaceNames)
            {
                Class<?> interfaceClass = classLoader.loadClass(interfaceName);
                this.localBusinessInterfaces.add(BusinessInterfaceDescriptorImpl.newInstance(interfaceClass));
            }

            if (ejb.isLocalBean()) {
                this.localBusinessInterfaces.add(BusinessInterfaceDescriptorImpl.newInstance(this.beanClass));
            }

            List<String> remoteInterfaceNames = ejb.getRemoteBusinessInterfaceNames();
            this.remoteBusinessInterfaces = new ArrayList<BusinessInterfaceDescriptor<?>>();
            for (String remoteInterfaceName : remoteInterfaceNames)
            {
                Class<?> remoteInterfaceClass = classLoader.loadClass(remoteInterfaceName);
                this.remoteBusinessInterfaces.add(BusinessInterfaceDescriptorImpl.newInstance(remoteInterfaceClass));
            }
        } catch (ClassNotFoundException e) {
            throw new CDIException(e);
        }

        this.ejbName = ejb.getName();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "EJB Name: " + ejbName);
        }

        EJBType ejbType = ejb.getEJBType();
        this.isStateless = ejbType == EJBType.STATELESS_SESSION;
        this.isSingleton = ejbType == EJBType.SINGLETON_SESSION;
        this.isStateful = ejbType == EJBType.STATEFUL_SESSION;
        this.isMessageDriven = ejbType == EJBType.MESSAGE_DRIVEN;
        this.isPassivationCapable = ejb.isPassivationCapable();

        this.removeMethods = getRemoveMethodsForEndPoint(ejb);
    }

    private Collection<Method> getRemoveMethodsForEndPoint(EJBEndpoint ejbEndpoint) throws CDIException
    {
        Collection<Method> removeMethods = null;
        try {
            removeMethods = ejbEndpoint.getStatefulRemoveMethods();
        } catch (EJBConfigurationException e) {
            throw new CDIException(e);
        }
        return removeMethods;
    }

    @Override
    public Class<T> getBeanClass()
    {
        return beanClass;
    }

    @Override
    public Collection<BusinessInterfaceDescriptor<?>> getLocalBusinessInterfaces()
    {
        return localBusinessInterfaces;
    }

    @Override
    public Collection<BusinessInterfaceDescriptor<?>> getRemoteBusinessInterfaces()
    {
        return remoteBusinessInterfaces;
    }

    @Override
    public String getEjbName()
    {
        return ejbJ2EEName.getComponent();
    }

    @Override
    public Collection<Method> getRemoveMethods()
    {
        return removeMethods;
    }

    @Override
    public boolean isStateless()
    {
        return isStateless;
    }

    @Override
    public boolean isSingleton()
    {
        return isSingleton;
    }

    @Override
    public boolean isStateful()
    {
        return isStateful;
    }

    @Override
    public boolean isMessageDriven()
    {
        return isMessageDriven;
    }

    @Override
    public boolean isPassivationCapable()
    {
        return isPassivationCapable;
    }

    @Override
    public J2EEName getEjbJ2EEName()
    {
        return ejbJ2EEName;
    }

    @Override
    public EJBReferenceFactory getReferenceFactory()
    {
        return referenceFactory;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ejbJ2EENameString.hashCode();
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        EjbDescriptorImpl<?> other = (EjbDescriptorImpl<?>) obj;
        String otherEjbJ2EENameString = other.getEjbJ2EEName().toString();
        if (!ejbJ2EENameString.equals(otherEjbJ2EENameString))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "EjbDescriptor: " + this.ejbJ2EENameString;
    }

}
