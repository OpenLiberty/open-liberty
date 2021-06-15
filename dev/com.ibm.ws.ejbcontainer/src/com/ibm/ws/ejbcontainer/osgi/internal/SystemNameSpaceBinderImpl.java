/*******************************************************************************
 * Copyright (c) 2015, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi.internal;

import java.rmi.RemoteException;
import java.util.List;

import javax.ejb.CreateException;
import javax.ejb.EJB;
import javax.naming.NamingException;

import com.ibm.ejs.container.HomeRecord;
import com.ibm.websphere.csi.HomeWrapperSet;
import com.ibm.ws.ejbcontainer.osgi.EJBRemoteRuntime;
import com.ibm.ws.ejbcontainer.runtime.NameSpaceBinder;

public class SystemNameSpaceBinderImpl implements NameSpaceBinder<String> {
    private final EJBRemoteRuntime remoteRuntime;

    public SystemNameSpaceBinderImpl(EJBRemoteRuntime remoteRuntime) {
        this.remoteRuntime = remoteRuntime;
    }

    @Override
    public void beginBind() throws NamingException {
    }

    @Override
    public String createBindingObject(HomeRecord hr,
                                      HomeWrapperSet homeSet,
                                      String interfaceName,
                                      int interfaceIndex,
                                      boolean local) throws NamingException, RemoteException, CreateException {
        HomeRecordImpl hrImpl = HomeRecordImpl.cast(hr);
        return hrImpl.systemHomeBindingName;
    }

    @Override
    public String createJavaBindingObject(HomeRecord hr,
                                          HomeWrapperSet homeSet,
                                          String interfaceName,
                                          int interfaceIndex,
                                          boolean local,
                                          String bindingObject) {
        return null;
    }

    @Override
    public void bindJavaGlobal(String name, String bindingObject) throws NamingException {
    }

    @Override
    public void bindJavaApp(String name, String bindingObject) throws NamingException {
    }

    @Override
    public void bindJavaModule(String name, String bindingObject) throws NamingException {
    }

    @Override
    public void bindBindings(String homeBindingName,
                             HomeRecord hr,
                             int numInterfaces,
                             boolean singleGlobalInterface,
                             int interfaceIndex,
                             String interfaceName,
                             boolean local,
                             boolean deferred) throws NamingException {
        if (remoteRuntime == null) {
            String remoteFeatureName = EJB.class.getName().startsWith("jakarta") ? "enterpriseBeansRemote" : "ejbRemote";
            throw new NamingException("Unable to bind system module. Ensure the " + remoteFeatureName + " feature is installed.");
        }

        HomeRecordImpl hrImpl = HomeRecordImpl.cast(hr);
        if (hrImpl.remoteBindingData == null) {
            hrImpl.remoteBindingData = remoteRuntime.bindSystem(hr.getBeanMetaData(), homeBindingName);
        }
    }

    @Override
    public void bindEJBFactory() throws NamingException {
    }

    @Override
    public void beginUnbind(boolean error) throws NamingException {
    }

    @Override
    public void unbindJavaGlobal(List<String> names) throws NamingException {
    }

    @Override
    public void unbindJavaApp(List<String> names) throws NamingException {
    }

    @Override
    public void unbindBindings(HomeRecord hr) throws NamingException {
        if (remoteRuntime != null) {
            HomeRecordImpl hrImpl = HomeRecordImpl.cast(hr);
            if (hrImpl.remoteBindingData != null) {
                remoteRuntime.unbindAll(hrImpl.remoteBindingData);
                hrImpl.remoteBindingData = null;
            }
        }
    }

    @Override
    public void unbindEJBFactory() throws NamingException {
    }

    @Override
    public void end() throws NamingException {
    }

    @Override
    public void bindDefaultEJBLocal(String bindingObject, HomeRecord hr) {
    }

    @Override
    public void bindDefaultEJBRemote(String bindingObject, HomeRecord hr) {
    }

    @Override
    public void unbindEJBLocal(List<String> names) throws NamingException {
    }

    @Override
    public void bindSimpleBindingName(String bindingObject, HomeRecord hr, boolean local, boolean generateDisambiguatedSimpleBindingNames) {
    }

    @Override
    public void bindLocalHomeBindingName(String bindingObject, HomeRecord hr) {
    }

    @Override
    public void bindLocalBusinessInterface(String bindingObject, HomeRecord hr) {
    }

    @Override
    public void unbindLocalColonEJB(List<String> names) throws NamingException {
    }

    @Override
    public void unbindRemote(List<String> names) {
    }
}
