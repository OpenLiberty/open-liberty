/*******************************************************************************
 * Copyright (c) 2011, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.webcontainer.osgi.collaborator;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.webcontainer.collaborator.CollaboratorService;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.webcontainer.collaborator.IConnectionCollaborator;
import com.ibm.wsspi.webcontainer.collaborator.IWebAppSecurityCollaborator;
import com.ibm.wsspi.webcontainer.collaborator.IWebAppTransactionCollaborator;
import com.ibm.wsspi.webcontainer.collaborator.WebAppInitializationCollaborator;
import com.ibm.wsspi.webcontainer.collaborator.WebAppInvocationCollaborator;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;

/**
 * Helper class to encapsulate collaborator dependencies, as they are optional
 */
public class CollaboratorServiceImpl implements CollaboratorService {
    private static final String CLASS_NAME = CollaboratorServiceImpl.class.getName();
    protected static final Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer.osgi.collaborator");

    private static final String DEFAULT_SECURITY_TYPE = "default";
    private static final String WEBAPP_SECURITY_COLLABORATOR = "webAppSecurityCollaborator";

    /**
     * Property to look for on the service that provides IWebAppSecurityColaboroator
     * in order to determine whether to call the admin or app security collaborator.
     */
    private static final String SECURITY_TYPE = "com.ibm.ws.security.type";

    /**
     * Active CollaboratorService instance. May be null between deactivate and activate
     * calls.
     */
    private static final AtomicReference<CollaboratorServiceImpl> instance = new AtomicReference<CollaboratorServiceImpl>();

    private IWebAppTransactionCollaborator watcService;

    /** Use the ConcurrentServiceReferenceMap class to manage set/unset/locate/cache of the security collaborator service */
    private final ConcurrentServiceReferenceMap<String, IWebAppSecurityCollaborator> webAppSecurityCollaborators = new ConcurrentServiceReferenceMap<String, IWebAppSecurityCollaborator>(WEBAPP_SECURITY_COLLABORATOR);

    private IConnectionCollaborator ccServ;

    /**
     * Allocate final variable containing set for webapp invocation collaborators:
     * set/unset of collaborators can occur before activate.
     */
    private final Set<WebAppInvocationCollaborator> webAppInvocationCollaborators = new CopyOnWriteArraySet<WebAppInvocationCollaborator>();

    private final Set<WebAppInitializationCollaborator> webAppInitializationCollaborator = new CopyOnWriteArraySet<WebAppInitializationCollaborator>();

