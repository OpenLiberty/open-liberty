/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.internal;

import java.lang.reflect.Method;
import java.rmi.Remote;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.container.EJBConfigurationException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.classloading.ClassGenerator;
import com.ibm.ws.container.service.app.deploy.ApplicationClassesContainerInfo;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.ContainerInfo;
import com.ibm.ws.container.service.app.deploy.ContainerInfo.Type;
import com.ibm.ws.container.service.app.deploy.ModuleClassesContainerInfo;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.ws.ejbcontainer.EJBEndpoint;
import com.ibm.ws.ejbcontainer.EJBEndpoints;
import com.ibm.ws.ejbcontainer.jitdeploy.CORBA_Utils;
import com.ibm.ws.ejbcontainer.jitdeploy.JIT_Stub;
import com.ibm.ws.ejbcontainer.osgi.EJBStubClassGenerator;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.LibertyProcess;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.ejbcontainer.JITDeploy;

@Component
public class EJBStubClassGeneratorImpl implements ClassGenerator, ApplicationStateListener, EJBStubClassGenerator {
    private static final TraceComponent tc = Tr.register(EJBStubClassGeneratorImpl.class);

    private static final String ORG_OMG_STUB_PREFIX = "org.omg.stub.";

    private static final int RMIC_COMPATIBLE_ALL = -1;

    @Reference(service = LibertyProcess.class, target = "(wlp.process.type=server)")
    protected void setLibertyProcess(ServiceReference<LibertyProcess> reference) {}

    protected void unsetLibertyProcess(ServiceReference<LibertyProcess> reference) {}

    /**
     * Set of classes that should be generated with maximum RMIC compatibility.
     * This is used to simulate tWAS, which would normally run rmic.
     * <p>
     * This is actually WeakMap[ClassLoader, WeakSet[Class]]. The values are
     * also weak to allow the keys in the map to be garbage collected.
     */
    private final Map<ClassLoader, Set<Class<?>>> rmicCompatibleClassesByLoader = new WeakHashMap<ClassLoader, Set<Class<?>>>();

    /**
     * Set of class names that should be generated with maximum RMIC compatibility
     * per application. This is used to provide the RMIC compatibility class to
     * the client container. <p>
     *
     * This is actually a WeakHashMap[ApplicationInfo, Set[String]] to ensure that the
     * entries may be garbage collected when an application is removed from the
     * server process. <p>
     */
    private final Map<ApplicationInfo, Set<String>> rmicCompatibleClassNamesByApp = new WeakHashMap<ApplicationInfo, Set<String>>();

    @Override
    @Trivial
    @FFDCIgnore(ClassNotFoundException.class)
    public byte[] generateClass(String name, ClassLoader loader) throws ClassNotFoundException {
        if (!name.startsWith(ORG_OMG_STUB_PREFIX)) {
            String remoteInterfaceName = JIT_Stub.getRemoteInterfaceName(name);
            if (remoteInterfaceName != null) {
                try {
                    // The ORB will try without the org.omg.stub prefix first,
                    // so we don't want to generate a stub if a stub with the
                    // org.omg.stub prefix would be subsequently found.
                    //
                    // Alternatively, we could generate org.omg.stub classes,
                    // but tWAS does it this way, so it's less confusing if we
                    // generate stubs the same way.
                    loader.loadClass(ORG_OMG_STUB_PREFIX + name);
                } catch (ClassNotFoundException e) {
                    // The org.omg.stub class does not exist, so we can generate
                    // the stub without the org.omg.stub prefix.
                    return generateStubClass(remoteInterfaceName, loader);
                }
            }
        }
        return null;
    }

