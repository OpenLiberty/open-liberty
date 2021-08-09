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
package com.ibm.ws.tx.admin.internal;

import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.management.j2ee.JTAResourceMBean;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;

@Component
public class TransactionMBeanServiceImpl {

    private static final TraceComponent tc = Tr.register(TransactionMBeanServiceImpl.class);

    static String objectNameStem = "WebSphere:j2eeType=JTAResource,name=TransactionService,J2EEServer=";

    private WsLocationAdmin locationAdmin;
    private ServiceRegistration<?> sr;

    @Reference
    protected void setLocationAdmin(WsLocationAdmin locationAdmin) {
        this.locationAdmin = locationAdmin;
    }

    @Activate
    protected void activate(BundleContext context) {
        final String objectName = objectNameStem + locationAdmin.getServerName();

        final Hashtable<String, String> properties = new Hashtable<String, String>();
        properties.put("service.vendor", "IBM");
        properties.put("jmx.objectname", objectName);

        sr = context.registerService(JTAResourceMBean.class.getName(), new TransactionServiceMBeanImpl(objectName), properties);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            Tr.event(tc, "TransactionServiceMBean registered", sr);
    }

    @Deactivate
    protected void deactivate(BundleContext context) {
        sr.unregister();
    }
}