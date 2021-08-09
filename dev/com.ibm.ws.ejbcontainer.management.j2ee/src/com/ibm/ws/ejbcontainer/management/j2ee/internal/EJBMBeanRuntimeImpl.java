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
package com.ibm.ws.ejbcontainer.management.j2ee.internal;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import javax.management.ObjectName;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.management.j2ee.EJBModuleMBean;
import com.ibm.websphere.management.j2ee.EntityBeanMBean;
import com.ibm.websphere.management.j2ee.J2EEManagementObjectNameFactory;
import com.ibm.websphere.management.j2ee.MessageDrivenBeanMBean;
import com.ibm.websphere.management.j2ee.SingletonSessionBeanMBean;
import com.ibm.websphere.management.j2ee.StatefulSessionBeanMBean;
import com.ibm.websphere.management.j2ee.StatelessSessionBeanMBean;
import com.ibm.websphere.management.j2ee.J2EEManagementObjectNameFactory.ModuleType;
import com.ibm.ws.ejbcontainer.EJBComponentMetaData;
import com.ibm.ws.ejbcontainer.EJBType;
import com.ibm.ws.ejbcontainer.osgi.EJBMBeanRuntime;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;

@Component
public class EJBMBeanRuntimeImpl implements EJBMBeanRuntime {
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
    public ServiceRegistration<?> registerModuleMBean(String appName, String moduleName, Container container, String ddPath, List<EJBComponentMetaData> ejbs) {
        String serverName = locationAdmin.getServerName();
        ObjectName objectName = J2EEManagementObjectNameFactory.createModuleObjectName(ModuleType.EJBModule, moduleName, appName, serverName);
        ObjectName serverObjectName = J2EEManagementObjectNameFactory.createJ2EEServerObjectName(serverName);
        ObjectName jvmObjectName = J2EEManagementObjectNameFactory.createJVMObjectName(serverName);

        ObjectName[] ejbObjectNames = new ObjectName[ejbs.size()];
        for (int i = 0; i < ejbs.size(); i++) {
            EJBComponentMetaData ejb = ejbs.get(i);
            ejbObjectNames[i] = J2EEManagementObjectNameFactory.createEJBObjectName(getJ2EEType(ejb.getEJBType()), ejb.getName(), moduleName, appName, serverName);
        }

        return registerMBean(objectName, EJBModuleMBean.class,
                             new EJBModule(objectName, serverObjectName, jvmObjectName, container, ddPath, ejbObjectNames));
    }

    private static J2EEManagementObjectNameFactory.EJBType getJ2EEType(EJBType type) {
        switch (type) {
            case STATELESS_SESSION:
                return J2EEManagementObjectNameFactory.EJBType.StatelessSessionBean;
            case STATEFUL_SESSION:
                return J2EEManagementObjectNameFactory.EJBType.StatefulSessionBean;
            case SINGLETON_SESSION:
                return J2EEManagementObjectNameFactory.EJBType.SingletonSessionBean;
            case BEAN_MANAGED_ENTITY:
                return J2EEManagementObjectNameFactory.EJBType.EntityBean;
            case CONTAINER_MANAGED_ENTITY:
                return J2EEManagementObjectNameFactory.EJBType.EntityBean;
            case MESSAGE_DRIVEN:
                return J2EEManagementObjectNameFactory.EJBType.MessageDrivenBean;
        }
        throw new IllegalArgumentException(type.toString());
    }

    @Override
    public ServiceRegistration<?> registerEJBMBean(String appName, String moduleName, String beanName, EJBType type) {
        String serverName = locationAdmin.getServerName();
        ObjectName objectName = J2EEManagementObjectNameFactory.createEJBObjectName(getJ2EEType(type), beanName, moduleName, appName, serverName);

        switch (type) {
            case STATELESS_SESSION:
                return registerMBean(objectName, StatelessSessionBeanMBean.class, new StatelessSessionBean(objectName));
            case STATEFUL_SESSION:
                return registerMBean(objectName, StatefulSessionBeanMBean.class, new StatefulSessionBean(objectName));
            case SINGLETON_SESSION:
                return registerMBean(objectName, SingletonSessionBeanMBean.class, new SingletonSessionBean(objectName));
            case BEAN_MANAGED_ENTITY:
            case CONTAINER_MANAGED_ENTITY:
                return registerMBean(objectName, EntityBeanMBean.class, new EntityBean(objectName));
            case MESSAGE_DRIVEN:
                return registerMBean(objectName, MessageDrivenBeanMBean.class, new MessageDrivenBean(objectName));
        }

        throw new IllegalArgumentException(type.toString());
    }
}
