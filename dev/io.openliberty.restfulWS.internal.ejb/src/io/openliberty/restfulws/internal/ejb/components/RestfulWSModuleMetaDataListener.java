/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.restfulws.internal.ejb.components;

import java.util.ArrayList;
import java.util.List;

import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.EJBModuleInfo;
import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedModuleInfo;
import com.ibm.ws.container.service.metadata.MetaDataEvent;
import com.ibm.ws.container.service.metadata.ModuleMetaDataListener;
import com.ibm.ws.ejbcontainer.EJBEndpoint;
import com.ibm.ws.ejbcontainer.EJBEndpoints;
import com.ibm.ws.ejbcontainer.EJBType;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

import io.openliberty.restfulWS.internal.common.api.RestfulWSEJBUtils;

@Component(name = "io.openliberty.restfulws.internal.ejb.components.RestfulWSModuleMetaDataListener", immediate = true, property = { "service.vendor=IBM" })
public class RestfulWSModuleMetaDataListener implements ModuleMetaDataListener, RestfulWSEJBUtils {

    private static final TraceComponent tc = Tr.register(RestfulWSModuleMetaDataListener.class);

    private static volatile Container moduleContainer = null;

    /*
     * Returns the JNDI String or null if it's not an EJB.
     */
    @Override
    public List<String> getJNDIResource(String className) {
        System.out.println("Adam - getJNDIResource(String className) className=" + className);
        List<String> jndiList = new ArrayList<String>();
        try {
            EJBEndpoints ejbEndpoints = moduleContainer.adapt(EJBEndpoints.class);
            System.out.println("Adam - ejbEndpoints=" + ejbEndpoints);
            for (EJBEndpoint ejbEndpoint : ejbEndpoints.getEJBEndpoints()) {
                if (ejbEndpoint.getClassName().equals(className)) {
                    System.out.println("Adam - ejbEndpoint=" + ejbEndpoint);
                    EJBType ejbType = ejbEndpoint.getEJBType();
                    System.out.println("Adam - ejbType=" + ejbType);
                    if (ejbType != EJBType.SINGLETON_SESSION && ejbType != EJBType.STATELESS_SESSION) {
                        continue;
                    }
                    
                    System.out.println("Adam - isLocalBean=" + ejbEndpoint.isLocalBean());
                    System.out.println("Adam - ejbClassName=" + ejbEndpoint.getClassName());
                    System.out.println("Adam -    className=" + className);
                    if (ejbEndpoint.isLocalBean()) {
                        System.out.println("Adam - no interface");
                        jndiList.add(getJNDIName(ejbEndpoint, null));
                    } else {
                        for (String iface : ejbEndpoint.getLocalBusinessInterfaceNames()) {
                            ejbEndpoint.getReferenceFactory();
                            System.out.println("Adam - iface=" + iface);
                            // TODO: can className be an interface? Or do we have to compare that elsewhere?
                            // return getJNDIName();
                            jndiList.add(getJNDIName(ejbEndpoint, iface));
                        }
                    }
                }
            }
        } catch (UnableToAdaptException e) {
            return null;
        }
        return jndiList;
    }
    
    private String getJNDIName(EJBEndpoint ejbEndpoint, String interfaceName) {

        /**
         * We need consider 2 cases:
         * 1)EJB jaxrs in war: JNDI lookup format is java:module/<beanName>[!<interface>]
         * 2)EJB jaxrs in ejb jar: JNDI lookup format is java:app/<ejbmodulename>/<beanName>[!<interface>]
         */
        String beanName = ejbEndpoint.getName();
        ExtendedModuleInfo moduleInfo = getModuleInfo(moduleContainer);
        String moduleName = moduleInfo.getName();
        
        System.out.println("Adam - moduleName=" + moduleName);
        
        StringBuffer jndiName = new StringBuffer();
        if (moduleName == null) {
            jndiName.append("java:module/").append(beanName);
        } else {
            jndiName.append("java:app/").append(moduleName + "/").append(beanName);
        }
        
        // TODO: how do we determine the interface?
        if ((interfaceName != null) && (!(interfaceName.trim().equals(""))))
            jndiName.append("!").append(interfaceName);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "jndi name is" + jndiName.toString());
        }
        return jndiName.toString();
    }

    @Override
    public void moduleMetaDataCreated(MetaDataEvent<ModuleMetaData> event) {
        RestfulWSModuleMetaDataListener.moduleContainer = event.getContainer();
        System.out.println("Adam - moduleContainer=" + moduleContainer);
    }

    @Override
    public void moduleMetaDataDestroyed(MetaDataEvent<ModuleMetaData> event) {
        RestfulWSModuleMetaDataListener.moduleContainer = null;
    }
    
    private ExtendedModuleInfo getModuleInfo(Container container) {
        ExtendedModuleInfo moduleInfo = null;
        NonPersistentCache overlayCache;
        try {
            overlayCache = container.adapt(NonPersistentCache.class);
        } catch (UnableToAdaptException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Unable to get ModuleInfo due to no NonPersistentCache found");
            }
            return null;
        }
        if (overlayCache != null) {
            moduleInfo = (ExtendedModuleInfo) overlayCache.getFromCache(WebModuleInfo.class);
            if (moduleInfo == null) {
                moduleInfo = (ExtendedModuleInfo) overlayCache.getFromCache(EJBModuleInfo.class);
            }
        }
        return moduleInfo;
    }

}
