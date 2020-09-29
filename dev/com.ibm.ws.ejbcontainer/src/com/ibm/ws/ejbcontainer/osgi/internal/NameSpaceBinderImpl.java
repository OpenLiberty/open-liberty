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
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
import com.ibm.wsspi.kernel.service.utils.OnErrorUtil.OnError;

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

    private static final List<ServiceRegistration<?>> registrations = Collections.synchronizedList(new ArrayList<ServiceRegistration<?>>());
    private static final HashMap<String, EJBBinding> svRemoteBindings = new HashMap<String, EJBBinding>();
    private final ReentrantReadWriteLock remoteLock = new ReentrantReadWriteLock();

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
     * @param name          The lookup name.
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
     * @param name          The lookup name.
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
     * @param name          The lookup name.
     * @param bindingObject The EJB binding information.
     */
    @Override
    public void bindJavaModule(String name, EJBBinding bindingObject) {
        ejbJavaColonHelper.addModuleBinding(moduleMetaData, name, bindingObject);
    }

    /**
     * Adds the default local custom bindings if no other binding is specified
     * <app>/<module.jar>/<bean>#<interface> for 3X
     * ejb/<ejb-name> for 1X and 2X
     *
     * @param bindingObject the EJB Binding information
     * @param hr            the HomeRecord of the EJB
     */
    @Override
    public void bindDefaultEJBLocal(EJBBinding bindingObject, HomeRecord hr) throws NamingException {
        HomeRecordImpl hrImpl = HomeRecordImpl.cast(hr);
        BindingsHelper bh = BindingsHelper.getLocalHelper(hr);

        if (hrImpl.bindToContextRoot()) {
            BeanMetaData bmd = hr.getBeanMetaData();
            boolean priorToVersion3 = bmd.ivModuleVersion < BeanMetaData.J2EE_EJB_VERSION_3_0;

            // EJB2X and 1X default
            if (priorToVersion3) {
                // Binding name is ejb/ + ejbName
                String bindingName = "ejb/" + hrImpl.getEJBName();

                // local:
                localColonNamingHelper.bind(bindingObject, bindingName, false);
                bh.ivLocalColonBindings.add(bindingName);
                sendBindingMessage(bindingObject.interfaceName, "local:" + bindingName, bmd);

                // ejblocal:
                ejbLocalNamingHelper.bind(bindingObject, bindingName, false);
                bh.ivEJBLocalBindings.add(bindingName);
                sendBindingMessage(bindingObject.interfaceName, "ejblocal:" + bindingName, bmd);

            } else {
                //EJB3X default
                J2EEName eeName = hrImpl.getJ2EEName();

                // if component-id binding was specified use that, otherwise use default long form
                String bindingName = null;
                if (bmd.ivComponent_Id != null) {
                    bindingName = bmd.ivComponent_Id + "#" + bindingObject.interfaceName;
                } else {
                    // <app>/<module.jar>/<bean>#<interface>
                    bindingName = eeName.getApplication() + "/" + eeName.getModule() + "/" + eeName.getComponent() + "#" + bindingObject.interfaceName;
                }

                ejbLocalNamingHelper.bind(bindingObject, bindingName, false);
                bh.ivEJBLocalBindings.add(bindingName);
                sendBindingMessage(bindingObject.interfaceName, "ejblocal:" + bindingName, bmd);

                // Default Short
                if (BindingsHelper.shortDefaultBindingsEnabled(hrImpl.getAppName())) {
                    bindingName = bindingObject.interfaceName;
                    if (ejbLocalNamingHelper.bind(bindingObject, bindingName, false)) {
                        bh.ivEJBLocalBindings.add(bindingName);
                        sendBindingMessage(bindingObject.interfaceName, "ejblocal:" + bindingName, bmd);
                    }
                }
            }
        }

    }

    /**
     * Adds the default remote legacy bindings to root
     * ejb/<app>/<module.jar>/<bean>#<interface> for 3X
     * ejb/<ejb-name> for 2X
     *
     * @param bindingObject the EJB Binding information
     * @param hr            the HomeRecord of the EJB
     */
    @Override
    public void bindDefaultEJBRemote(EJBBinding bindingObject, HomeRecord hr) throws NamingException {
        HomeRecordImpl hrImpl = HomeRecordImpl.cast(hr);

        if (hrImpl.bindToContextRoot()) {
            BeanMetaData bmd = hr.getBeanMetaData();
            boolean priorToVersion3 = bmd.ivModuleVersion < BeanMetaData.J2EE_EJB_VERSION_3_0;

            // EJB2X and 1X default
            if (priorToVersion3) {
                // Binding name is ejb/ + ejbName
                String bindingName = "ejb/" + hrImpl.getEJBName();
                bindLegacyRemoteBinding(bindingObject, hr, bindingName, false);

            } else {
                //EJB3X default
                String bindingName = null;

                // if component-id binding was specified use that, otherwise use default long form
                if (bmd.ivComponent_Id != null) {
                    bindingName = "ejb/" + bmd.ivComponent_Id + "#" + bindingObject.interfaceName;
                } else {
                    // Default Long
                    J2EEName eeName = hrImpl.getJ2EEName();
                    // ejb/<app>/<module.jar>/<bean>#<interface>
                    bindingName = "ejb/" + eeName.getApplication() + "/" + eeName.getModule() + "/" + eeName.getComponent() + "#" + bindingObject.interfaceName;
                }
                bindLegacyRemoteBinding(bindingObject, hr, bindingName, false);

                // Default Short
                if (BindingsHelper.shortDefaultBindingsEnabled(hrImpl.getAppName())) {
                    bindingName = bindingObject.interfaceName;
                    bindLegacyRemoteBinding(bindingObject, hr, bindingName, false);
                }
            }
        }
    }

    /**
     * Binds a bindingObject with a bindingName to root for legacy remote bindings.
     * To bind to root we register a service to the BundleContext, passing it a Reference Object
     *
     * @param bindingObject the EJB Binding information
     * @param hr            the HomeRecord of the EJB
     * @param bindingName   the JNDI binding name
     * @param isSimpleName  Flag used to force creation of an AmbiguousEJBReference if an
     *                          ambiguous simple name binding is detected
     */
    private void bindLegacyRemoteBinding(EJBBinding bindingObject, HomeRecord hr, String bindingName, boolean isSimpleName) throws NamingException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        EJBRemoteRuntime remoteRuntime = ejbRemoteRuntimeServiceRef.getService();
        if (remoteRuntime != null) {
            BindingsHelper bh = BindingsHelper.getRemoteHelper(hr);
            BundleContext bc = ejbRemoteRuntimeServiceRef.getReference().getBundle().getBundleContext();
            BeanMetaData bmd = hr.getBeanMetaData();
            EJBBinding newBinding = new EJBBinding(bindingObject.homeRecord, bindingObject.interfaceName, bindingObject.interfaceIndex, bindingObject.isLocal);

            Lock readLock = remoteLock.readLock();
            readLock.lock();

            EJBBinding previousBinding = null;
            try {
                previousBinding = svRemoteBindings.get(bindingName);
            } finally {
                readLock.unlock();
            }

            // There won't be a previous binding for an ambiguous simple binding name
            if (isSimpleName) {
                newBinding.setAmbiguousReference();
            }

            if (previousBinding != null) {

                OnError onError = ContainerProperties.customBindingsOnErr;

                if (isTraceOn && tc.isDebugEnabled()) {
                    Tr.debug(tc, "found ambiguous binding and customBindingsOnErr=" + onError.toString());
                }

                BeanMetaData oldbmd = previousBinding.homeRecord.getBeanMetaData();
                switch (onError) {
                    case WARN:
                        //NAME_ALREADY_BOUND_WARN_CNTR0338W=CNTR0338W: The {0} interface of the {1} bean in the {2} module of the {3} application cannot be bound to the {4} name location. The {5} interface of the {6} bean in the {7} module of the {8} application is already bound to the {4} name location. The {4} name location is not accessible.
                        Tr.warning(tc, "NAME_ALREADY_BOUND_WARN_CNTR0338W",
                                   new Object[] { newBinding.interfaceName, bmd.j2eeName.getComponent(), bmd.j2eeName.getModule(), bmd.j2eeName.getApplication(), bindingName,
                                                  previousBinding.interfaceName, oldbmd.j2eeName.getComponent(), oldbmd.j2eeName.getModule(), oldbmd.j2eeName.getApplication() });
                        break;
                    case FAIL:
                        Tr.error(tc, "NAME_ALREADY_BOUND_WARN_CNTR0338W",
                                 new Object[] { newBinding.interfaceName, bmd.j2eeName.getComponent(), bmd.j2eeName.getModule(), bmd.j2eeName.getApplication(), bindingName,
                                                previousBinding.interfaceName, oldbmd.j2eeName.getComponent(), oldbmd.j2eeName.getModule(), oldbmd.j2eeName.getApplication() });
                        throw new NamingException("The " + newBinding.interfaceName + " interface of the " + bmd.j2eeName.getComponent() + " bean in the "
                                                  + bmd.j2eeName.getModule() + " module of the application cannot be bound to " + bindingName
                                                  + ", a bean is already bound to that location.");
                    case IGNORE:
                        if (isTraceOn && tc.isDebugEnabled()) {
                            Tr.debug(tc, "customBindingsOnErr is IGNORE, not binding");
                        }
                        return;
                }

                newBinding.setAmbiguousReference();
                newBinding.addJ2EENames(previousBinding.getJ2EENames());
                removePreviousRemoteBinding(bindingName);
            }

            // Our Service registration object needs some properties saying its a JNDI naming service
            // with a Reference Object.
            Dictionary<String, Object> properties = new Hashtable<String, Object>(1);
            properties.put(JNDI_SERVICENAME, bindingName);
            properties.put(Constants.OBJECTCLASS, Reference.class.getName());

            // Create our wrapper Reference Object to bind
            EJBRemoteReferenceBinding ref = new EJBRemoteReferenceBinding(newBinding, bindingName);

            ServiceRegistration<?> registration = bc.registerService(Reference.class, ref, properties);

            synchronized (registrations) {
                registrations.add(registration);
            }

            Lock writeLock = remoteLock.writeLock();
            writeLock.lock();

            try {
                svRemoteBindings.put(bindingName, newBinding);
            } finally {
                writeLock.unlock();
            }

            if (!newBinding.isAmbiguousReference) {
                // Only add this binding if it is not an AmbiguousEJBReference,
                // otherwise we will try to remove it multiple times (once for each EJB)
                // from the list of ServiceRegistrations when stopping the app
                bh.ivRemoteBindings.add(bindingName);
                sendBindingMessage(newBinding.interfaceName, bindingName, bmd);
            }
        } else {
            if (isTraceOn && tc.isDebugEnabled()) {
                Tr.debug(tc, "Remote Runtime Service isn't enabled, not adding remote binding.");
            }
        }
    }

    /**
     * Binds the simpleBindingName custom binding
     *
     * Caller should ensure the simpleBindingName exists
     *
     * @param bindingObject                            - the EJBBinding
     * @param hr                                      - the bean home record
     * @param local                                   - is local bean
     * @param generateDisambiguatedSimpleBindingNames - A boolean, which when true
     *                                                    will cause any generated simple binding names to be
     *                                                    constructed to include "#<interfaceName>" at the end
     *                                                    of the binding name.
     */
    @Override
    public void bindSimpleBindingName(EJBBinding bindingObject, HomeRecord hr, boolean local, boolean generateDisambiguatedSimpleBindingNames) throws NamingException {
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
     * @param bindingObject                           - the EJBBinding
     * @param hr                                      - the bean home record
     * @param bindingName                             - the parsed simpleBindingName
     * @param generateDisambiguatedSimpleBindingNames - A boolean, which when true
     *                                                    will cause any generated simple binding names to be
     *                                                    constructed to include "#<interfaceName>" at the end
     *                                                    of the binding name.
     */
    private void bindLocalSimpleBindingName(EJBBinding bindingObject, HomeRecord hr, String bindingName, boolean generateDisambiguatedSimpleBindingNames) throws NamingException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        BindingsHelper bh = BindingsHelper.getLocalHelper(hr);
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
                // Bind an AmbiguousEJBReferenceException into the original simple binding name
                localColonNamingHelper.bind(bindingObject, localColonBindingName, true);
                bh.ivLocalColonBindings.add(localColonBindingName);

                localColonBindingName = localColonBindingName + "#" + bindingObject.interfaceName;

            }

            localColonNamingHelper.bind(bindingObject, localColonBindingName, false);
            bh.ivLocalColonBindings.add(localColonBindingName);
            sendBindingMessage(bindingObject.interfaceName, "local:" + localColonBindingName, bmd);

        }

        if (isTraceOn && tc.isDebugEnabled()) {
            Tr.debug(tc, "binding to ejblocal:");
        }

        if (generateDisambiguatedSimpleBindingNames) {
            // Bind an AmbiguousEJBReferenceException into the original simple binding name
            ejbLocalNamingHelper.bind(bindingObject, bindingName, true);
            bh.ivEJBLocalBindings.add(bindingName);

            bindingName = bindingName + "#" + bindingObject.interfaceName;

        }

        ejbLocalNamingHelper.bind(bindingObject, bindingName, false);
        bh.ivEJBLocalBindings.add(bindingName);
        sendBindingMessage(bindingObject.interfaceName, "ejblocal:" + bindingName, bmd);
    }

    /**
     * Binds the remote bean for simple-binding-name
     *
     * @param bindingObject                           - the EJBBinding
     * @param hr                                      - the bean home record
     * @param bindingName                             - the parsed simpleBindingName
     * @param generateDisambiguatedSimpleBindingNames - A boolean, which when true
     *                                                    will cause any generated simple binding names to be
     *                                                    constructed to include "#<interfaceName>" at the end
     *                                                    of the binding name.
     */
    private void bindRemoteSimpleBindingName(EJBBinding bindingObject, HomeRecord hr, String bindingName, boolean generateDisambiguatedSimpleBindingNames) throws NamingException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (generateDisambiguatedSimpleBindingNames) {
            // Bind an AmbiguousEJBReferenceException into the original simple binding name
            bindLegacyRemoteBinding(bindingObject, hr, bindingName, true);

            if (isTraceOn && tc.isDebugEnabled()) {
                Tr.debug(tc, "EJB with simple-binding-name has multiple interfaces, appending interface to simple-binding-name");
            }
            bindingName = bindingName + "#" + bindingObject.interfaceName;
        }

        bindLegacyRemoteBinding(bindingObject, hr, bindingName, false);
    }

    /**
     * Binds the localHomeBindingName custom binding
     *
     * @param bindingObject - the EJBBinding
     * @param hr            - the bean home record
     */
    @Override
    public void bindLocalHomeBindingName(EJBBinding bindingObject, HomeRecord hr) throws NamingException {
        BindingsHelper bh = BindingsHelper.getLocalHelper(hr);
        BeanMetaData bmd = hr.getBeanMetaData();
        String bindingName = bmd.localHomeJndiBindingName;

        ejbLocalNamingHelper.bind(bindingObject, bindingName, false);
        bh.ivEJBLocalBindings.add(bindingName);
        sendBindingMessage(bindingObject.interfaceName, bindingName, bmd);
    }

    /**
     * Binds the interface binding-name custom binding for local
     *
     * @param bindingObject - the EJBBinding
     * @param hr            - the bean home record
     */
    @Override
    public void bindLocalBusinessInterface(EJBBinding bindingObject, HomeRecord hr) throws NamingException {
        BindingsHelper bh = BindingsHelper.getLocalHelper(hr);
        BeanMetaData bmd = hr.getBeanMetaData();
        String interfaceName = bindingObject.interfaceName;
        String bindingName = bmd.businessInterfaceJndiBindingNames.get(interfaceName);

        ejbLocalNamingHelper.bind(bindingObject, bindingName, false);
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
                             boolean deferred) throws NamingException {
        EJBRemoteRuntime remoteRuntime = ejbRemoteRuntimeServiceRef.getService();
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

        // TODO: #13338 change to check ContainerProperties.bindToServerRoot
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
            if (bmd.remoteHomeJndiBindingName != null && !local && interfaceIndex == -1) {
                hasCustomBindings = true;
                bindLegacyRemoteBinding(bindingObject, hr, bmd.remoteHomeJndiBindingName, false);
            }

            if (bmd.businessInterfaceJndiBindingNames != null && interfaceIndex >= 0 && bmd.businessInterfaceJndiBindingNames.containsKey(interfaceName)) {
                hasCustomBindings = true;
                if (local) {
                    bindLocalBusinessInterface(bindingObject, hr);
                } else {
                    bindLegacyRemoteBinding(bindingObject, hr, bmd.businessInterfaceJndiBindingNames.get(interfaceName), false);
                }
            }

            // bind default traditional specific JNDI bindings
            if (!hasCustomBindings) {

                if (local) {
                    bindDefaultEJBLocal(bindingObject, hr);
                } else {
                    bindDefaultEJBRemote(bindingObject, hr);
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
        EJBRemoteRuntime remoteRuntime = ejbRemoteRuntimeServiceRef.getService();
        if (remoteRuntime != null) {
            BindingsHelper remoteBH = BindingsHelper.getRemoteHelper(hr);
            HomeRecordImpl hrImpl = HomeRecordImpl.cast(hr);
            if (hrImpl.remoteBindingData != null) {
                remoteRuntime.unbindAll(hrImpl.remoteBindingData);
            }

            unbindRemote(remoteBH.ivRemoteBindings);

            Lock writeLock = remoteLock.writeLock();
            writeLock.lock();
            try {
                for (String bindingName : remoteBH.ivRemoteBindings) {
                    svRemoteBindings.remove(bindingName);
                }
            } finally {
                writeLock.unlock();
            }
        }

        BindingsHelper localBH = BindingsHelper.getLocalHelper(hr);
        unbindEJBLocal(localBH.ivEJBLocalBindings);
        unbindLocalColonEJB(localBH.ivLocalColonBindings);
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
        ServiceRegistration<?> removedRegistration = null;

        synchronized (registrations) {
            for (String name : names) {
                for (Iterator<ServiceRegistration<?>> it = registrations.iterator(); it.hasNext();) {
                    ServiceRegistration<?> registration = it.next();
                    if (name.equals(registration.getReference().getProperty(JNDI_SERVICENAME))) {
                        removedRegistration = registration;
                        registration.unregister();
                        break;
                    }
                }

                if (removedRegistration != null) {
                    registrations.remove(removedRegistration);
                    removedRegistration = null;
                }
            }
        }
    }

    private void removePreviousRemoteBinding(String bindingName) {
        ServiceRegistration<?> removedRegistration = null;

        synchronized (registrations) {
            for (Iterator<ServiceRegistration<?>> it = registrations.iterator(); it.hasNext();) {
                ServiceRegistration<?> registration = it.next();
                if (bindingName.equals(registration.getReference().getProperty(JNDI_SERVICENAME))) {
                    removedRegistration = registration;
                    registration.unregister();
                    break;
                }
            }

            if (removedRegistration != null) {
                registrations.remove(removedRegistration);
            }
        }
    }
}
