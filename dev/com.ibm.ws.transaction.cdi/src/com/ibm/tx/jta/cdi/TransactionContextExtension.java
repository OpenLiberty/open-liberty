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
package com.ibm.tx.jta.cdi;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.cdi.extension.WebSphereCDIExtension;

/**
 *
 */
@Component(service = WebSphereCDIExtension.class,
           property = { "bean.defining.annotations=javax.transaction.TransactionScoped" })
public class TransactionContextExtension implements Extension, WebSphereCDIExtension {

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery event, BeanManager manager) {
        TransactionContext tc = new TransactionContext();
        com.ibm.tx.jta.impl.TransactionImpl.registerTransactionScopeDestroyer(tc);
        event.addContext(new TransactionContext());
    }
}