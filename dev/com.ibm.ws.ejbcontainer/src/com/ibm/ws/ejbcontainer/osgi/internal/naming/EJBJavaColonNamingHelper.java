/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi.internal.naming;

import java.rmi.Remote;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.ejs.container.EJSHome;
import com.ibm.ejs.container.EJSWrapperCommon;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.container.service.metadata.MetaDataEvent;
import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.container.service.metadata.MetaDataSlotService;
import com.ibm.ws.container.service.metadata.MetaDataUtils;
import com.ibm.ws.container.service.metadata.ModuleMetaDataListener;
import com.ibm.ws.container.service.naming.JavaColonNamespaceBindings;
import com.ibm.ws.container.service.naming.JavaColonNamingHelper;
import com.ibm.ws.container.service.naming.NamingConstants;
import com.ibm.ws.container.service.naming.NamingConstants.JavaColonNamespace;
import com.ibm.ws.container.service.naming.RemoteJavaColonNamingHelper;
import com.ibm.ws.container.service.naming.RemoteObjectInstance;
import com.ibm.ws.container.service.naming.RemoteObjectInstanceFactory;
import com.ibm.ws.ejbcontainer.osgi.EJBHomeRuntime;
import com.ibm.ws.ejbcontainer.osgi.EJBRemoteRuntime;
import com.ibm.ws.jndi.url.contexts.javacolon.JavaJNDIComponentMetaDataAccessor;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.MetaDataSlot;
import com.ibm.ws.runtime.metadata.ModuleMetaData;

/**
 * This {@link JavaColonNamingHelper} implementation provides support for
 * the standard Java EE component naming context for java:global, java:app and
 * java:module. <p>
 * 
 */
@Component(service = { JavaColonNamingHelper.class, ModuleMetaDataListener.class, EJBJavaColonNamingHelper.class, RemoteJavaColonNamingHelper.class })
@Trivial
public class EJBJavaColonNamingHelper implements JavaColonNamingHelper, JavaColonNamespaceBindings.ClassNameProvider<EJBBinding>, ModuleMetaDataListener, RemoteJavaColonNamingHelper {
    private static final TraceComponent tc = Tr.register(EJBJavaColonNamingHelper.class);

    /**
     * Namespace bindings for java:global.
     */
    private final JavaColonNamespaceBindings<EJBBinding> javaColonGlobalBindings =
                    new JavaColonNamespaceBindings<EJBBinding>(NamingConstants.JavaColonNamespace.GLOBAL, this);

    private final ReentrantReadWriteLock javaColonLock = new ReentrantReadWriteLock();
    protected MetaDataSlotService metaDataSlotService;
    protected volatile boolean homeRuntime;
    protected volatile boolean remoteRuntime;
    private RemoteObjectInstanceFactory roiFactory;

    /**
     * Module meta data slot for the java:module naming map.
     */
    protected MetaDataSlot mmdSlot;

    /**
     * Module meta data slot for the java:app naming map.
     */
    protected MetaDataSlot amdSlot;

    @Activate
    protected void activate(ComponentContext context) {
        mmdSlot = metaDataSlotService.reserveMetaDataSlot(ModuleMetaData.class);
        amdSlot = metaDataSlotService.reserveMetaDataSlot(ApplicationMetaData.class);
    }

    @Reference
    protected void setMetaDataSlotService(MetaDataSlotService service) {
        metaDataSlotService = service;
    }

    protected void unsetMetaDataSlotService(MetaDataSlotService service) {
        metaDataSlotService = null;
    }

    @Reference(service = EJBHomeRuntime.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC)
    protected void setEJBHomeRuntime(ServiceReference<EJBHomeRuntime> ref) {
        homeRuntime = true;
    }

    protected void unsetEJBHomeRuntime(ServiceReference<EJBHomeRuntime> ref) {
        homeRuntime = false;
    }

    @Reference(service = EJBRemoteRuntime.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC)
    protected void setEJBRemoteRuntime(ServiceReference<EJBRemoteRuntime> ref) {
        remoteRuntime = true;
    }

    protected void unsetEJBRemoteRuntime(ServiceReference<EJBRemoteRuntime> ref) {
        remoteRuntime = false;
    }

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setRemoteObjectInstanceFactory(RemoteObjectInstanceFactory roiFactory) {
        this.roiFactory = roiFactory;
    }

