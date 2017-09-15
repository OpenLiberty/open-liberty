/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.server.component;

import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxrs20.api.JaxRsBeanValidationService;

/**
 * 
 */
@Component(name = "com.ibm.ws.jaxrs20.server.component.JaxRsBeanValidation", immediate = true, property = { "service.vendor=IBM" })
public class JaxRsBeanValidation {
    private static final TraceComponent tc = Tr.register(JaxRsBeanValidation.class);
    private static volatile JaxRsBeanValidationService veanValidationService = null;

    @Reference(name = "jaxRsBeanValidationService",
               service = JaxRsBeanValidationService.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setJaxRsBeanValidationService(JaxRsBeanValidationService bean) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "registerJaxRsBeanValidationService");
        }
        veanValidationService = bean;
    }

    protected void unsetJaxRsBeanValidationService(JaxRsBeanValidationService bean) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "unRegisterJaxRsBeanValidationService");
        }
        if (veanValidationService == bean)
            veanValidationService = null;
    }

    public static boolean enableBeanValidationProviders(List<Object> providers) {
        if (veanValidationService != null) {
            return veanValidationService.enableBeanValidationProviders(providers);
        }
        return false;
    }

    public static Class<?> getBeanValidationProviderClass() {
        if (veanValidationService != null) {
            return veanValidationService.getBeanValidationProviderClass();
        }
        return null;
    }

}
