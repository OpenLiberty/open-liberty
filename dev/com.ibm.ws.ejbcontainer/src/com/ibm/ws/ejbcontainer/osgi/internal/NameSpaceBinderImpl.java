/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi.internal;

import java.util.ArrayList;
import java.util.List;

import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.container.HomeRecord;
import com.ibm.ejs.csi.EJBApplicationMetaData;
import com.ibm.ejs.csi.EJBModuleMetaDataImpl;
import com.ibm.websphere.csi.HomeWrapperSet;
import com.ibm.ws.ejbcontainer.osgi.EJBRemoteRuntime;
import com.ibm.ws.ejbcontainer.osgi.internal.naming.EJBBinding;
import com.ibm.ws.ejbcontainer.osgi.internal.naming.EJBJavaColonNamingHelper;
import com.ibm.ws.ejbcontainer.runtime.NameSpaceBinder;

/**
 * Adds EJB names to the name space for java:global, java:app, and
 * java:module.
 */
public class NameSpaceBinderImpl implements NameSpaceBinder<EJBBinding> {
    protected final EJBModuleMetaDataImpl moduleMetaData;
    private final EJBJavaColonNamingHelper ejbJavaColonHelper;
    private final EJBRemoteRuntime remoteRuntime;

    NameSpaceBinderImpl(EJBModuleMetaDataImpl mmd,
                        EJBJavaColonNamingHelper jcnh,
                        EJBRemoteRuntime remoteRuntime) {
        moduleMetaData = mmd;
        ejbJavaColonHelper = jcnh;
        this.remoteRuntime = remoteRuntime;
    }

    @Override
    public void beginBind() {
        // Nothing needed
    }

    /**
     * Store the binding information for later use.
     *
     * @see com.ibm.ws.ejbcontainer.runtime.NameSpaceBinder#createBindingObject(com.ibm.ejs.container.HomeRecord, com.ibm.websphere.csi.HomeWrapperSet, java.lang.String, boolean,
     *      boolean)
     */
    @Override
    public EJBBinding createBindingObject(HomeRecord hr,
                                          HomeWrapperSet homeSet,
                                          String interfaceName,
                                          int interfaceIndex,
                                          boolean local) {
        return new EJBBinding(hr, interfaceName, interfaceIndex, local);
    }

    /**
     * This method is provided for tWas and is not used for Liberty.
     * Just return the bindingObject for now.
     */
    @Override
    public EJBBinding createJavaBindingObject(HomeRecord hr,
                                              HomeWrapperSet homeSet,
                                              String interfaceName,
                                              int interfaceIndex,
                                              boolean local,
                                              EJBBinding bindingObject) {
        return bindingObject;
    }

    /**
     * Adds the EJB reference for later lookup in the java:global
     * name space.
     *
     * @param name The lookup name.
     * @param bindingObject The binding information.
     */
    @Override
    public void bindJavaGlobal(String name, EJBBinding bindingObject) {
        String bindingName = buildJavaGlobalName(name);
        ejbJavaColonHelper.addGlobalBinding(bindingName, bindingObject);
    }

    /**
     * Adds the EJB application binding to the java:app name space.
     *
     * @param name The lookup name.
     * @param bindingObject The EJB binding information.
     */
    @Override
    public void bindJavaApp(String name, EJBBinding bindingObject) {
        String bindingName = buildJavaAppName(name);
        ejbJavaColonHelper.addAppBinding(moduleMetaData, bindingName, bindingObject);
    }

    /**
     * Adds the module binding to the java:module name space.
     *
     * @param name The lookup name.
     * @param bindingObject The EJB binding information.
     */
    @Override
    public void bindJavaModule(String name, EJBBinding bindingObject) {
        ejbJavaColonHelper.addModuleBinding(moduleMetaData, name, bindingObject);
    }

    @Override
    public void bindBindings(EJBBinding bindingObject,
                             HomeRecord hr,
                             int numInterfaces,
                             boolean singleGlobalInterface,
                             int interfaceIndex,
                             String interfaceName,
                             boolean local,
                             boolean deferred) {
        if (!local && remoteRuntime != null) {
            HomeRecordImpl hrImpl = HomeRecordImpl.cast(hr);
            if (hrImpl.remoteBindingData == null) {
                BeanMetaData bmd = hr.getBeanMetaData();
                EJBModuleMetaDataImpl mmd = bmd._moduleMetaData;
                EJBApplicationMetaData amd = mmd.getEJBApplicationMetaData();
                String appLogicalName = amd.isStandaloneModule() ? null : amd.getLogicalName();
                hrImpl.remoteBindingData = remoteRuntime.createBindingData(bmd, appLogicalName, mmd.ivLogicalName);
            }

            remoteRuntime.bind(hrImpl.remoteBindingData, interfaceIndex, interfaceName);
        }
    }

    @Override
    public void bindEJBFactory() {
        // Nothing. Remote EJBFactory is not supported.
    }

    @Override
    public void beginUnbind(boolean error) {
        // Nothing.
    }

    /**
     * Unbind the list of java:global names.
     *
     * @param names List of name space names to remove.
     */
    @Override
    public void unbindJavaGlobal(List<String> names) {

        List<String> fullNames = new ArrayList<String>(names.size());
        for (String name : names) {
            fullNames.add(buildJavaGlobalName(name));
        }
        ejbJavaColonHelper.removeGlobalBindings(fullNames);
    }

    /**
     * Unbind the names from the java:app name space.
     *
     * @param names List of names to remove from the
     *            application name space.
     */
    @Override
    public void unbindJavaApp(List<String> names) {

        List<String> fullNames = new ArrayList<String>(names.size());
        for (String name : names) {
            fullNames.add(buildJavaAppName(name));
        }
        ejbJavaColonHelper.removeAppBindings(moduleMetaData, fullNames);
    }

    @Override
    public void unbindBindings(HomeRecord hr) {
        if (remoteRuntime != null) {
            HomeRecordImpl hrImpl = HomeRecordImpl.cast(hr);
            if (hrImpl.remoteBindingData != null) {
                remoteRuntime.unbindAll(hrImpl.remoteBindingData);
            }
        }
    }

    @Override
    public void unbindEJBFactory() {
        // Nothing. Remote EJBFactory is not supported.
    }

    @Override
    public void end() {
        // Nothing
    }

    /**
     * Creates the binding name used in the java:global lookup. The format is
     * <app>/<module>/<ejbname>[!<fully qualified interface name] for modules in an application and
     * <module>/<ejbname>[!<fully qualified interface name] for stand alone modules.
     *
     * @param name The EJB name
     * @return the key for the lookup.
     */
    private String buildJavaGlobalName(String name) {
        StringBuffer bindingName = new StringBuffer();
        if (!moduleMetaData.getEJBApplicationMetaData().isStandaloneModule()) {
            bindingName.append(moduleMetaData.getEJBApplicationMetaData().getLogicalName());
            bindingName.append("/");
        }
        bindingName.append(moduleMetaData.ivLogicalName);
        bindingName.append("/");
        bindingName.append(name);
        return bindingName.toString();
    }

    /**
     * Creates the binding name used in the java:app lookup. The format is
     * <module>/<ejbname>[!<fully qualified interface name].
     *
     * @param name The EJB name
     * @return the key for the lookup.
     */
    private String buildJavaAppName(String name) {
        return moduleMetaData.ivLogicalName + "/" + name;
    }
}