    protected void activate(ComponentContext context) {
        // Set this as the active CollaboratorService instance
        webAppSecurityCollaborators.activate(context);
        instance.set(this);
        
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "activate", " context [" + context + "] , this [" + this + "]");
        }
    }

    protected void deactivate(ComponentContext context) {
        
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "deactivate", " context [" +context + "] , this [" + this + "]");
        }
        
        // Clear this as the active instance
        instance.compareAndSet(this, null);
    }

    /*
     * Transaction collaborator methods
     */
    public void setWebAppTransactionCollaborator(IWebAppTransactionCollaborator watc) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "setWebAppTransactionCollaborator", " IWebAppTransactionCollaborator [" + watc + "] , " + this);
        }
        
        watcService = watc;
    }

    public void unsetWebAppTransactionCollaborator(IWebAppTransactionCollaborator watc) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "unsetWebAppTransactionCollaborator", " this [" + this + "]");
        }
        
        if (watc == watcService) {
            watcService = null;
        }
    }

    public static IWebAppTransactionCollaborator getWebAppTransactionCollaborator() {
        CollaboratorServiceImpl thisService = instance.get();
        
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "getWebAppTransactionCollaborator", " thisService [" + thisService + "]");
        }

        if (thisService != null) {
            return thisService.watcService;
        }

        return null;
    }

    /*
     * Security collaborator methods
     */
    public void setWebAppSecurityCollaborator(ServiceReference<IWebAppSecurityCollaborator> ref) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "setWebAppSecurityCollaborator", " this [" + this + "]");
        }
        
        String securityType = getSecurityType(ref);

        webAppSecurityCollaborators.putReference(securityType, ref);
    }

    /**
     * @param ref
     * @return
     */
    private String getSecurityType(ServiceReference<IWebAppSecurityCollaborator> ref) {
        Object secTypeObj = ref.getProperty(SECURITY_TYPE);
        String securityType = null;
        if (secTypeObj instanceof String)
            securityType = (String) secTypeObj;
        if (securityType == null || securityType.length() == 0) {
            securityType = DEFAULT_SECURITY_TYPE;
        }
        return securityType;
    }

    public void unsetWebAppSecurityCollaborator(ServiceReference<IWebAppSecurityCollaborator> ref) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "unsetWebAppSecurityCollaborator", " this [" + this + "]");
        }
        
        // check that this service is in the map
        String securityType = getSecurityType(ref);
        webAppSecurityCollaborators.removeReference(securityType, ref);
    }

    public static IWebAppSecurityCollaborator getWebAppSecurityCollaborator(String securityType) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "getWebAppSecurityCollaborator", " securityType [" + securityType + "]");
        }
        
        IWebAppSecurityCollaborator collab = null;
        CollaboratorServiceImpl thisService = instance.get();

        if (securityType == null)
            securityType = DEFAULT_SECURITY_TYPE;
        if (!securityType.equals(DEFAULT_SECURITY_TYPE) && 
            !securityType.equals("com.ibm.ws.management"))
            securityType = "com.ibm.ws.feature";
        if (thisService != null) {
            collab = thisService.webAppSecurityCollaborators.getService(securityType);
        }

        return collab;
    }

    /*
     * Connection manager collaborator methods
     */
    public void setWebAppConnectionCollaborator(IConnectionCollaborator cc) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "setWebAppConnectionCollaborator", " IConnectionCollaborator [" + cc + "] , " + this);
        }
        
        ccServ = cc;
    }

    public void unsetWebAppConnectionCollaborator(IConnectionCollaborator cc) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "unsetWebAppConnectionCollaborator", " IConnectionCollaborator [" + cc + "] , " + this);
        }
        
        if (ccServ == cc) {
            ccServ = null;
        }
    }

    public static IConnectionCollaborator getWebAppConnectionCollaborator() {
        CollaboratorServiceImpl thisService = instance.get();

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "getWebAppConnectionCollaborator", " thisService [" + thisService + "]");
        }

        if (thisService != null) {
            return thisService.ccServ;
        }

        return null;
    }

    /*
     * Generic collaborator methods
     */
    public void setWebAppInitializationCollaborator(WebAppInitializationCollaborator waic) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "setWebAppInitializationCollaborator", " WebAppInitializationCollaborator [" + waic + "] , " + this);
        }

        webAppInitializationCollaborator.add(waic);
    }

    public void unsetWebAppInitializationCollaborator(WebAppInitializationCollaborator waic) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "unsetWebAppInitializationCollaborator", " WebAppInitializationCollaborator [" + waic + "] , " + this);
        }

        webAppInitializationCollaborator.remove(waic);
    }

    public static Set<WebAppInitializationCollaborator> getWebAppInitializationCollaborator() {
        CollaboratorServiceImpl thisService = instance.get();

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "getWebAppInitializationCollaborator", " thisService [" + thisService + "]");
        }

        if (thisService != null)
            return Collections.unmodifiableSet(thisService.webAppInitializationCollaborator);

        return null;
    }

    public void setWebAppInvocationCollaborator(WebAppInvocationCollaborator waic) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "setWebAppInvocationCollaborator", " WebAppInvocationCollaborator [" + waic + "] , " + this);
        }

        webAppInvocationCollaborators.add(waic);
    }

    public void unsetWebAppInvocationCollaborator(WebAppInvocationCollaborator waic) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "unsetWebAppInvocationCollaborator", " WebAppInvocationCollaborator [" + waic + "] , " + this);
        }

        webAppInvocationCollaborators.remove(waic);
    }

    public static Set<WebAppInvocationCollaborator> getWebAppInvocationCollaborators() {
        CollaboratorServiceImpl thisService = instance.get();

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "getWebAppInvocationCollaborators", " thisService [" + thisService + "]");
        }

        if (thisService != null)
            return Collections.unmodifiableSet(thisService.webAppInvocationCollaborators);

        return null;
    }
}
