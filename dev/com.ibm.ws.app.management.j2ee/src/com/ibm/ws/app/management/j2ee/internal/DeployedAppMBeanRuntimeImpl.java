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
package com.ibm.ws.app.management.j2ee.internal;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import javax.management.ObjectName;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.management.j2ee.J2EEApplicationMBean;
import com.ibm.websphere.management.j2ee.J2EEManagementObjectNameFactory;
import com.ibm.websphere.management.j2ee.J2EEManagementObjectNameFactory.ModuleType;
import com.ibm.ws.app.manager.module.DeployedAppMBeanRuntime;
import com.ibm.ws.container.service.app.deploy.ClientModuleInfo;
import com.ibm.ws.container.service.app.deploy.ConnectorModuleInfo;
import com.ibm.ws.container.service.app.deploy.EJBModuleInfo;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;

@Component
public class DeployedAppMBeanRuntimeImpl implements DeployedAppMBeanRuntime {
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

    private <T> ServiceRegistration<T> registerMBean(ObjectName on, Class<T> type, T o) {
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("jmx.objectname", on.toString());
        return context.registerService(type, o, props);
    }

    @Override
    public ServiceRegistration<?> registerApplicationMBean(String appName, Container container, String ddPath, List<ModuleInfo> modules) {
        String serverName = locationAdmin.getServerName();
        ObjectName objectName = J2EEManagementObjectNameFactory.createApplicationObjectName(appName, serverName);
        ObjectName serverObjectName = J2EEManagementObjectNameFactory.createJ2EEServerObjectName(serverName);

        ObjectName[] moduleObjectNames = new ObjectName[modules.size()];
        for (int i = 0; i < modules.size(); i++) {
            ModuleInfo modInfo = modules.get(i);
            ModuleType type = null;
            if (modInfo instanceof WebModuleInfo) {
                type = ModuleType.WebModule;
            } else if (modInfo instanceof EJBModuleInfo) {
                type = ModuleType.EJBModule;
            } else if (modInfo instanceof ConnectorModuleInfo) {
                type = ModuleType.ResourceAdapterModule;
            } else if (modInfo instanceof ClientModuleInfo) {
                type = ModuleType.AppClientModule;
            }
            moduleObjectNames[i] = J2EEManagementObjectNameFactory.createModuleObjectName(type, modInfo.getURI(), appName, serverName);
        }

        return registerMBean(objectName, J2EEApplicationMBean.class,
                             new DeployedApplication(objectName, serverObjectName, container, ddPath, moduleObjectNames));
    }
}
