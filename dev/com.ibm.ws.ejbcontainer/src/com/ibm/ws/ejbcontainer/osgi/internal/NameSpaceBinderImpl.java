/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
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
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import javax.naming.NamingException;
import javax.naming.Reference;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.container.BindingsHelper;
import com.ibm.ejs.container.ContainerProperties;
import com.ibm.ejs.container.HomeRecord;
import com.ibm.ejs.csi.EJBApplicationMetaData;
import com.ibm.ejs.csi.EJBModuleMetaDataImpl;
import com.ibm.websphere.csi.HomeWrapperSet;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.naming.EJBLocalNamingHelper;
import com.ibm.ws.container.service.naming.LocalColonEJBNamingHelper;
import com.ibm.ws.ejbcontainer.osgi.EJBRemoteRuntime;
import com.ibm.ws.ejbcontainer.osgi.internal.naming.EJBBinding;
import com.ibm.ws.ejbcontainer.osgi.internal.naming.EJBJavaColonNamingHelper;
import com.ibm.ws.ejbcontainer.osgi.internal.naming.EJBRemoteReferenceBinding;
import com.ibm.ws.ejbcontainer.runtime.NameSpaceBinder;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * Adds EJB names to the name space for java:global, java:app, and
 * java:module.
 */
public class NameSpaceBinderImpl implements NameSpaceBinder<EJBBinding> {
    private static TraceComponent tc = Tr.register(NameSpaceBinderImpl.class, "EJBContainer", "com.ibm.ejs.container.container");
    private static final String JNDI_SERVICENAME = "osgi.jndi.service.name";

    protected final EJBModuleMetaDataImpl moduleMetaData;
    private final EJBJavaColonNamingHelper ejbJavaColonHelper;
    private final EJBLocalNamingHelper<EJBBinding> ejbLocalNamingHelper;
    private final LocalColonEJBNamingHelper<EJBBinding> localColonNamingHelper;
    private final AtomicServiceReference<EJBRemoteRuntime> ejbRemoteRuntimeServiceRef;
    private final EJBRemoteRuntime remoteRuntime;

    private final List<ServiceRegistration<?>> registrations = new ArrayList<ServiceRegistration<?>>();

