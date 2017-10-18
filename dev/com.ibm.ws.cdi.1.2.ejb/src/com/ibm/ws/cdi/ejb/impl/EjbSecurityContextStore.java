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
package com.ibm.ws.cdi.ejb.impl;

import java.security.Principal;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicReference;

import javax.ejb.EJBContext;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.interfaces.SecurityContextStore;

/**
 * Stores EJB context information for real-time extraction
 * of state data.
 * 
 */
@Component(
                name = "com.ibm.ws.cdi.EJBSecurityContextStore",
                immediate = true,
                property = { "service.vendor=IBM", "service.ranking:Integer=99" })
public class EjbSecurityContextStore implements SecurityContextStore {

    private static final TraceComponent tc = Tr.register(EjbSecurityContextStore.class);

    private static final AtomicReference<EjbSecurityContextStore> INSTANCE = new AtomicReference<EjbSecurityContextStore>();

    /**
     * DS method to activate this component.
     * 
     * @param compcontext
     */
    protected void activate(ComponentContext compcontext) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Activating " + this.getClass().getName());
        }
        INSTANCE.set(this);
    }

    /**
     * DS method to deactivate this component.
     * 
     * @param compcontext
     */
    protected void deactivate(ComponentContext compcontext) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Deactivating " + this.getClass().getName());
        }
        // Clear this as the active instance
        INSTANCE.compareAndSet(this, null);
    }

    /**
     * @return the active instance; may be null between deactivate and activate.
     */
    public static EjbSecurityContextStore getCurrentInstance() {
        return INSTANCE.get();
    }

    /**
     * If an EJB is a part of a BDA, a thread local EJBContext will be stored.
     */
    private final ThreadLocal<Stack<EJBContext>> ejbContexts = new ThreadLocal<Stack<EJBContext>>();

    /**
     * 
     * @return Principal from EJBContext or null
     */
    @Override
    public Principal getCurrentPrincipal() {
        Stack<EJBContext> ejbContextStack = getStackOfEjbContexts();

        if (ejbContextStack.isEmpty()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "getCurrentPrincipal: no ejbContexts in stack, returning null");
            }
            return null;
        }
        EJBContext currentContext = ejbContextStack.peek();

        if (currentContext != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                if (currentContext.getCallerPrincipal() == null) {
                    Tr.debug(tc, "getCurrentPrincipal: null");
                } else {
                    Tr.debug(tc, "getCurrentPrincipal: " + currentContext.getCallerPrincipal().getName());
                }
            }
            return currentContext.getCallerPrincipal();
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getCurrentPrincipal: null context");
        }
        return null;
    }

    public void storeEJBContext(EJBContext ejb) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "storeEJBContext: " + ejb);
        }
        Stack<EJBContext> stack = getStackOfEjbContexts();
        stack.push(ejb);
    }

    public void removeEJBContext() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "removeEJBContext: " + ejbContexts.get());
        }

        Stack<EJBContext> stack = getStackOfEjbContexts();
        stack.pop();

        //If the stack is empty, unset the thread local so we don't leak
        if (stack.isEmpty()) {
            ejbContexts.remove();
        }
    }

    private Stack<EJBContext> getStackOfEjbContexts() {
        Stack<EJBContext> stackejb = ejbContexts.get();
        if (null == stackejb) {
            stackejb = new Stack<EJBContext>();
            ejbContexts.set(stackejb);
        }
        return stackejb;
    }

}