    @FFDCIgnore(ClassNotFoundException.class)
    @Sensitive
    private byte[] generateStubClass(String remoteInterfaceName, ClassLoader loader) {
        try {
            Class<?> remoteInterface = loader.loadClass(remoteInterfaceName);
            int rmicCompatible = isRMICCompatibleClass(remoteInterface, loader) ? RMIC_COMPATIBLE_ALL : JITDeploy.RMICCompatible;
            return JITDeploy.generateStubBytes(remoteInterface, rmicCompatible);
        } catch (ClassNotFoundException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "unable to load remote interface class: " + e);
        } catch (EJBConfigurationException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "failed to process " + remoteInterfaceName + " as a remote interface", e);
        }
        return null;
    }

    private synchronized boolean isRMICCompatibleClass(Class<?> c, ClassLoader loader) {
        Set<Class<?>> rmicCompatibleClasses = rmicCompatibleClassesByLoader.get(loader);
        return rmicCompatibleClasses != null && rmicCompatibleClasses.contains(c);
    }

    @Override
    public Set<String> getRMICCompatibleClasses(String appName) {
        // Create an empty set to return if none found; otherwise use it to
        // make a copy to protect against caller modification.
        Set<String> rmicCompatibleClassNames = new HashSet<String>();
        synchronized (rmicCompatibleClassNamesByApp) {
            for (Entry<ApplicationInfo, Set<String>> entry : rmicCompatibleClassNamesByApp.entrySet()) {
                if (appName.equals(entry.getKey().getName())) {
                    rmicCompatibleClassNames.addAll(entry.getValue());
                    break;
                }
            }
        }
        return rmicCompatibleClassNames;
    }

    @Override
    public void addRMICCompatibleClasses(ClassLoader loader, Set<String> classesToAdd) {
        synchronized (rmicCompatibleClassesByLoader) {
            Set<Class<?>> rmicCompatibleClasses = rmicCompatibleClassesByLoader.get(loader);
            if (rmicCompatibleClasses == null) {
                rmicCompatibleClasses = Collections.newSetFromMap(new WeakHashMap<Class<?>, Boolean>());
                rmicCompatibleClassesByLoader.put(loader, rmicCompatibleClasses);
            }

            for (String rmicCompatibleClass : classesToAdd) {
                addRMICCompatibleClassIfNeeded(rmicCompatibleClasses, loader, rmicCompatibleClass);
            }
        }
    }

    @Override
    public void applicationStarting(ApplicationInfo appInfo) throws StateChangeException {
        // In tWAS, pre-EJB 3 modules are processed by ejbdeploy, and rmic is
        // used to generate stubs for remote home and interface classes.  These
        // stubs need to exist so that we do not dynamically generate stubs that
        // use the "WAS EJB 3" marshalling rules.
        //
        // In Liberty, there is no separate deploy step, so we need to ensure
        // that stubs for pre-EJB 3 modules are generated with as much
        // compatibility with RMIC as we can.

        NonPersistentCache appCache = getNonPersistentCache(appInfo.getContainer());
        ApplicationClassesContainerInfo appClassesContainerInfo = getFromCache(appCache, ApplicationClassesContainerInfo.class);
        if (appClassesContainerInfo != null) {
            Set<Class<?>> appRmicCompatibleClasses = new HashSet<Class<?>>();
            for (ModuleClassesContainerInfo moduleContainerInfo : appClassesContainerInfo.getModuleClassesContainerInfo()) {
                for (ContainerInfo containerInfo : moduleContainerInfo.getClassesContainerInfo()) {
                    if (containerInfo.getType() == Type.EJB_MODULE) {
                        Container container = containerInfo.getContainer();

                        EJBEndpoints ejbEndpoints = getEJBEndpoints(container);
                        if (ejbEndpoints != null) {
                            if (ejbEndpoints.getModuleVersion() < BeanMetaData.J2EE_EJB_VERSION_3_0) {
                                NonPersistentCache moduleCache = getNonPersistentCache(container);
                                ModuleInfo moduleInfo = getFromCache(moduleCache, ModuleInfo.class);
                                ClassLoader loader = moduleInfo.getClassLoader();

                                synchronized (rmicCompatibleClassesByLoader) {
                                    Set<Class<?>> rmicCompatibleClasses = rmicCompatibleClassesByLoader.get(loader);
                                    if (rmicCompatibleClasses == null) {
                                        rmicCompatibleClasses = Collections.newSetFromMap(new WeakHashMap<Class<?>, Boolean>());
                                        rmicCompatibleClassesByLoader.put(loader, rmicCompatibleClasses);
                                    }

                                    for (EJBEndpoint ejbEndpoint : ejbEndpoints.getEJBEndpoints()) {
                                        addRMICCompatibleClassIfNeeded(rmicCompatibleClasses, loader, ejbEndpoint.getHomeInterfaceName());
                                        addRMICCompatibleClassIfNeeded(rmicCompatibleClasses, loader, ejbEndpoint.getRemoteInterfaceName());
                                    }

                                    // Collect all classes for the application across all ClassLoaders
                                    appRmicCompatibleClasses.addAll(rmicCompatibleClasses);
                                }
                            }
                        }
                    }
                }
            }
            if (appRmicCompatibleClasses.size() > 0) {
                Set<String> rmicCompatibleClassNames = new HashSet<String>(appRmicCompatibleClasses.size());
                for (Class<?> rmicCompatibleClass : appRmicCompatibleClasses) {
                    rmicCompatibleClassNames.add(rmicCompatibleClass.getName());
                }
                synchronized (rmicCompatibleClassNamesByApp) {
                    rmicCompatibleClassNamesByApp.put(appInfo, rmicCompatibleClassNames);
                }
            }
        }
    }

    private static NonPersistentCache getNonPersistentCache(Container container) throws StateChangeException {
        try {
            return container.adapt(NonPersistentCache.class);
        } catch (UnableToAdaptException e) {
            throw new StateChangeException(e);
        }
    }

    @Trivial
    @SuppressWarnings("unchecked")
    private static <T> T getFromCache(NonPersistentCache cache, Class<T> klass) {
        return (T) cache.getFromCache(klass);
    }

    private static EJBEndpoints getEJBEndpoints(Container container) throws StateChangeException {
        try {
            return container.adapt(EJBEndpoints.class);
        } catch (UnableToAdaptException e) {
            throw new StateChangeException(e);
        }
    }

    private void addRMICCompatibleClassIfNeeded(@Sensitive Set<Class<?>> rmicCompatibleClasses, ClassLoader loader, String className) {
        if (className != null) {
            try {
                addRMICCompatibleClassIfNeeded(rmicCompatibleClasses, loader.loadClass(className));
            } catch (ClassNotFoundException e) {
            }
        }
    }

    @Trivial
    private void addRMICCompatibleClassIfNeeded(Set<Class<?>> rmicCompatibleClasses, Class<?> c) {
        // rmic only generates stubs for types that could be remote object
        // references.
        if (CORBA_Utils.isRemoteable(c, RMIC_COMPATIBLE_ALL) &&
            c != Remote.class &&
            c != org.omg.CORBA.Object.class &&
            rmicCompatibleClasses.add(c)) {
            addReferencedRMICCompatibleClasses(rmicCompatibleClasses, c);
        }
    }

    private void addReferencedRMICCompatibleClasses(@Sensitive Set<Class<?>> rmicCompatibleClasses, Class<?> c) {
        // rmic recursively generates stubs for all extended interfaces and
        // for all return types and paramter values that could be remote
        // object references.
        for (Class<?> intf : c.getInterfaces()) {
            addRMICCompatibleClassIfNeeded(rmicCompatibleClasses, intf);
        }

        for (Method method : c.getMethods()) {
            addRMICCompatibleClassIfNeeded(rmicCompatibleClasses, method.getReturnType());
            for (Class<?> paramType : method.getParameterTypes()) {
                addRMICCompatibleClassIfNeeded(rmicCompatibleClasses, paramType);
            }
        }
    }

    @Override
    public void applicationStarted(ApplicationInfo appInfo) {}

    @Override
    public void applicationStopping(ApplicationInfo appInfo) {}

    @Override
    public void applicationStopped(ApplicationInfo appInfo) {}
}