    NameSpaceBinderImpl(EJBModuleMetaDataImpl mmd,
                        EJBJavaColonNamingHelper jcnh,
                        EJBLocalNamingHelper<EJBBinding> ejblocal,
                        LocalColonEJBNamingHelper<EJBBinding> localColon,
                        AtomicServiceReference<EJBRemoteRuntime> remoteRuntimeRef) {
        moduleMetaData = mmd;
        ejbJavaColonHelper = jcnh;
        ejbLocalNamingHelper = ejblocal;
        localColonNamingHelper = localColon;
        this.ejbRemoteRuntimeServiceRef = remoteRuntimeRef;
        this.remoteRuntime = remoteRuntimeRef.getService();
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

    /**
     * Adds the default ejblocal: bindings to the ejblocal: name space
     *
     * @param bindingObject the EJB Binding information
     * @param hr the HomeRecord of the EJB
     */
    @Override
    public void bindDefaultEJBLocal(EJBBinding bindingObject, HomeRecord hr) {
        HomeRecordImpl hrImpl = HomeRecordImpl.cast(hr);
        if (hrImpl.bindToContextRoot()) {
            BeanMetaData bmd = hr.getBeanMetaData();
            J2EEName eeName = hrImpl.getJ2EEName();

            // if component-id binding was specified use that, otherwise use defaul long form
            String bindingName = null;
            if (bmd.ivComponent_Id != null) {
                bindingName = bmd.ivComponent_Id + "#" + bindingObject.interfaceName;
            } else {
                // <app>/<module.jar>/<bean>#<interface>
                bindingName = eeName.getApplication() + "/" + eeName.getModule() + "/" + eeName.getComponent() + "#" + bindingObject.interfaceName;
            }
            ejbLocalNamingHelper.bind(bindingObject, bindingName);

            BindingsHelper bh = BindingsHelper.getLocalHelper(hr);
            bh.ivEJBLocalBindings.add(bindingName);

            sendBindingMessage(bindingObject.interfaceName, "ejblocal:" + bindingName, bmd);

            // Default Short
            ejbLocalNamingHelper.bind(bindingObject, bindingObject.interfaceName);
            bh.ivEJBLocalBindings.add(bindingObject.interfaceName);
            sendBindingMessage(bindingObject.interfaceName, "ejblocal:" + bindingObject.interfaceName, bmd);
        }

    }

    /**
     * Adds the default remote legacy bindings to root
     *
     * @param bindingObject the EJB Binding information
     * @param hr the HomeRecord of the EJB
     */
    @Override
    public void bindDefaultEJBRemote(EJBBinding bindingObject, HomeRecord hr) {
        HomeRecordImpl hrImpl = HomeRecordImpl.cast(hr);

        if (hrImpl.bindToContextRoot()) {

            // Default Long
            J2EEName eeName = hrImpl.getJ2EEName();
            // ejb/<app>/<module.jar>/<bean>#<interface>
            String bindingName = "ejb/" + eeName.getApplication() + "/" + eeName.getModule() + "/" + eeName.getComponent() + "#" + bindingObject.interfaceName;
            bindLegacyRemoteBinding(bindingObject, hr, bindingName);

            // Default Short
            bindingName = bindingObject.interfaceName;
            bindLegacyRemoteBinding(bindingObject, hr, bindingName);

        }
    }

    /**
     * Binds a bindingObject with a bindingName to root for legacy remote bindings.
     * To bind to root we register a service to the BundleContext, passing it a Reference Object
     *
     * @param bindingObject the EJB Binding information
     * @param hr the HomeRecord of the EJB
     * @param bindingName the JNDI binding name
     */
    private void bindLegacyRemoteBinding(EJBBinding bindingObject, HomeRecord hr, String bindingName) {
        // TODO: If BindingsHelper.ivRemoteBindings.contains(bindingName); we have duplicate bindings
        // and need to bind Ambiguous. #11441

        BindingsHelper bh = BindingsHelper.getRemoteHelper(hr);
        bh.ivRemoteBindings.add(bindingName);

        BundleContext bc = this.ejbRemoteRuntimeServiceRef.getReference().getBundle().getBundleContext();
        BeanMetaData bmd = hr.getBeanMetaData();

        // Our Service registration object needs some properties saying its a JNDI naming service
        // with a Reference Object.
        Dictionary<String, Object> properties = new Hashtable<String, Object>(1);
        properties.put(JNDI_SERVICENAME, bindingName);
        properties.put(Constants.OBJECTCLASS, Reference.class.getName());

        // Create our wrapper Reference Object to bind
        EJBRemoteReferenceBinding ref = new EJBRemoteReferenceBinding(bindingObject);

        ServiceRegistration<?> registration = bc.registerService(Reference.class, ref, properties);

        registrations.add(registration);

        sendBindingMessage(bindingObject.interfaceName, bindingName, bmd);
    }

    /**
     * Binds the simpleBindingName custom binding
     *
     * Caller should ensure the simpleBindingName exists
     *
     * @param bindingObject - the EJBBinding
     * @param hr - the bean home record
     * @param local - is local bean
     * @param generateDisambiguatedSimpleBindingNames - A boolean, which when true
     *            will cause any generated simple binding names to be
     *            constructed to include "#<interfaceName>" at the end
     *            of the binding name.
     */
    @Override
    public void bindSimpleBindingName(EJBBinding bindingObject, HomeRecord hr, boolean local, boolean generateDisambiguatedSimpleBindingNames) {
        BeanMetaData bmd = hr.getBeanMetaData();

        if (local) {
            bindLocalSimpleBindingName(bindingObject, hr, bmd.simpleJndiBindingName, generateDisambiguatedSimpleBindingNames);
        } else {
            bindRemoteSimpleBindingName(bindingObject, hr, bmd.simpleJndiBindingName, generateDisambiguatedSimpleBindingNames);
        }
    }

    /**
     * Binds the local bean into local: and ejblocal: namespaces
     *
     * @param bindingObject - the EJBBinding
     * @param hr - the bean home record
     * @param bindingName - the parsed simpleBindingName
     * @param generateDisambiguatedSimpleBindingNames - A boolean, which when true
     *            will cause any generated simple binding names to be
     *            constructed to include "#<interfaceName>" at the end
     *            of the binding name.
     */
    private void bindLocalSimpleBindingName(EJBBinding bindingObject, HomeRecord hr, String bindingName, boolean generateDisambiguatedSimpleBindingNames) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        BeanMetaData bmd = hr.getBeanMetaData();
        boolean priorToVersion3 = bmd.ivModuleVersion < BeanMetaData.J2EE_EJB_VERSION_3_0;

        if (generateDisambiguatedSimpleBindingNames) {
            if (isTraceOn && tc.isDebugEnabled()) {
                Tr.debug(tc, "EJB with simple-binding-name has multiple interfaces, appending interface to simple-binding-name");
            }
        }

        // only bind to local: if EJB2.X binding
        if (priorToVersion3) {
            if (isTraceOn && tc.isDebugEnabled()) {
                Tr.debug(tc, "binding to local:");
            }

            // add ejb/ in front of binding
            String localColonBindingName = "ejb/" + bindingName;

            // In the case where ambiguous simple binding names are possible
            // (for instance multiple business interfaces or presence of
            // homes and business interfaces), disambiguate them by appending
            // the interfaceName to the end of the binding name.
            if (generateDisambiguatedSimpleBindingNames) {
                // TODO: bind AmbiguousEJBReferenceException in the original simple-binding-name
                // value. #11441

                localColonBindingName = localColonBindingName + "#" + bindingObject.interfaceName;

            }

            localColonNamingHelper.bind(bindingObject, localColonBindingName);

            BindingsHelper bh = BindingsHelper.getLocalHelper(hr);
            bh.ivLocalColonBindings.add(localColonBindingName);

            sendBindingMessage(bindingObject.interfaceName, "local:" + localColonBindingName, bmd);

        }

        if (isTraceOn && tc.isDebugEnabled()) {
            Tr.debug(tc, "binding to ejblocal:");
        }

        if (generateDisambiguatedSimpleBindingNames) {
            // TODO: bind AmbiguousEJBReferenceException in the original simple-binding-name
            // value. #11441

            bindingName = bindingName + "#" + bindingObject.interfaceName;

        }

        ejbLocalNamingHelper.bind(bindingObject, bindingName);

        BindingsHelper bh = BindingsHelper.getLocalHelper(hr);
        bh.ivEJBLocalBindings.add(bindingName);

        sendBindingMessage(bindingObject.interfaceName, "ejblocal:" + bindingName, bmd);
    }

