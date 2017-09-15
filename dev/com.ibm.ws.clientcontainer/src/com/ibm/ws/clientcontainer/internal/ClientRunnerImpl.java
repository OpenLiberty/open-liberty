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
package com.ibm.ws.clientcontainer.internal;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.util.List;

import javax.security.auth.callback.CallbackHandler;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.clientcontainer.metadata.ClientModuleMetaData;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.javaee.dd.client.ApplicationClient;
import com.ibm.ws.javaee.dd.common.LifecycleCallback;
import com.ibm.ws.kernel.launch.service.ClientRunner;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.injectionengine.InjectionException;

public class ClientRunnerImpl implements ClientRunner {
    private final static TraceComponent tc = Tr.register(ClientRunnerImpl.class, "clientContainer", "com.ibm.ws.clientcontainer.resources.Messages");
    //default is 30 seconds 
    private final static int MAX_WAIT_TO_LAUNCH = Integer.getInteger("com.ibm.ws.clientcontainer.maxLaunchWaitTime", 30000);

    private boolean readyToRun;
    private boolean initFailed;
    private String[] args; // Contains the arguments passed to the runtime on the command line
    private Class<?> mainClass;
    public Exception invokeException;
    private ComponentMetaData cmd;
    private ClassLoader classLoader;
    private ClientModuleInjection cmi;

    ClientRunnerImpl() {}

    synchronized void readyToRun(ClientModuleInjection cmi, String[] args, ComponentMetaData cmd, ClassLoader classLoader) throws ClassNotFoundException {
        this.cmi = cmi;
        this.mainClass = cmi.getMainClass();
        this.args = args.clone();
        this.cmd = cmd;
        this.classLoader = classLoader;

        this.readyToRun = true;
        this.notify();
    }

    synchronized void setupFailure() {
        initFailed = true;
        this.notify();
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void run() {
        if (initFailed) {
            throw new IllegalStateException("Client module main failed initialization");
        }
        if (!readyToRun) {
            try {
                this.wait(MAX_WAIT_TO_LAUNCH);
            } catch (InterruptedException e) {
                // Auto-FFDC
            } finally {
                if (initFailed) {
                    throw new IllegalStateException("Client module main failed initialization");
                }
                if (!readyToRun) {
                    throw new IllegalStateException("Failed to initialize client prior to attempted invocation.");
                }
            }
        }

        Method mainMethod = null;
        Object appParms[] = { getUserAppArgs() };

        ClassLoader origLoader = AccessController.doPrivileged(new ClientModuleRuntimeContainer.GetTCCL());
        ClassLoader newLoader = AccessController.doPrivileged(new ClientModuleRuntimeContainer.SetTCCL(classLoader));
        // Launch static main method of the client application.
        try {
            ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().beginContext(cmd);

            Class<String[]> parms = String[].class;
            mainMethod = mainClass.getMethod("main", parms);
            processPostConstruct();
            mainMethod.invoke(null, appParms);
            processPreDestroy();
        } catch (Exception e) {
            invokeException = e;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception occurred invoking main method", e);
            }
        } finally {
            if (origLoader != newLoader) {
                AccessController.doPrivileged(new ClientModuleRuntimeContainer.SetTCCL(origLoader));
            }
            ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().endContext();
        }
    }

    private Object getUserAppArgs() {
        return args;
    }

    /**
     * Process PostContruct annotation or metadata for Main class or Login Callback class.
     */
    @FFDCIgnore(InjectionException.class)
    private void processPostConstruct() {
        ApplicationClient appClient = ((ClientModuleMetaData) cmd.getModuleMetaData()).getAppClient();
        boolean isMetadataComplete = appClient.isMetadataComplete();
        LifecycleCallbackHelper helper = new LifecycleCallbackHelper(isMetadataComplete);
        List<LifecycleCallback> postConstruct = appClient.getPostConstruct();
        CallbackHandler loginCallbackHandler = cmi.getCallbackHandler();

        try {
            if (loginCallbackHandler != null) {
                helper.doPostConstruct(loginCallbackHandler, postConstruct);
            }
            helper.doPostConstruct(mainClass, postConstruct);
        } catch (InjectionException e) {
            Tr.error(tc, "INJECTION_POSTCONSTRUCT_CWWKC2452E", new Object[] { e.getLocalizedMessage() });
        }
    }

    /**
     * Process PreDestroy annotation or metadata for Login Callback class.
     */
    @FFDCIgnore(InjectionException.class)
    private void processPreDestroy() {
        ApplicationClient appClient = ((ClientModuleMetaData) cmd.getModuleMetaData()).getAppClient();
        boolean isMetadataComplete = appClient.isMetadataComplete();
        LifecycleCallbackHelper helper = new LifecycleCallbackHelper(isMetadataComplete);
        List<LifecycleCallback> preDestroy = appClient.getPreDestroy();
        CallbackHandler loginCallbackHandler = cmi.getCallbackHandler();

        try {
            if (loginCallbackHandler != null) {
                helper.doPreDestroy(loginCallbackHandler, preDestroy);
            }
        } catch (InjectionException e) {
            Tr.error(tc, "INJECTION_PREDESTROY_CWWKC2453E", new Object[] { e.getLocalizedMessage() });
        }
    }

}
