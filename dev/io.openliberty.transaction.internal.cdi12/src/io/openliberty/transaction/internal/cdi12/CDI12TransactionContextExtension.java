/*******************************************************************************
 * Copyright (c) 2015, 2022 IBM Corporation and others.
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
package io.openliberty.transaction.internal.cdi12;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;

import org.osgi.service.component.annotations.Component;

import com.ibm.tx.TranConstants;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.extension.WebSphereCDIExtension;

@Component(service = WebSphereCDIExtension.class,
           property = { "bean.defining.annotations=javax.transaction.TransactionScoped" })
public class CDI12TransactionContextExtension implements Extension, WebSphereCDIExtension {

    private static final TraceComponent tc = Tr.register(CDI12TransactionContext.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery event, BeanManager manager) {
        CDI12TransactionContext tc = new CDI12TransactionContext(manager);
        event.addContext(tc);
    }
}