    protected void unsetRemoteObjectInstanceFactory(RemoteObjectInstanceFactory roiFactory) {
        this.roiFactory = null;
    }

    @Override
    public String getBindingClassName(EJBBinding binding) {
        return binding.interfaceName;
    }

    /** {@inheritDoc} */
    @Override
    public Object getObjectInstance(JavaColonNamespace namespace, String name) throws NamingException {

        Object instance;

        // This helper only provides support for java:global, java:app, and java:module
        if (namespace == JavaColonNamespace.GLOBAL) {
            instance = processJavaColonGlobal(name);
        } else if (namespace == JavaColonNamespace.APP) {
            instance = processJavaColonApp(name);
        } else if (namespace == JavaColonNamespace.MODULE) {
            instance = processJavaColonModule(name);
        } else {
            instance = null;
        }

        return instance;
    }

    /**
     * This method process lookup requests for java:global.
     * 
     * @param name
     * @param cmd
     * @return the EJB object instance.
     * @throws NamingException
     */
    private Object processJavaColonGlobal(String name) throws NamingException {
        // Called to ensure that the java:global lookup code path
        // is coming from a Java EE thread. If not this will reject the
        // lookup with the correct Java EE error message.
        getComponentMetaData(JavaColonNamespace.GLOBAL, name);
        Lock readLock = javaColonLock.readLock();
        readLock.lock();

        EJBBinding binding;
        try {
            binding = javaColonGlobalBindings.lookup(name);
        } finally {
            readLock.unlock();
        }

        return processJavaColon(binding, JavaColonNamespace.GLOBAL, name);
    }

    /**
     * This method process lookup requests for java:app.
     * 
     * @param appName Application name.
     * @param lookupName JNDI lookup name.
     * @param cmd Component metadata.
     * @return the EJB object instance.
     * @throws NamingException
     */
    private Object processJavaColonApp(String lookupName) throws NamingException {

        ComponentMetaData cmd = getComponentMetaData(JavaColonNamespace.APP, lookupName);
        ModuleMetaData mmd = cmd.getModuleMetaData();
        ApplicationMetaData amd = mmd.getApplicationMetaData();

        Lock readLock = javaColonLock.readLock();
        readLock.lock();

        EJBBinding binding = null;
        try {
            JavaColonNamespaceBindings<EJBBinding> appMap = getAppBindingMap(amd);
            if (appMap != null) {
                binding = appMap.lookup(lookupName);
            }
        } finally {
            readLock.unlock();
        }

        return processJavaColon(binding, JavaColonNamespace.APP, lookupName);
    }

    /**
     * This method process lookup requests for java:module.
     * 
     * @param lookupName JNDI lookup name
     * @param cmd The component metadata
     * @return the EJB object instance.
     * @throws NamingException
     */
    private Object processJavaColonModule(String lookupName) throws NamingException {
        ComponentMetaData cmd = getComponentMetaData(JavaColonNamespace.MODULE, lookupName);
        ModuleMetaData mmd = cmd.getModuleMetaData();
        JavaColonNamespaceBindings<EJBBinding> modMap = getModuleBindingMap(mmd);
        EJBBinding binding = modMap.lookup(lookupName);

        return processJavaColon(binding, JavaColonNamespace.MODULE, lookupName);
    }

    /**
     * This method process lookup requests for java:
     * 
     * @param binding EJBBindings data
     * @param name JNDI lookup name
     * @param cmd Component meta data.
     * @return the EJB object instance.
     * @throws NamingException
     */
    protected Object processJavaColon(EJBBinding binding, JavaColonNamespace jndiType, String lookupName) throws NamingException {

        Object instance = null;

        if (binding == null) {
            return null;
        }

        // Home and remote interfaces are not supported in Liberty
        if (binding.isHome() && !homeRuntime) {
            throwCannotInstanciateUnsupported(binding, jndiType, lookupName,
                                              "JNDI_CANNOT_INSTANTIATE_HOME_CNTR4008E");
        }

        if (!binding.isLocal && !remoteRuntime) {
            throwCannotInstanciateUnsupported(binding, jndiType, lookupName,
                                              "JNDI_CANNOT_INSTANTIATE_REMOTE_CNTR4009E");
        }

        try {
            EJSHome home = binding.homeRecord.getHomeAndInitialize();

            if (binding.isHome()) {
                EJSWrapperCommon wc = home.getWrapper();
                if (binding.isLocal) {
                    instance = wc.getLocalObject();
                } else {
                    instance = home.getContainer().getEJBRuntime().getRemoteReference(wc.getRemoteWrapper());
                }
            } else {
                // Use interface name to create the business object
                if (binding.isLocal) {
                    instance = home.createLocalBusinessObject(binding.interfaceIndex, null);
                } else {
                    instance = home.createRemoteBusinessObject(binding.interfaceIndex, null);
                }
            }
        } catch (Throwable t) {
            throwCannotInstanciateObjectException(binding,
                                                  jndiType,
                                                  lookupName,
                                                  t);
        }

        return instance;
    }

