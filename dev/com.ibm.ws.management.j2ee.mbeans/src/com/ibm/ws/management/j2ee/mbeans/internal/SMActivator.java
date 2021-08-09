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

package com.ibm.ws.management.j2ee.mbeans.internal;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.management.j2ee.mbeans.J2EEDomainMBeanImpl;
import com.ibm.ws.management.j2ee.mbeans.J2EEServerMBeanImpl;
import com.ibm.ws.management.j2ee.mbeans.JVMMBeanImpl;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * Registers System Management(SM) related J2EE MBeans (JVMMBean, J2EEDomainMBean, J2EEServerMBean and JavaMailResourceMBean) with MBeanServer.
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
                immediate = true,
                property = { "service.vendor=IBM" })
public class SMActivator {

    public static final String TRACE_GROUP_SM_MBEANS = "management.j2ee.mbeans";
    public static final String TRACE_BUNDLE_SM_MBEANS = "com.ibm.ws.management.j2ee.mbeans.internal.resources.SMMessages";

    private static final TraceComponent tc = Tr.register(SMActivator.class, TRACE_GROUP_SM_MBEANS, TRACE_BUNDLE_SM_MBEANS);

    private static final String KEY_JMX_OBJECTNAME = "jmx.objectname";
    private static final String KEY_SERVICE_VENDOR = "service.vendor";

    private static final int JVM_INDEX = 1;
    private static final int J2EEDOMAIN_INDEX = 2;
    private static final int J2EESERVER_INDEX = 3;
    private static final int JMR_INDEX = 4;

    private static final String KEY_LOCATION_ADMIN = "locationAdmin";
    private final AtomicServiceReference<WsLocationAdmin> locationAdminRef = new AtomicServiceReference<WsLocationAdmin>(KEY_LOCATION_ADMIN);

    @Activate
    protected void activate(ComponentContext context) throws IOException {
        locationAdminRef.activate(context);
        BundleContext bundle = context.getBundleContext();
        registerService(JVM_INDEX, bundle);
        registerService(J2EEDOMAIN_INDEX, bundle);
        registerService(J2EESERVER_INDEX, bundle);

    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        // TODO deactivate anything else?
        locationAdminRef.deactivate(context);
    }

    @Reference(name = KEY_LOCATION_ADMIN, service = WsLocationAdmin.class)
    protected void setLocationAdmin(ServiceReference<WsLocationAdmin> ref) {
        locationAdminRef.setReference(ref);
    }

    protected void unsetLocationAdmin(ServiceReference<WsLocationAdmin> ref) {
        locationAdminRef.unsetReference(ref);
    }

    protected WsLocationAdmin getWsLocationAdmin() {
        WsLocationAdmin locationAdmin = locationAdminRef.getService();

        if (locationAdmin == null) {
            throwMissingServiceError("WsLocationAdmin");
        }

        return locationAdmin;
    }

    private void throwMissingServiceError(String service) {
        throw new RuntimeException(Tr.formatMessage(tc, "OSGI_SERVICE_ERROR", service));
    }

    private void registerService(int index, BundleContext bundle) {
        Dictionary<String, String> props = new Hashtable<String, String>();
        props.put(KEY_SERVICE_VENDOR, "IBM");

        switch (index) {
            case JVM_INDEX: //JVM
                JVMMBeanImpl jvm = new JVMMBeanImpl(getServerName());
                props.put(KEY_JMX_OBJECTNAME, jvm.getobjectName());
                bundle.registerService(JVMMBeanImpl.class, jvm, props);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "jvm=" + jvm.toString() + " props=" + props);
                }

                break;
            case J2EEDOMAIN_INDEX: //J2EEDomain
                J2EEDomainMBeanImpl j2eeDomain = new J2EEDomainMBeanImpl();
                props.put(KEY_JMX_OBJECTNAME, j2eeDomain.getobjectName());
                bundle.registerService(J2EEDomainMBeanImpl.class, j2eeDomain, props);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "j2eeDomain=" + j2eeDomain.toString() + " props=" + props);
                }

                break;
            case J2EESERVER_INDEX: //J2EEServer
                J2EEServerMBeanImpl j2eeServer = new J2EEServerMBeanImpl(getServerName());
                props.put(KEY_JMX_OBJECTNAME, j2eeServer.getobjectName());
                bundle.registerService(J2EEServerMBeanImpl.class, j2eeServer, props);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "j2eeServer=" + j2eeServer.toString() + " props=" + props);
                }

                break;
            case JMR_INDEX: //JavaMailResource
                break;
        }
    }

    private String getServerName() {
        return getWsLocationAdmin().getServerName();
    }
}
