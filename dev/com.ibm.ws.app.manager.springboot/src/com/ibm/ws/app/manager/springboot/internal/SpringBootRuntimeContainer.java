/*******************************************************************************
 * Copyright (c) 2018, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.springboot.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.app.manager.springboot.container.ApplicationError;
import com.ibm.ws.app.manager.springboot.internal.SpringBootRuntimeContainer.SpringModuleMetaData.SpringBootComponentMetaData;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedApplicationInfo;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedModuleInfo;
import com.ibm.ws.container.service.app.deploy.extended.ModuleRuntimeContainer;
import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.container.service.metadata.extended.DeferredMetaDataFactory;
import com.ibm.ws.container.service.metadata.extended.IdentifiableComponentMetaData;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.ws.kernel.LibertyProcess;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.MetaDataImpl;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.ws.threading.FutureMonitor;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM", "type:String=spring", "deferredMetaData=SPRING" })
public class SpringBootRuntimeContainer implements ModuleRuntimeContainer, DeferredMetaDataFactory {
    private static final TraceComponent tc = Tr.register(SpringBootRuntimeContainer.class);

    static class SpringModuleMetaData extends MetaDataImpl implements ModuleMetaData {
        private final SpringBootModuleInfo moduleInfo;
        private final SpringBootComponentMetaData componentMetaData;

        public SpringModuleMetaData(SpringBootModuleInfo moduleInfo) {
            super(0);
            this.moduleInfo = moduleInfo;
            this.componentMetaData = new SpringBootComponentMetaData();
        }

        @Override
        public String getName() {
            return moduleInfo.getName();
        }

        @Override
        public ApplicationMetaData getApplicationMetaData() {
            return ((ExtendedApplicationInfo) moduleInfo.getApplicationInfo()).getMetaData();
        }

        @Override
        public ComponentMetaData[] getComponentMetaDatas() {
            return new ComponentMetaData[] { componentMetaData };
        }

        SpringBootComponentMetaData getComponentMetaData() {
            return componentMetaData;
        }

        @Override
        public J2EEName getJ2EEName() {
            return ((ExtendedApplicationInfo) moduleInfo.getApplicationInfo()).getMetaData().getJ2EEName();
        }

        class SpringBootComponentMetaData extends MetaDataImpl implements ComponentMetaData, IdentifiableComponentMetaData {
            public SpringBootComponentMetaData() {
                super(0);
            }

            @Override
            public String getName() {
                return SpringModuleMetaData.this.getName();
            }

            @Override
            public J2EEName getJ2EEName() {
                return SpringModuleMetaData.this.getJ2EEName();
            }

            @Override
            public ModuleMetaData getModuleMetaData() {
                return SpringModuleMetaData.this;
            }

            @Override
            public String getPersistentIdentifier() {
                return "SPRING#" + getName();
            }

            ClassLoader getClassLoader() {
                return moduleInfo.getClassLoader();
            }
        }
    }

    private final ExecutorService executor;
    private final FutureMonitor futureMonitor;
    private final LibertyProcess libertyProcess;
    private final Map<String, ComponentMetaData> components = new ConcurrentHashMap<>();

    @Activate
    public SpringBootRuntimeContainer(@Reference ExecutorService executor, @Reference FutureMonitor futureMonitor,
                                      @Reference LibertyProcess libertyProcess) {
        this.executor = executor;
        this.futureMonitor = futureMonitor;
        this.libertyProcess = libertyProcess;
    }

    @Override
    public ModuleMetaData createModuleMetaData(ExtendedModuleInfo moduleInfo) throws MetaDataException {
        return new SpringModuleMetaData((SpringBootModuleInfo) moduleInfo);
    }

    @Override
    public Future<Boolean> startModule(ExtendedModuleInfo moduleInfo) throws StateChangeException {
        SpringBootModuleInfo springBootModuleInfo = (SpringBootModuleInfo) moduleInfo;
        SpringBootComponentMetaData springBootComponentMetaData = ((SpringModuleMetaData) springBootModuleInfo.getMetaData()).getComponentMetaData();
        components.put(springBootComponentMetaData.getPersistentIdentifier(), springBootComponentMetaData);
        Future<Boolean> result = futureMonitor.createFuture(Boolean.class);
        invokeSpringMain(result, springBootModuleInfo);
        return result;
    }

    private void invokeSpringMain(Future<Boolean> mainInvokeResult, SpringBootModuleInfo springBootModuleInfo) {
        final SpringBootApplicationImpl springBootApplication = springBootModuleInfo.getSpringBootApplication();
        final boolean setEEContextOnStartup = springBootApplication.setEEContextOnStartup();
        final Method main;

        SpringBootComponentMetaData springBootComponentMetaData = ((SpringModuleMetaData) springBootModuleInfo.getMetaData()).getComponentMetaData();

        ComponentMetaDataAccessorImpl accessor = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor();
        ComponentMetaData currentCmd = accessor.getComponentMetaData();

        ClassLoader newTccl = springBootModuleInfo.getThreadContextClassLoader();
        ClassLoader previousTccl = AccessController.doPrivileged((PrivilegedAction<ClassLoader>) () -> Thread.currentThread().getContextClassLoader());
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            Thread.currentThread().setContextClassLoader(newTccl);
            return null;
        });

        try {
            if (setEEContextOnStartup && currentCmd == null) {
                accessor.beginContext(springBootComponentMetaData);
            }
            springBootApplication.registerSpringConfigFactory();
            Class<?> springApplicationClass = springBootModuleInfo.getClassLoader().loadClass(springBootApplication.getSpringBootManifest().getSpringStartClass());
            main = springApplicationClass.getMethod("main", String[].class);
            // TODO not sure Spring Boot supports non-private main methods
            main.setAccessible(true);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            futureMonitor.setResult(mainInvokeResult, e);
            return;
        } finally {
            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                Thread.currentThread().setContextClassLoader(previousTccl);
                return null;
            });
            if (setEEContextOnStartup && currentCmd == null) {
                accessor.endContext();
            }
        }

        // Execute the main method asynchronously.
        // The mainInvokeResult is tracked to monitor completion
        executor.execute(() -> {
            ClassLoader execPreviousTccl = AccessController.doPrivileged((PrivilegedAction<ClassLoader>) () -> Thread.currentThread().getContextClassLoader());
            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                Thread.currentThread().setContextClassLoader(newTccl);
                return null;
            });
            try {
                if (setEEContextOnStartup && currentCmd == null) {
                    accessor.beginContext(springBootComponentMetaData);
                }
                // get the application args to pass from the springBootApplication
                String[] appArgs = libertyProcess.getArgs();
                if (appArgs.length == 0) {
                    appArgs = springBootApplication.getAppArgs().toArray(new String[0]);
                }
                main.invoke(null, new Object[] { appArgs });
                springBootApplication.getApplicationReadyLatch().countDown();
                futureMonitor.setResult(mainInvokeResult, true);
            } catch (InvocationTargetException e) {
                Throwable target = e.getTargetException();
                String msgKey = null;
                if (target instanceof ApplicationError) {
                    msgKey = ((ApplicationError) target).getType().getMessageKey();
                    Tr.error(tc, msgKey);
                    futureMonitor.setResult(mainInvokeResult, target);
                } else {
                    futureMonitor.setResult(mainInvokeResult, e.getTargetException());
                }
            } catch (IllegalAccessException | IllegalArgumentException e) {
                // Auto FFDC here this should not happen
                futureMonitor.setResult(mainInvokeResult, e);
            } finally {
                AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                    Thread.currentThread().setContextClassLoader(execPreviousTccl);
                    return null;
                });
                if (setEEContextOnStartup && currentCmd == null) {
                    accessor.endContext();
                }
            }
        });
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.container.service.app.deploy.extended.ModuleRuntimeContainer#stopModule(com.ibm.ws.container.service.app.deploy.extended.ExtendedModuleInfo)
     */
    @Override
    public void stopModule(ExtendedModuleInfo moduleInfo) {
        SpringBootModuleInfo springBootModuleInfo = (SpringBootModuleInfo) moduleInfo;
        SpringBootComponentMetaData springBootComponentMetaData = ((SpringModuleMetaData) springBootModuleInfo.getMetaData()).getComponentMetaData();
        components.remove(springBootComponentMetaData.getPersistentIdentifier());
        springBootModuleInfo.getSpringBootApplication().unregisterSpringConfigFactory();
        springBootModuleInfo.getSpringBootApplication().callShutdownHooks();
        springBootModuleInfo.destroyThreadContextClassLoader();
    }

    @Override
    public ComponentMetaData createComponentMetaData(String identifier) {
        ComponentMetaData cmd = components.get(identifier);
        if (cmd == null) {
            throw new IllegalStateException("Could not find ComponentMetaData");
        }
        return cmd;
    }

    @Override
    public void initialize(ComponentMetaData metadata) throws IllegalStateException {
        // nothing to do
    }

    @Override
    public String getMetaDataIdentifier(String appName, String moduleName, String componentName) {
        return appName + "#" + moduleName + "#" + componentName;
    }

    @Override
    public ClassLoader getClassLoader(ComponentMetaData metadata) {
        return ((SpringBootComponentMetaData) metadata).getClassLoader();
    }
}