    /** {@inheritDoc} */
    // can suppress warning - if cmd is null NamingException will be thrown by getComponentMetaData
    @Override
    public boolean hasObjectWithPrefix(JavaColonNamespace namespace, String name) throws NamingException {

        JavaColonNamespaceBindings<EJBBinding> bindings;
        boolean result = false;

        Lock readLock = null;
        ComponentMetaData cmd = null;

        try {
            // This helper only provides support for java:global, java:app, and java:module
            if (namespace == JavaColonNamespace.GLOBAL) {
                // Called to ensure that the java:global code path
                // is coming from a Java EE thread. If not this will reject this
                // method call with the correct Java EE error message.
                cmd = getComponentMetaData(namespace, name);
                bindings = javaColonGlobalBindings;
                readLock = javaColonLock.readLock();
                readLock.lock();

            } else if (namespace == JavaColonNamespace.APP) {
                // Get the ComponentMetaData for the currently active component.
                // There is no component name space if there is no active component.
                cmd = getComponentMetaData(namespace, name);
                bindings = getAppBindingMap(cmd.getModuleMetaData().getApplicationMetaData());

                readLock = javaColonLock.readLock();
                readLock.lock();

            } else if (namespace == JavaColonNamespace.MODULE) {
                cmd = getComponentMetaData(namespace, name);
                bindings = getModuleBindingMap(cmd.getModuleMetaData());
            } else {
                bindings = null;
            }

            result = bindings != null && bindings.hasObjectWithPrefix(name);
        } finally {
            if (readLock != null) {
                readLock.unlock();
            }
        }
        return result;
    }