    /**
     * Binds the remote bean for simple-binding-name
     *
     * @param bindingObject - the EJBBinding
     * @param hr - the bean home record
     * @param bindingName - the parsed simpleBindingName
     * @param generateDisambiguatedSimpleBindingNames - A boolean, which when true
     *            will cause any generated simple binding names to be
     *            constructed to include "#<interfaceName>" at the end
     *            of the binding name.
     */
    private void bindRemoteSimpleBindingName(EJBBinding bindingObject, HomeRecord hr, String bindingName, boolean generateDisambiguatedSimpleBindingNames) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (generateDisambiguatedSimpleBindingNames) {
            if (isTraceOn && tc.isDebugEnabled()) {
                Tr.debug(tc, "EJB with simple-binding-name has multiple interfaces, appending interface to simple-binding-name");
            }
            // TODO: bind AmbiguousEJBReferenceException in the original simple-binding-name
            // value. #11441

            bindingName = bindingName + "#" + bindingObject.interfaceName;
        }

        bindLegacyRemoteBinding(bindingObject, hr, bindingName);
    }

    /**
     * Binds the localHomeBindingName custom binding
     *
     * @param bindingObject - the EJBBinding
     * @param hr - the bean home record
     */
    @Override
    public void bindLocalHomeBindingName(EJBBinding bindingObject, HomeRecord hr) {
        BeanMetaData bmd = hr.getBeanMetaData();
        String bindingName = bmd.localHomeJndiBindingName;

        ejbLocalNamingHelper.bind(bindingObject, bindingName);

        BindingsHelper bh = BindingsHelper.getLocalHelper(hr);
        bh.ivEJBLocalBindings.add(bindingName);

        sendBindingMessage(bindingObject.interfaceName, bindingName, bmd);
    }

    /**
     * Binds the interface binding-name custom binding for local
     *
     * @param bindingObject - the EJBBinding
     * @param hr - the bean home record
     */
    @Override
    public void bindLocalBusinessInterface(EJBBinding bindingObject, HomeRecord hr) {
        BeanMetaData bmd = hr.getBeanMetaData();
        String interfaceName = bindingObject.interfaceName;
        String bindingName = bmd.businessInterfaceJndiBindingNames.get(interfaceName);

        ejbLocalNamingHelper.bind(bindingObject, bindingName);

        BindingsHelper bh = BindingsHelper.getLocalHelper(hr);
        bh.ivEJBLocalBindings.add(bindingName);

        sendBindingMessage(bindingObject.interfaceName, bindingName, bmd);
    }

    private void sendBindingMessage(String interfaceName, String jndiName, BeanMetaData bmd) {
        Tr.info(tc, "JNDI_BINDING_LOCATION_INFO_CNTR0167I",
                new Object[] { interfaceName,
                               bmd.j2eeName.getComponent(),
                               bmd.j2eeName.getModule(),
                               bmd.j2eeName.getApplication(),
                               jndiName });
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

        if (ContainerProperties.customBindingsEnabledBeta) {
            BeanMetaData bmd = hr.getBeanMetaData();
            boolean hasCustomBindings = false;

            if (bmd.simpleJndiBindingName != null) {
                hasCustomBindings = true;
                bindSimpleBindingName(bindingObject, hr, local, numInterfaces > 1);
            }

            // if the interface Index is -1 it is a home interface
            if (bmd.localHomeJndiBindingName != null && local && interfaceIndex == -1) {
                hasCustomBindings = true;
                bindLocalHomeBindingName(bindingObject, hr);
            }

            if (bmd.businessInterfaceJndiBindingNames != null && interfaceIndex >= 0 && bmd.businessInterfaceJndiBindingNames.containsKey(interfaceName)) {
                hasCustomBindings = true;
                if (local) {
                    bindLocalBusinessInterface(bindingObject, hr);
                } else {
                    // TODO: Remote Business Interface Binding
                }
            }

            // bind default traditional specific JNDI bindings
            if (!hasCustomBindings) {

                if (local) {
                    bindDefaultEJBLocal(bindingObject, hr);
                } else {
                    if (remoteRuntime != null) {
                        bindDefaultEJBRemote(bindingObject, hr);
                    }
                }
            }
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
    public void unbindBindings(HomeRecord hr) throws NamingException {
        if (remoteRuntime != null) {
            HomeRecordImpl hrImpl = HomeRecordImpl.cast(hr);
            if (hrImpl.remoteBindingData != null) {
                remoteRuntime.unbindAll(hrImpl.remoteBindingData);
            }

            BindingsHelper remoteBH = BindingsHelper.getRemoteHelper(hr);
            unbindRemote(remoteBH.ivRemoteBindings);
        }

        BindingsHelper bh = BindingsHelper.getLocalHelper(hr);

        unbindEJBLocal(bh.ivEJBLocalBindings);
        unbindLocalColonEJB(bh.ivLocalColonBindings);
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

    /**
     * Unbind the names from the ejblocal: name space.
     *
     * @param names List of names to remove from the
     *            application name space.
     */
    @Override
    public void unbindEJBLocal(List<String> names) throws NamingException {
        ejbLocalNamingHelper.removeBindings(names);
    }

    /**
     * Undoes the bindings from local namespace.
     *
     * @param names List of names to remove from the
     *            application name space.
     */
    @Override
    public void unbindLocalColonEJB(List<String> names) throws NamingException {
        localColonNamingHelper.removeBindings(names);
    }

    /**
     * Undoes the root remote bindings.
     *
     * @param names List of names to remove from the
     *            application name space.
     */
    @Override
    public void unbindRemote(List<String> names) {
        for (String name : names) {
            for (ServiceRegistration<?> registration : registrations) {
                if (name.equals(registration.getReference().getProperty(JNDI_SERVICENAME))) {
                    registration.unregister();
                }
            }
        }
    }
}
