/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.osgi.collaborator;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.webcontainer.collaborator.CollaboratorService;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.webcontainer.collaborator.IConnectionCollaborator;
import com.ibm.wsspi.webcontainer.collaborator.IWebAppSecurityCollaborator;
import com.ibm.wsspi.webcontainer.collaborator.IWebAppTransactionCollaborator;
import com.ibm.wsspi.webcontainer.collaborator.WebAppInitializationCollaborator;
import com.ibm.wsspi.webcontainer.collaborator.WebAppInvocationCollaborator;

/**
 * Helper class to encapsulate collaborator dependencies, as they are optional
 */
public class CollaboratorServiceImpl implements CollaboratorService {
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
    }

    protected void deactivate(ComponentContext context) {
        // Clear this as the active instance
        instance.compareAndSet(this, null);
    }

    /*
     * Transaction collaborator methods
     */
    public void setWebAppTransactionCollaborator(IWebAppTransactionCollaborator watc) {
        watcService = watc;
    }

    public void unsetWebAppTransactionCollaborator(IWebAppTransactionCollaborator watc) {
        if (watc == watcService) {
            watcService = null;
        }
    }

    public static IWebAppTransactionCollaborator getWebAppTransactionCollaborator() {
        CollaboratorServiceImpl thisService = instance.get();

        if (thisService != null) {
            return thisService.watcService;
        }

        return null;
    }

    /*
     * Security collaborator methods
     */
    public void setWebAppSecurityCollaborator(ServiceReference<IWebAppSecurityCollaborator> ref) {
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
        // check that this service is in the map
        String securityType = getSecurityType(ref);
        webAppSecurityCollaborators.removeReference(securityType, ref);
    }

    public static IWebAppSecurityCollaborator getWebAppSecurityCollaborator(String securityType) {
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
        ccServ = cc;
    }

    public void unsetWebAppConnectionCollaborator(IConnectionCollaborator cc) {
        if (ccServ == cc) {
            ccServ = null;
        }
    }

    public static IConnectionCollaborator getWebAppConnectionCollaborator() {
        CollaboratorServiceImpl thisService = instance.get();

        if (thisService != null) {
            return thisService.ccServ;
        }

        return null;
    }

    /*
     * Generic collaborator methods
     */
    public void setWebAppInitializationCollaborator(WebAppInitializationCollaborator waic) {
        webAppInitializationCollaborator.add(waic);
    }

    public void unsetWebAppInitializationCollaborator(WebAppInitializationCollaborator waic) {
        webAppInitializationCollaborator.remove(waic);
    }

    public static Set<WebAppInitializationCollaborator> getWebAppInitializationCollaborator() {
        CollaboratorServiceImpl thisService = instance.get();

        if (thisService != null)
            return Collections.unmodifiableSet(thisService.webAppInitializationCollaborator);

        return null;
    }

    public void setWebAppInvocationCollaborator(WebAppInvocationCollaborator waic) {
        webAppInvocationCollaborators.add(waic);
    }

    public void unsetWebAppInvocationCollaborator(WebAppInvocationCollaborator waic) {
        webAppInvocationCollaborators.remove(waic);
    }

    public static Set<WebAppInvocationCollaborator> getWebAppInvocationCollaborators() {
        CollaboratorServiceImpl thisService = instance.get();

        if (thisService != null)
            return Collections.unmodifiableSet(thisService.webAppInvocationCollaborators);

        return null;
    }
}
