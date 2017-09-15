/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.container.v21.cdi.internal;

import java.lang.reflect.Proxy;
import java.util.Map;

import javax.enterprise.inject.spi.BeanManager;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.CDIService;
import com.ibm.ws.jpa.management.JPAEMFPropertyProvider;

/**
 * Class to provide properties specific to CDI to the JPA container.
 */
@Component
public class CDIJPAEMFPropertyProviderImpl implements JPAEMFPropertyProvider {
    private static final TraceComponent tc = Tr.register(CDIJPAEMFPropertyProviderImpl.class);
    private static final ClassLoader CLASSLOADER = BeanManager.class.getClassLoader();
    /**
     * Spec-defined property used for passing an instance of the BeanManager to
     * to the JPA Provider for injection into entity listeners.
     */
    private static final String CDI_BEANMANAGER = "javax.persistence.bean.manager";

    private CDIService cdiService;

    @Override
    public void updateProperties(Map<String, Object> props) {
        if (cdiService != null) {
            Object beanManager = Proxy.newProxyInstance(CLASSLOADER,
                                                        new Class<?>[] { BeanManager.class },
                                                        new BeanManagerInvocationHandler(cdiService));
            props.put(CDI_BEANMANAGER, beanManager);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "updateProperties setting {0}={1}", CDI_BEANMANAGER, beanManager);
            }
        }

    }

    @Reference
    protected void setCDIService(CDIService cdiService) {
        this.cdiService = cdiService;
    }

    protected void unsetCDIService(CDIService cdiService) {
        this.cdiService = null;
    }
}
