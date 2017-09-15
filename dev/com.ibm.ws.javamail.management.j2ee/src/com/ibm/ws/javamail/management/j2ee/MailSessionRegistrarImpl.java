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
package com.ibm.ws.javamail.management.j2ee;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.management.ObjectName;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.management.j2ee.J2EEManagementObjectNameFactory;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.javamail.j2ee.MailSessionRegistrar;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * Registers JavaMailResourceMBean with MBeanServer.
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
                immediate = true,
                property = { "service.vendor=IBM" })
public class MailSessionRegistrarImpl implements MailSessionRegistrar {

    public static final String TRACE_GROUP_JAVAMAIL = "javamail.management.j2ee";
    public static final String TRACE_BUNDLE_JAVAMAIL = "com.ibm.ws.javamail.management.j2ee.internal.resources.JavaMailMessages";

    private static final TraceComponent tc = Tr.register(MailSessionRegistrarImpl.class, TRACE_GROUP_JAVAMAIL, TRACE_BUNDLE_JAVAMAIL);

    private static final String KEY_JMX_OBJECTNAME = "jmx.objectname";
    private static final String KEY_SERVICE_VENDOR = "service.vendor";

    private static int RESOURCE_COUNTER = 1;

    private static final String KEY_LOCATION_ADMIN = "locationAdmin";
    private final AtomicServiceReference<WsLocationAdmin> locationAdminRef = new AtomicServiceReference<WsLocationAdmin>(KEY_LOCATION_ADMIN);

    private BundleContext bundle;

    @Activate
    protected void activate(ComponentContext context) throws IOException {
        this.bundle = context.getBundleContext();
        locationAdminRef.activate(context);
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
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

    private static void throwMissingServiceError(String service) {
        throw new RuntimeException(Tr.formatMessage(tc, "OSGI_SERVICE_ERROR", service));
    }

    /** {@inheritDoc} */
    @Override
    public ServiceRegistration<?> registerJavaMailMBean(String mailSessionID) {
        ObjectName objectName = J2EEManagementObjectNameFactory.createJavaMailObjectName(getServerName(), mailSessionID, getResourceCounter());
        JavaMailResourceMBeanImpl javaMail = new JavaMailResourceMBeanImpl(objectName);

        Dictionary<String, String> props = new Hashtable<String, String>();
        props.put(KEY_SERVICE_VENDOR, "IBM");
        props.put(KEY_JMX_OBJECTNAME, javaMail.getobjectName());
        ServiceRegistration<?> sr = bundle.registerService(JavaMailResourceMBeanImpl.class, javaMail, props);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "javaMail=" + javaMail.toString() + " props=" + props);
        }
        return sr;
    }

    private static synchronized int getResourceCounter() {
        return RESOURCE_COUNTER++;
    }

    private String getServerName() {
        return getWsLocationAdmin().getServerName();
    }

}
