/*******************************************************************************
 * Copyright (c) 2019,2020 IBM Corporation and others.
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
package com.ibm.ws.jaxrs21.clientconfig;

import java.util.concurrent.ExecutorService;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.ws.threading.CompletionStageFactory;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE, service = { JAXRSClientCompletionStageFactoryConfig.class }, immediate = true, property = { "service.vendor=IBM" })
public class JAXRSClientCompletionStageFactoryConfig {
    
    private static final AtomicServiceReference<ExecutorService> executorServiceRef = new AtomicServiceReference<ExecutorService>("executorService");
    private static final AtomicServiceReference<ExecutorService> managedExecutorServiceRef = new AtomicServiceReference<ExecutorService>("managedExecutorService");

    private static volatile CompletionStageFactory completionStageFactory = null;

    @Activate
    protected void activate(ComponentContext cc) {
        executorServiceRef.activate(cc);
        managedExecutorServiceRef.activate(cc);
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        executorServiceRef.deactivate(cc);
        managedExecutorServiceRef.deactivate(cc);
    }

    
    @Reference(name = "completionStageFactory", service = CompletionStageFactory.class)
    protected void setCompletionStageFactory(CompletionStageFactory completionStageFactory) {        
        JAXRSClientCompletionStageFactoryConfig.completionStageFactory = completionStageFactory;
    }
    
    protected void unsetCompletionStageFactory(CompletionStageFactory completionStageFactory) {        
        JAXRSClientCompletionStageFactoryConfig.completionStageFactory = null;
    }

    public static CompletionStageFactory getCompletionStageFactory() {
        return completionStageFactory;
    }
    
    //Jim start
    @Reference(name = "executorService", service = ExecutorService.class, target = "(component.name=com.ibm.ws.threading)")
    protected void setExecutorService(ServiceReference<ExecutorService> ref) {
        System.out.println("Jim... CompletionStageFactory.setExecutorService:  ref = " + ref);
        executorServiceRef.setReference(ref);

    }

    protected void unsetExecutorService(ServiceReference<ExecutorService> ref) {
        System.out.println("Jim... CompletionStageFactory.unsetExecutorService");
        executorServiceRef.unsetReference(ref);
    }

    @Reference(name = "managedExecutorService", service = ExecutorService.class, target = "(id=DefaultManagedExecutorService)", policyOption = ReferencePolicyOption.GREEDY,
               cardinality = ReferenceCardinality.OPTIONAL)
    protected void setManagedExecutorService(ServiceReference<ExecutorService> ref) {
        System.out.println("Jim... CompletionStageFactory.setManagedExecutorService: ref = " + ref);
        managedExecutorServiceRef.setReference(ref);
    }

    protected void unsetManagedExecutorService(ServiceReference<ExecutorService> ref) {
        System.out.println("Jim... CompletionStageFactory.unsetManagedExecutorService");
        managedExecutorServiceRef.unsetReference(ref);
    }

    public static ExecutorService getExecutorService() {
        ExecutorService managedExecutorService = managedExecutorServiceRef.getService();
        if (managedExecutorService != null) {
            System.out.println("Jim... CompletionStageFactory.getExecutorService returning managedExectorService: " + managedExecutorService.getClass().getName());
            return managedExecutorService;
        } 
        System.out.println("Jim... CompletionStageFactory.getExecutorService returning exectorServiceRef: " + executorServiceRef.getServiceWithException().getClass().getName());
        return executorServiceRef.getService();
    }

    //Jim end

}
