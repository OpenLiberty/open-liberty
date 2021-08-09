/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.osgi.session;

import com.ibm.ws.webcontainer.SessionRegistry;
import com.ibm.ws.webcontainer.httpsession.SessionManager;
import com.ibm.ws.webcontainer.session.impl.SessionContextRegistryImpl;
import com.ibm.ws.webcontainer.session.impl.SessionContextRegistryImplFactory;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

/**
 * Helper class to encapsulate the session dependency
 */
@Component(service={SessionHelper.class}, property={"service.vendor=IBM"})
public class SessionHelper {

    private SessionManager manager = null;
    private SessionContextRegistryImpl registry = null;
    private SessionContextRegistryImplFactory sessionContextRegistryImplFactory;

    /**
     * @return SessionRegistry the current SessionRegistry, or null if one isn't found
     */
    public synchronized SessionRegistry getRegistry() {
        if (this.registry == null && this.manager != null) {
            this.registry = sessionContextRegistryImplFactory.createSessionContextRegistryImpl(this.manager);
            this.manager.start(this.registry);
        }
        return this.registry;
    }

    /**
     * @param ref the new SessionManager reference
     */
    @Reference(cardinality=ReferenceCardinality.MANDATORY, policy=ReferencePolicy.DYNAMIC)
    protected synchronized void setSessionManager(SessionManager ref) {
        /*-
         * Declarative Services reminder about dynamic required service x:
         *   1) DS will call setx(X1) before activate()
         *   2) If X1 is going to be deactivated, but X2 is available,
         *      then DS will call setx(X2) before unsetx(X1)
         *   3) This allows the dynamic required reference to remain satisfied.
         */
        this.manager = ref;
        this.registry = null; // assume session bundle handles app restart appropriately
    }

    /**
     * @param ref the old SessionManager reference
     */
    protected synchronized void unsetSessionManager(SessionManager ref) {
        if (ref == this.manager) {
            this.manager = null;
            this.registry = null; // assume session bundle handles app restart appropriately
        }
    }
    
    @Reference(cardinality=ReferenceCardinality.MANDATORY, policy=ReferencePolicy.DYNAMIC, policyOption=ReferencePolicyOption.GREEDY)
    protected synchronized void setSessionContextRegistryImplFactory(SessionContextRegistryImplFactory factory) {
        this.sessionContextRegistryImplFactory = factory;
    }
    
    protected synchronized void unsetSessionContextRegistryImplFactory(SessionContextRegistryImplFactory factory) {
        // no-op intended here to avoid SessionContextRegistryImplFactory being null when switching service implementations
    }
    
}
