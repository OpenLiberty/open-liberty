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
package com.ibm.ws.messaging.jms.j2ee.mbeans.internal;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.messaging.jms.j2ee.mbeans.JmsServiceProviderMBeanImpl;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * Registers JMS related J2EE MBeans (as per JSR 77 spec)
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
                immediate = true,
                property = { "service.vendor=IBM" })
public class JMSMbeansActivator {

    // because we use a pack-info.java for trace options our group and message file is already there
    // We just need to register the class here
    private static final TraceComponent tc = Tr.register(JMSMbeansActivator.class);

    private static final String KEY_JMX_OBJECTNAME = "jmx.objectname";
    private static final String KEY_SERVICE_VENDOR = "service.vendor";
    private static final String KEY_JMS2_PROVIDER = "JMS-2.0Provider";

    private static final String KEY_LOCATION_ADMIN = "locationAdmin";
    private final AtomicServiceReference<WsLocationAdmin> locationAdminRef = new AtomicServiceReference<WsLocationAdmin>(KEY_LOCATION_ADMIN);

    ServiceRegistration<?> jmsProvderMBean;

    @Activate
    protected void activate(ComponentContext context) throws IOException {
        locationAdminRef.activate(context);

        //register JMS Provider MBean.
        jmsProvderMBean = registerMBeanService(KEY_JMS2_PROVIDER, context.getBundleContext());

    }

    @Deactivate
    protected void deactivate(ComponentContext context) {

        locationAdminRef.deactivate(context);

        //unregister provider MBean
        jmsProvderMBean.unregister();

    }

    @Reference(name = KEY_LOCATION_ADMIN, service = WsLocationAdmin.class,
                    cardinality = ReferenceCardinality.MANDATORY)
    protected void setLocationAdmin(ServiceReference<WsLocationAdmin> ref) {
        locationAdminRef.setReference(ref);
    }

    protected void unsetLocationAdmin(ServiceReference<WsLocationAdmin> ref) {
        locationAdminRef.unsetReference(ref);
    }

    private String getServerName() {
        WsLocationAdmin locationAdmin = locationAdminRef.getService();

        if (locationAdmin == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "locationAdmin is not bound. Hence j2EEServer name will be null ");
            }
            return null;

        } else {
            return locationAdmin.getServerName();
        }
    }

    /**
     * Registers MBean for JMSServiceProvider .. in future can be made generic.
     * 
     * @param jmsResourceName
     * @param bundleContext
     * @return
     */
    private ServiceRegistration<?> registerMBeanService(String jmsResourceName, BundleContext bundleContext) {
        Dictionary<String, String> props = new Hashtable<String, String>();
        props.put(KEY_SERVICE_VENDOR, "IBM");

        JmsServiceProviderMBeanImpl jmsProviderMBean = new JmsServiceProviderMBeanImpl(getServerName(), KEY_JMS2_PROVIDER);
        props.put(KEY_JMX_OBJECTNAME, jmsProviderMBean.getobjectName());

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "JmsQueueMBeanImpl=" + jmsProviderMBean.getobjectName() + " props=" + props);
        }
        return bundleContext.registerService(JmsServiceProviderMBeanImpl.class, jmsProviderMBean, props);
    }

}
