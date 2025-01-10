/*
 * Copyright (c) 2010, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.transaction.services;

import static com.ibm.tx.jta.TransactionManagerFactory.getTransactionManager;
import static com.ibm.ws.tx.jta.embeddable.EmbeddableTransactionSynchronizationRegistryFactory.getTransactionSynchronizationRegistry;
import static com.ibm.ws.uow.embeddable.UOWManagerFactory.getUOWManager;
import static java.security.AccessController.doPrivileged;

import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.naming.InvalidNameException;
import javax.naming.NameClassPair;
import javax.naming.NamingException;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.tx.jta.embeddable.UserTransactionDecorator;
import com.ibm.tx.jta.embeddable.impl.EmbeddableUserTransactionImpl;
import com.ibm.ws.container.service.naming.JavaColonNamingHelper;
import com.ibm.ws.container.service.naming.NamingConstants.JavaColonNamespace;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereUserTransaction;
import com.ibm.ws.uow.embeddable.UOWManager;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * This class allows a
 * UserTransaction to be looked up on a jndi lookup.
 */
public class TransactionJavaColonHelper implements JavaColonNamingHelper {

    private ServiceReference<EmbeddableWebSphereUserTransaction> userTranSvcRef;
    private final AtomicServiceReference<UserTransactionDecorator> userTranDecoratorSvcRef = new AtomicServiceReference<UserTransactionDecorator>("userTransactionDecorator");

    /**
     * DS method to activate this component.
     *
     * @param cContext DeclarativeService defined/populated component context
     */
    protected void activate(ComponentContext cContext) {
        userTranDecoratorSvcRef.activate(cContext);
    }

    /**
     * DS method to deactivate this component.
     *
     * @param reason int representation of reason the component is stopping
     */
    protected void deactivate(int reason, ComponentContext cContext) {
        userTranDecoratorSvcRef.deactivate(cContext);
    }

    /*
     * Injection methods called by SCR
     * Use indirect forms (ie Service Reference vs UserTransaction reference) to
     * allow lazy activation of UserTransaction only when a lookup is performed.
     */
    protected void setUserTransaction(ServiceReference<EmbeddableWebSphereUserTransaction> utSR) {
        userTranSvcRef = utSR;
    }

    protected void unsetUserTransaction(ServiceReference<EmbeddableWebSphereUserTransaction> utSR) {
        if (utSR == userTranSvcRef) {
            userTranSvcRef = null;
        }
    }

    protected void setUserTransactionDecorator(ServiceReference<UserTransactionDecorator> utdsr) {
        userTranDecoratorSvcRef.setReference(utdsr);
    }

    protected UserTransactionDecorator getUserTransactionDecorator() {
        return userTranDecoratorSvcRef.getService();
    }

    protected void unsetUserTransactionDecorator(ServiceReference<UserTransactionDecorator> utdsr) {
        userTranDecoratorSvcRef.unsetReference(utdsr);
    }

    /** {@inheritDoc} */
    @Override
    public Object getObjectInstance(JavaColonNamespace namespace, String name) throws NamingException {
        switch (namespace) {
        case COMP:
            switch (name) {
            case "TransactionManager": return getTransactionManager();
            case "TransactionSynchronizationRegistry": return getTransactionSynchronizationRegistry();
            // If we have a service reference we know it's safe to return a reference
            case "UserTransaction": return userTranSvcRef == null ? null : getUserTransaction(false, null);
            default: return null;
            }
        case COMP_WS:
            switch (name) {
            case "UOWManager": return getUOWManager();
            default: return null;
            }
        default: return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasObjectWithPrefix(JavaColonNamespace namespace, String name) throws NamingException {
        if (name == null) {
            throw new InvalidNameException();
        }
        boolean result = false;
        if ((namespace == JavaColonNamespace.COMP || namespace == JavaColonNamespace.COMP_WS) && name.isEmpty()) {
            result = true;
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public Collection<? extends NameClassPair> listInstances(JavaColonNamespace namespace, String nameInContext) {

        if (JavaColonNamespace.COMP.equals(namespace) && "".equals(nameInContext)) {
            ArrayList<NameClassPair> retVal = new ArrayList<>();
            if (userTranSvcRef != null) {
                NameClassPair pair = new NameClassPair(nameInContext, EmbeddableUserTransactionImpl.class.getName());
                retVal.add(pair);
            }

            retVal.add(new NameClassPair(nameInContext, TransactionSynchronizationRegistry.class.getName()));
            retVal.add(new NameClassPair(nameInContext, UOWManager.class.getName()));

            return retVal;
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Helper method for use with injection TransactionObjectFactoruy.
     *
     * @param injection if the UserTransaction is being obtained for injection
     * @param injectionContext the injection target context if injection is true, or null if unspecified
     * @return UserTransaction object with decorator applied if present
     * @throws NamingException if the decorator determines the UserTransaction is not available
     */
    protected UserTransaction getUserTransaction(boolean injection, Object injectionContext) throws NamingException {
        final UserTransaction ut = doPrivileged((PrivilegedAction<UserTransaction>) () -> userTranSvcRef.getBundle().getBundleContext().getService(userTranSvcRef));
        final UserTransactionDecorator utd = getUserTransactionDecorator();
        if (utd == null) {
            return ut;
        } else {
            return utd.decorateUserTransaction(ut, injection, injectionContext);
        }
    }
}

