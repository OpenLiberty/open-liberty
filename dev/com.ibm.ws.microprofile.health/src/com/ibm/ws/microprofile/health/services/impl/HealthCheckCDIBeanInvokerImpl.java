/*******************************************************************************
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.health.services.impl;

import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.util.AnnotationLiteral;
import javax.naming.InitialContext;

import org.eclipse.microprofile.health.Health;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.ws.microprofile.health.services.HealthCheckBeanCallException;
import com.ibm.ws.microprofile.health.services.HealthCheckCDIBeanInvoker;

@Component(service = HealthCheckCDIBeanInvoker.class, configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM" })
public class HealthCheckCDIBeanInvokerImpl implements HealthCheckCDIBeanInvoker {

    private final static TraceComponent tc = Tr.register(com.ibm.ws.microprofile.health.services.impl.HealthCheckCDIBeanInvokerImpl.class);

    @Override
    public Set<HealthCheckResponse> checkAllBeans() throws HealthCheckBeanCallException {
        Set<HealthCheckResponse> retVal = new HashSet<HealthCheckResponse>();
        for (Object obj : getHealthCheckBeans()) {
            HealthCheck tempHCBean = (HealthCheck) obj;
            try {
                retVal.add(tempHCBean.call());
            } catch (Throwable e) {
                HealthCheckBeanCallException hcbce = new HealthCheckBeanCallException(e);
                hcbce.setBeanName(obj.getClass().toString());
                throw hcbce;
            }
            Tr.event(tc, "HealthCheck beanClass: " + obj.getClass() + " called");
        }
        return retVal;
    }

    private Set<Object> getHealthCheckBeans() {
        BeanManager beanManager = null;
        Set<Object> healthCheckBeans = new HashSet<Object>();
        Set<Bean<?>> beans;
        try {
            InitialContext context = new InitialContext();
            beanManager = (BeanManager) context.lookup("java:comp/BeanManager");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (beanManager != null) {
            beans = beanManager.getBeans(HealthCheck.class, new AnnotationLiteral<Health>() {});
            for (Bean<?> bean : beans) {
                Tr.event(tc, "Bean Found: HealthCheck beanClass = " + bean.getBeanClass() + ", class = " + bean.getClass() + ", name = " + bean.getName());
                healthCheckBeans.add(beanManager.getReference(bean, bean.getBeanClass(), beanManager.createCreationalContext(bean)));
            }
        }
        return healthCheckBeans;
    }
}
