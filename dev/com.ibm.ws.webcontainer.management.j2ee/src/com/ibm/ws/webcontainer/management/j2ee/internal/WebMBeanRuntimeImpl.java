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
package com.ibm.ws.webcontainer.management.j2ee.internal;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.management.ObjectName;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.management.j2ee.J2EEManagementObjectNameFactory;
import com.ibm.websphere.management.j2ee.ServletMBean;
import com.ibm.websphere.management.j2ee.WebModuleMBean;
import com.ibm.ws.webcontainer.osgi.WebMBeanRuntime;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.webcontainer.servlet.IServletConfig;

@Component
public class WebMBeanRuntimeImpl implements WebMBeanRuntime {
    private BundleContext context;
    private WsLocationAdmin locationAdmin;

    @Reference
    protected void setLocationAdmin(WsLocationAdmin locationAdmin) {
        this.locationAdmin = locationAdmin;
    }

    @Activate
    protected void activate(BundleContext context) {
        this.context = context;
    }

    public static final String JMX_OBJECT_NAME_KEY = "jmx.objectname";

    private <T> ServiceRegistration<T> registerMBean(ObjectName objName, Class<T> classType, T serviceObj) {
        // TODO: log the registration of the MBean
        Dictionary<String, Object> properties = new Hashtable<String, Object>(1);
        properties.put(JMX_OBJECT_NAME_KEY, objName.toString());
        return context.registerService(classType, serviceObj, properties);
    }

    /** {@inheritDoc} */
    @Override
    public ServiceRegistration<?> registerModuleMBean
                    (String appName,
                     String moduleName,
                     Container container,
                     String ddPath,
                     Iterator<IServletConfig> servletConfigs) {

        String serverName = locationAdmin.getServerName();
        ObjectName objectName = J2EEManagementObjectNameFactory.createModuleObjectName
                        (J2EEManagementObjectNameFactory.ModuleType.WebModule, moduleName, appName, serverName);

        ObjectName serverObjectName = J2EEManagementObjectNameFactory.createJ2EEServerObjectName(serverName);
        ObjectName jvmObjectName = J2EEManagementObjectNameFactory.createJVMObjectName(serverName);

        List<String> servletNames = new ArrayList<String>();
        while (servletConfigs.hasNext()) {
            IServletConfig servletConfig = servletConfigs.next();
            servletNames.add(servletConfig.getServletName());
        }

        int numServlets = servletNames.size();
        ObjectName[] servletObjectNames = new ObjectName[numServlets];
        for (int servletNo = 0; servletNo < numServlets; servletNo++) {
            String servletName = servletNames.get(servletNo);
            servletObjectNames[servletNo] = J2EEManagementObjectNameFactory.createServletObjectName(servletName, moduleName, appName, serverName);
        }

        return registerMBean(objectName, WebModuleMBean.class, new WebModule(objectName, serverObjectName, jvmObjectName, container, ddPath, servletObjectNames));
    }

    /** {@inheritDoc} */
    @Override
    public ServiceRegistration<?> registerServletMBean(String appName, String moduleName, String servletName) {
        String serverName = locationAdmin.getServerName();
        ObjectName objectName = J2EEManagementObjectNameFactory.createServletObjectName(servletName, moduleName, appName, serverName);
        return registerMBean(objectName, ServletMBean.class, new Servlet(objectName));
    }

}
