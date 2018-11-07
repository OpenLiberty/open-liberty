/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.springboot.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.app.manager.springboot.container.ApplicationError;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedApplicationInfo;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedModuleInfo;
import com.ibm.ws.container.service.app.deploy.extended.ModuleRuntimeContainer;
import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.ws.kernel.LibertyProcess;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.MetaDataImpl;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.threading.FutureMonitor;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM", "type:String=spring" })
public class SpringBootRuntimeContainer implements ModuleRuntimeContainer {
    private static final TraceComponent tc = Tr.register(SpringBootRuntimeContainer.class);

    static class SpringModuleMetaData extends MetaDataImpl implements ModuleMetaData {
        private final SpringBootModuleInfo moduleInfo;

        /**
         * @param slotCnt
         */
        public SpringModuleMetaData(SpringBootModuleInfo moduleInfo) {
            super(0);
            this.moduleInfo = moduleInfo;
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
            return null;
        }

        @Override
        public J2EEName getJ2EEName() {
            return ((ExtendedApplicationInfo) moduleInfo.getApplicationInfo()).getMetaData().getJ2EEName();
        }

    }

    @Reference
    private ExecutorService executor;
    @Reference
    private FutureMonitor futureMonitor;

    @Reference
    private LibertyProcess libertyProcess;

    @Override
    public ModuleMetaData createModuleMetaData(ExtendedModuleInfo moduleInfo) throws MetaDataException {
        return new SpringModuleMetaData((SpringBootModuleInfo) moduleInfo);
    }

    @Override
    public Future<Boolean> startModule(ExtendedModuleInfo moduleInfo) throws StateChangeException {
        SpringBootModuleInfo springBootModuleInfo = (SpringBootModuleInfo) moduleInfo;
        Future<Boolean> result = futureMonitor.createFuture(Boolean.class);
        invokeSpringMain(result, springBootModuleInfo);
        return result;
    }

    private void invokeSpringMain(Future<Boolean> mainInvokeResult, SpringBootModuleInfo springBootModuleInfo) {
        final SpringBootApplicationImpl springBootApplication = springBootModuleInfo.getSpringBootApplication();
        final Method main;
        ClassLoader newTccl = springBootModuleInfo.getThreadContextClassLoader();
        ClassLoader previousTccl = AccessController.doPrivileged((PrivilegedAction<ClassLoader>) () -> Thread.currentThread().getContextClassLoader());
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            Thread.currentThread().setContextClassLoader(newTccl);
            return null;
        });
        try {
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
                // get the application args to pass from the springBootApplication
                String[] appArgs = libertyProcess.getArgs();
                if (appArgs.length == 0) {
                    appArgs = springBootApplication.getAppArgs().toArray(new String[0]);
                }
                main.invoke(null, new Object[] { appArgs });
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
        springBootModuleInfo.getSpringBootApplication().unregisterSpringConfigFactory();
        springBootModuleInfo.getSpringBootApplication().callShutdownHooks();
        springBootModuleInfo.destroyThreadContextClassLoader();
    }

}