    /**
     * Add a java:global binding object to the global mapping.
     * 
     * @param name lookup name
     * @param bindingObject object to use to instantiate EJB at lookup time.
     * @return
     */
    public synchronized void addGlobalBinding(String name, EJBBinding bindingObject) {
        Lock writeLock = javaColonLock.writeLock();
        writeLock.lock();

        try {
            javaColonGlobalBindings.bind(name, bindingObject);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Remove names from the global mapping.
     * 
     * @param names List of names to remove.
     */
    public void removeGlobalBindings(List<String> names) {
        Lock writeLock = javaColonLock.writeLock();
        writeLock.lock();

        try {
            for (String name : names) {
                javaColonGlobalBindings.unbind(name);
            }
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Add a java:app binding object to the mapping.
     * 
     * @param name lookup name
     * @param bindingObject object to use to instantiate EJB at lookup time.
     * @return
     * @throws NamingException
     */
    public synchronized void addAppBinding(ModuleMetaData mmd, String name, EJBBinding bindingObject) {

        Lock writeLock = javaColonLock.writeLock();
        writeLock.lock();

        try {
            JavaColonNamespaceBindings<EJBBinding> bindings = getAppBindingMap(mmd.getApplicationMetaData());
            bindings.bind(name, bindingObject);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Add a java:module binding to the map.
     * Lock not required since the the map is unique to one module.
     * 
     * @param mmd module meta data
     * @param name lookup name
     * @param bindingObject
     */
    public void addModuleBinding(ModuleMetaData mmd, String name, EJBBinding bindingObject) {
        JavaColonNamespaceBindings<EJBBinding> bindingMap = getModuleBindingMap(mmd);
        bindingMap.bind(name, bindingObject);
    }

    /**
     * Get the EJBBinding map from the application meta data. Initialize if it
     * is null.
     * 
     * @param amd
     * @return Map for the lookup names and binding object.
     */
    private JavaColonNamespaceBindings<EJBBinding> getAppBindingMap(ApplicationMetaData amd) {
        @SuppressWarnings("unchecked")
        JavaColonNamespaceBindings<EJBBinding> bindingMap = (JavaColonNamespaceBindings<EJBBinding>) amd.getMetaData(amdSlot);
        if (bindingMap == null) {
            bindingMap = new JavaColonNamespaceBindings<EJBBinding>(NamingConstants.JavaColonNamespace.APP, this);
            amd.setMetaData(amdSlot, bindingMap);
        }
        return bindingMap;
    }

    /**
     * Get the EJBBinding map from the module meta data. Initialize if it
     * is null.
     * 
     * @param mmd
     * @return Map for the lookup names and binding object.
     */
    private JavaColonNamespaceBindings<EJBBinding> getModuleBindingMap(ModuleMetaData mmd) {
        @SuppressWarnings("unchecked")
        JavaColonNamespaceBindings<EJBBinding> bindingMap = (JavaColonNamespaceBindings<EJBBinding>) mmd.getMetaData(mmdSlot);
        if (bindingMap == null) {
            bindingMap = new JavaColonNamespaceBindings<EJBBinding>(NamingConstants.JavaColonNamespace.MODULE, this);
            mmd.setMetaData(mmdSlot, bindingMap);
        }
        return bindingMap;
    }

    /**
     * Remove names from the application mapping. If all the bindings have
     * been removed for an application, remove the application mapping.
     * 
     * @param moduleMetaData Name of the application being used.
     * @param names List of names to remove.
     */
    public void removeAppBindings(ModuleMetaData mmd, List<String> names) {

        ApplicationMetaData amd = mmd.getApplicationMetaData();

        Lock writeLock = javaColonLock.writeLock();
        writeLock.lock();

        try {
            JavaColonNamespaceBindings<EJBBinding> bindings = getAppBindingMap(amd);
            // getAppBindings returns a non-null value

            for (String name : names) {
                bindings.unbind(name);
            }

        } finally {
            writeLock.unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public Collection<? extends NameClassPair> listInstances(JavaColonNamespace namespace, String nameInContext) throws NamingException {

        // only handles java:global, java:app, and java:module
        if (namespace != JavaColonNamespace.GLOBAL &&
            namespace != JavaColonNamespace.APP &&
            namespace != JavaColonNamespace.MODULE) {
            return Collections.emptyList();
        }

        ComponentMetaData cmd = getComponentMetaData(namespace, nameInContext);

        JavaColonNamespaceBindings<EJBBinding> bindingsMap;
        boolean lockingRequired = true;

        if (namespace == JavaColonNamespace.GLOBAL) {
            bindingsMap = javaColonGlobalBindings;
        } else if (namespace == JavaColonNamespace.MODULE) {
            bindingsMap = getModuleBindingMap(cmd.getModuleMetaData());
            lockingRequired = false;
        } else {
            bindingsMap = getAppBindingMap(cmd.getModuleMetaData().getApplicationMetaData());
        }

        Collection<? extends NameClassPair> retVal = null;

        Lock readLock = null;
        if (lockingRequired) {
            readLock = javaColonLock.readLock();
            readLock.lock();
        }

        try {
            retVal = bindingsMap.listInstances(nameInContext);
        } finally {
            if (readLock != null) {
                readLock.unlock();
            }
        }

        return retVal;
    }

    /**
     * Get the component meta data.
     */
    protected ComponentMetaData getComponentMetaData(JavaColonNamespace namespace, String name) throws NamingException {
        return JavaJNDIComponentMetaDataAccessor.getComponentMetaData(namespace, name);
    }

    protected J2EEName getJ2EEName(EJBBinding binding) {
        return binding.homeRecord.getJ2EEName();
    }

    /**
     * Internal method that creates a NamingException that contains cause
     * information regarding why a binding failed to resolve. <p>
     * 
     * The returned exception will provide similar information as the
     * CannotInstantiateObjectException from traditional WAS.
     */
    private NamingException throwCannotInstanciateObjectException(EJBBinding binding,
                                                                  JavaColonNamespace jndiType,
                                                                  String lookupName,
                                                                  Throwable cause) throws NamingException {
        String jndiName = jndiType.toString() + "/" + lookupName;
        J2EEName j2eeName = getJ2EEName(binding);
        Object causeMsg = cause.getLocalizedMessage();
        if (causeMsg == null) {
            causeMsg = cause.toString();
        }
        String msgTxt = Tr.formatMessage(tc, "JNDI_CANNOT_INSTANTIATE_OBJECT_CNTR4007E",
                                         binding.interfaceName,
                                         j2eeName.getComponent(),
                                         j2eeName.getModule(),
                                         j2eeName.getApplication(),
                                         jndiName,
                                         causeMsg);
        NamingException nex = new NamingException(msgTxt);
        nex.initCause(cause);
        throw nex;
    }

    /**
     * Internal method to throw a NameNotFoundException for unsupported
     * Home and Remote interfaces.
     * 
     * @param binding
     * @param jndiType
     * @param lookupName
     * @param cmd
     * @param messageId
     * @throws NameNotFoundException
     */
    private void throwCannotInstanciateUnsupported(EJBBinding binding,
                                                   JavaColonNamespace jndiType,
                                                   String lookupName,
                                                   String messageId) throws NameNotFoundException {
        J2EEName j2eeName = getJ2EEName(binding);
        String jndiName = jndiType.toString() + "/" + lookupName;
        String msgTxt = Tr.formatMessage(tc, messageId,
                                         binding.interfaceName,
                                         j2eeName.getComponent(),
                                         j2eeName.getModule(),
                                         j2eeName.getApplication(),
                                         jndiName);
        throw (new NameNotFoundException(msgTxt));
    }

    /** {@inheritDoc} */
    @Override
    public void moduleMetaDataCreated(MetaDataEvent<ModuleMetaData> event) throws MetaDataException {
        if (!MetaDataUtils.copyModuleMetaDataSlot(event, mmdSlot)) {
            getModuleBindingMap(event.getMetaData());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void moduleMetaDataDestroyed(MetaDataEvent<ModuleMetaData> event) {
        // We don't need to do anything here.
    }

    private EJBBinding getEJBBinding(JavaColonNamespace namespace, String name) throws NamingException {
        ComponentMetaData cmd = getComponentMetaData(namespace, name);
        ModuleMetaData mmd = cmd.getModuleMetaData();
        ApplicationMetaData amd = mmd == null ? null : mmd.getApplicationMetaData();

        Lock readLock = javaColonLock.readLock();
        readLock.lock();

        EJBBinding binding = null;
        try {
            if (JavaColonNamespace.GLOBAL.equals(namespace)) {
                binding = javaColonGlobalBindings.lookup(name);
            } else if (amd != null && JavaColonNamespace.APP.equals(namespace)) {
                JavaColonNamespaceBindings<EJBBinding> appMap = getAppBindingMap(amd);
                if (appMap != null) {
                    binding = appMap.lookup(name);
                }
            } else if (mmd != null && JavaColonNamespace.MODULE.equals(namespace)) {
                JavaColonNamespaceBindings<EJBBinding> modMap = getModuleBindingMap(mmd);
                binding = modMap.lookup(name);
            }
        } finally {
            readLock.unlock();
        }
        return binding;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.container.service.naming.remote.RemoteJavaColonNamingHelper#getRemoteObjectInstance(com.ibm.ws.container.service.naming.NamingConstants.JavaColonNamespace,
     * java.lang.String)
     */
    @Override
    public RemoteObjectInstance getRemoteObjectInstance(JavaColonNamespace namespace, String name) throws NamingException {
        RemoteObjectInstance roi = null;
        Object o = getObjectInstance(namespace, name);
        if (o != null && o instanceof java.rmi.Remote) {
            EJBBinding ejbBinding = getEJBBinding(namespace, name);
            if (ejbBinding == null || ejbBinding.interfaceName == null) {
                throw new NamingException("Unable to determine bound EJB's remote interface");
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "getObjectInstance() ==> " + o + "  interfaceName = " + ejbBinding.interfaceName);
            }

            roi = roiFactory.create((Remote) o, ejbBinding.interfaceName);
        }
        return roi;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.container.service.naming.remote.RemoteJavaColonNamingHelper#hasRemoteObjectWithPrefix(com.ibm.ws.container.service.naming.NamingConstants.JavaColonNamespace,
     * java.lang.String)
     */
    @Override
    public boolean hasRemoteObjectWithPrefix(JavaColonNamespace namespace, String name) throws NamingException {
        return hasObjectWithPrefix(namespace, name);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.container.service.naming.remote.RemoteJavaColonNamingHelper#listRemoteInstances(com.ibm.ws.container.service.naming.NamingConstants.JavaColonNamespace,
     * java.lang.String)
     */
    @Override
    public Collection<? extends NameClassPair> listRemoteInstances(JavaColonNamespace namespace, String nameInContext) throws NamingException {
        return listInstances(namespace, nameInContext);
    }

}
