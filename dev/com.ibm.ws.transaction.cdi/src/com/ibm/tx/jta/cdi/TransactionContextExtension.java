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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;

import com.ibm.tx.TranConstants;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;
import com.ibm.ws.cdi.CDIService;
import com.ibm.ws.cdi.extension.WebSphereCDIExtension;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = WebSphereCDIExtension.class,
           property = { "bean.defining.annotations=javax.transaction.TransactionScoped" })
public class TransactionContextExtension implements Extension, WebSphereCDIExtension {

    private static final TraceComponent tc = Tr.register(TransactionContext.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    //This is not actually used since weld will create a new instance of this class seperate from the one OSGI has populated. 
    //But this stays so that OSGI will manage the extensions lifecycle. 
    @Reference
    private CDIService cdiSvc;

    private BeanManager beanManager = null;

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery event, BeanManager manager) {
        TransactionContext tc = new TransactionContext(getBeanManager());
        com.ibm.tx.jta.impl.TransactionImpl.registerTransactionScopeDestroyer(tc);
        event.addContext(tc);
    }

    private BeanManager getBeanManager() {
        if (beanManager == null) {
            CDIService cdiService = getCDIService();
            if (cdiService != null) {
                beanManager = cdiService.getCurrentBeanManager();
            } else {
                throw new IllegalStateException("Failed to get the beanManager.");
            }
        }
        return beanManager;
    }

    private CDIService getCDIService() {
        Bundle bundle = FrameworkUtil.getBundle(CDIService.class);
        CDIService cdiService = AccessController.doPrivileged((PrivilegedAction<CDIService>) () -> {
            BundleContext bCtx = bundle.getBundleContext();
            ServiceReference<CDIService> svcRef = bCtx.getServiceReference(CDIService.class);
            return svcRef == null ? null : bCtx.getService(svcRef);
        });
        if (cdiService == null) {
            throw new IllegalStateException("Failed to get the CDIService.");
        }
        return cdiService;
    }

}
