/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jbatch.rest.bridge;

import java.util.Collections;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.classloader.context.ClassLoaderThreadContextFactory;
import com.ibm.ws.classloading.ClassLoaderIdentifierService;
import com.ibm.ws.container.service.metadata.extended.MetaDataIdentifierService;
import com.ibm.ws.javaee.metadata.context.JEEMetadataThreadContextFactory;
import com.ibm.ws.jbatch.rest.utils.StringUtils;
import com.ibm.wsspi.threadcontext.ThreadContext;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;
import com.ibm.wsspi.threadcontext.WSContextService;

/**
 * Retrieves the ComponentMetaData and classloader context for a given
 * app and module name (only works for standalone WARs).
 * 
 */
@Component(service = AppModuleContextService.class, configurationPolicy = ConfigurationPolicy.IGNORE)
public class AppModuleContextService {

    /**
     * For building the CMD for an app/module.
     */
    private MetaDataIdentifierService metaDataIdentifierService;
    
    /**
     * For building the CMD for an app/module.
     */
    private ClassLoaderIdentifierService classLoaderIdentifierService;
    
    /**
     * For capturing thread context.
     */
    private WSContextService contextService;
    
    /**
     * For creating JEE MetaData ThreadContext
     */
    private JEEMetadataThreadContextFactory jeeMetaDataContextProvider;
    
    /**
     * For creating context classloader ThreadContext
     */
    private ClassLoaderThreadContextFactory classLoaderContextProvider;
    
    /**
     * The collection of contexts to capture under createThreadContext.
     * Classloader, JeeMetadata, and security.
     */
    @SuppressWarnings("unchecked")
    private static final Map<String, ?>[] CapturedContexts = new Map[] {
           Collections.singletonMap(WSContextService.THREAD_CONTEXT_PROVIDER, "com.ibm.ws.classloader.context.provider"),
           Collections.singletonMap(WSContextService.THREAD_CONTEXT_PROVIDER, "com.ibm.ws.javaee.metadata.context.provider"),
           Collections.singletonMap(WSContextService.THREAD_CONTEXT_PROVIDER, "com.ibm.ws.security.context.provider"),
    };
    
    /**
     * DS injection
     */
    @Reference
    protected void setMetaDataIdentifierService(MetaDataIdentifierService metaDataIdentifierService) {
        this.metaDataIdentifierService = metaDataIdentifierService;
    }
    
    /**
     * DS injection
     */
    @Reference
    protected void setClassLoaderIdentifierService(ClassLoaderIdentifierService classLoaderIdentifierService) {
        this.classLoaderIdentifierService = classLoaderIdentifierService;
    }
    
    /**
     * DS injection.
     */
    @Reference(target = "(service.pid=com.ibm.ws.context.manager)")
    protected void setContextService(WSContextService contextService) {
        this.contextService = contextService;
    }
    
    /**
     * DS injection. 
     */
    @Reference
    protected void setJeeMetaDataContextProvider(JEEMetadataThreadContextFactory jeeMetaDataContextProvider) {
        this.jeeMetaDataContextProvider = jeeMetaDataContextProvider;
    }
    
    /**
     * DS injection. 
     */
    @Reference
    protected void setClassLoaderContextProvider(ClassLoaderThreadContextFactory classLoaderContextProvider) {
        this.classLoaderContextProvider = classLoaderContextProvider;
    }

    /**
     * @return "WEB" if comp name is empty; otherwise "EJB".
     */
    protected String getComponentType(J2EEName j2eeName) {
        return StringUtils.isEmpty( j2eeName.getComponent() ) ? "WEB" : "EJB";
    }
    
    /**
     * 
     * @param j2eeName the app / module / comp name of the target component
     * 
     * @return the metadata id for the given app/module/comp
     */
    protected String getMetaDataIdentifier(J2EEName j2eeName) {
        return metaDataIdentifierService.getMetaDataIdentifier(getComponentType(j2eeName), 
                                                               j2eeName.getApplication(), 
                                                               j2eeName.getModule(),
                                                               j2eeName.getComponent());
    }
    
    /**
     * 
     * @param j2eeName the app / module / comp name of the target component
     * 
     * @return the context classloader id for the given app/module/comp
     * 
     * @throws IllegalStateException if the classloader id is not null (app not installed)
     */
    protected String getContextClassLoaderIdentifier(J2EEName j2eeName) {
        String retMe = classLoaderIdentifierService.getClassLoaderIdentifier(getComponentType(j2eeName), 
                                                                             j2eeName.getApplication(), 
                                                                             j2eeName.getModule(),
                                                                             j2eeName.getComponent());
        if (StringUtils.isEmpty(retMe)) {
            throw new IllegalStateException("ClassLoader ID is null for application component " + j2eeName + "."
                                            + " Verify the application component is installed.");
        } else {
            return retMe;
        }
    }
    
    /**
     * Capture the current security context and create the JeeMetadata and Classloader context for
     * the given app component (j2eeName).
     * 
     * @return a ThreadContextDescriptor containing the current security context and the jeemetadata
     *         and classloader context for the given app component (j2eeName).
     *         
     * @throws IllegalStateException if the app component is not installed
     */
    protected ThreadContextDescriptor createThreadContext( Map<String, String> execProps, J2EEName j2eeName ) {
        
        ThreadContext classloaderContext = classLoaderContextProvider.createThreadContext( execProps, getContextClassLoaderIdentifier(j2eeName) );
        ThreadContext jeeContext = jeeMetaDataContextProvider.createThreadContext( execProps, getMetaDataIdentifier(j2eeName) );
        
        ThreadContextDescriptor retMe = contextService.captureThreadContext(execProps, CapturedContexts);
        retMe.set("com.ibm.ws.javaee.metadata.context.provider", jeeContext);
        retMe.set("com.ibm.ws.classloader.context.provider", classloaderContext);

        return retMe;
    }
    
    /**
     * @param execProps passed to WSContextService.captureThreadContext
     * @param j2eeName identifies the app component
     * @param instance to be wrapped with the context proxy
     * @param intf the interfaces implemented by instance (and the returned proxy)
     * 
     * @return a contextual proxy that will execute under the current security context along with
     *         the jee-metadata and classloader context of the given app component (j2eeName).
     *         
     * @throws IllegalStateException if the app component is not installed
     */
    public <T> T createContextualProxy(Map<String, String> execProps, 
                                       J2EEName j2eeName, 
                                       T instance, 
                                       Class<T> intf) {
        
        ThreadContextDescriptor tcd = createThreadContext(execProps, j2eeName);
        return  contextService.createContextualProxy(tcd, instance, intf);
    }
}
