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
package com.ibm.ws.jndi.management.j2ee.internal;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import javax.management.ObjectName;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.management.j2ee.J2EEManagedObject;
import com.ibm.websphere.management.j2ee.J2EEManagementObjectNameFactory;
import com.ibm.websphere.management.j2ee.JNDIResourceMBean;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;

/**
 * A declarative services component can be completely POJO based
 * (no awareness/use of OSGi services).
 * 
 * OSGi methods (activate/deactivate) should be protected.
 */
@Component
public class JNDIMBeanRuntime {
    private BundleContext context;
    private WsLocationAdmin locationAdmin;
    private ObjectName objectName;
    private ServiceRegistration<JNDIResourceMBean> serviceRegistration;

    private static final TraceComponent tc = Tr.register(JNDIMBeanRuntime.class);

    @Reference
    protected void setLocationAdmin(WsLocationAdmin locationAdmin) {
        this.locationAdmin = locationAdmin;
    }

    /**
     * DS method to activate this component.
     * Best practice: this should be a protected method, not public or private
     * 
     * @param cc : BundleContext passed when the bundle is activated
     * @param properties : Map containing service & config properties
     *            populated/provided by config admin
     */
    @Activate
    protected void activate(BundleContext cc, Map<String, Object> properties) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "JNDIMBeanRuntime activated", properties);
        }

        this.context = cc;
        String serverName = locationAdmin.getServerName();

        objectName = J2EEManagementObjectNameFactory.createResourceObjectName(serverName, J2EEManagementObjectNameFactory.TYPE_JNDI_RESOURCE, "NameService");
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Generated ObjectName", objectName);
        }
        //Instantiate a new MBean
        JNDIResourceMBean mbean = new JNDIMBeanImpl(objectName);

        this.serviceRegistration = registerMBean(objectName, JNDIResourceMBean.class, mbean);

    }

    /**
     * DS method to deactivate this component.
     * Best practice: this should be a protected method, not public or private
     * 
     * @param reason int representation of reason the component is stopping
     */
    @Deactivate
    protected void deactivate(int reason) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            Tr.event(tc, "JNDIMBeanRuntime deactivated, reason=" + reason);

        serviceRegistration.unregister();
    }

    /**
     * Used to Register an MBean
     * 
     * @param on : The ObjectName registration for the MBean
     * @param type : The class type of the MBean being register
     * @param o: The MBean
     * @return : A service registration that provides access to manage the MBean
     */
    private <T> ServiceRegistration<T> registerMBean(ObjectName on, Class<T> type, T o) {
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("jmx.objectname", on.toString());
        return context.registerService(type, o, props);
    }

    private class JNDIMBeanImpl extends J2EEManagedObject implements JNDIResourceMBean {

        /**
         * @param objectName
         */
        public JNDIMBeanImpl(ObjectName objectName) {
            super(objectName);
        }
    }
}
