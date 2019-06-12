/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.microprofile.health20.services.impl;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.util.AnnotationLiteral;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import org.eclipse.microprofile.health.Health;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.microprofile.health.services.HealthCheckBeanCallException;
import com.ibm.ws.microprofile.health20.internal.HealthCheckConstants;
import com.ibm.ws.microprofile.health20.services.HealthCheck20CDIBeanInvoker;

@Component(service = HealthCheck20CDIBeanInvoker.class, configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM" })
public class HealthCheck20CDIBeanInvokerImpl implements HealthCheck20CDIBeanInvoker {

    private final static TraceComponent tc = Tr.register(com.ibm.ws.microprofile.health20.services.impl.HealthCheck20CDIBeanInvokerImpl.class);

    /** {@inheritDoc} */
    @Override
    public Set<HealthCheckResponse> checkAllBeans() throws HealthCheckBeanCallException {
        return checkAllBeans(HealthCheckConstants.HEALTH_CHECK_ALL);
    }

    @Override
    public Set<HealthCheckResponse> checkAllBeans(String healthCheckProcedure) throws HealthCheckBeanCallException {
        Set<HealthCheckResponse> retVal = new HashSet<HealthCheckResponse>();
        Set<Object> healthCheckBeans = new HashSet<Object>();

        switch (healthCheckProcedure) {
            case HealthCheckConstants.HEALTH_CHECK_LIVE:
                healthCheckBeans = getHealthCheckLivenessBeans();
                break;
            case HealthCheckConstants.HEALTH_CHECK_READY:
                healthCheckBeans = getHealthCheckReadinessBeans();
                break;
            default:
                healthCheckBeans = getAllHealthCheckBeans();
        }

        for (Object obj : healthCheckBeans) {
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

    private Set<Object> getHealthCheckLivenessBeans() {
        Set<Object> hcLivenessBeans = new HashSet<Object>();
        Annotation hcLivenessQualifier = new AnnotationLiteral<Liveness>() {};
        hcLivenessBeans = getHealthCheckBeans(hcLivenessQualifier);

        return hcLivenessBeans;
    }

    private Set<Object> getHealthCheckReadinessBeans() {
        Set<Object> hcReadinessBeans = new HashSet<Object>();

        Annotation hcReadinessQualifier = new AnnotationLiteral<Readiness>() {};
        hcReadinessBeans = getHealthCheckBeans(hcReadinessQualifier);

        return hcReadinessBeans;
    }

    private Set<Object> getHealthCheckHealthBeans() {
        Set<Object> hcHealthBeans = new HashSet<Object>();

        Annotation hcHealthQualifier = new AnnotationLiteral<Health>() {};
        hcHealthBeans = getHealthCheckBeans(hcHealthQualifier);

        return hcHealthBeans;
    }

    private Set<Object> getAllHealthCheckBeans() {
        Set<Object> hcAllBeans = new HashSet<Object>();

        hcAllBeans.addAll(getHealthCheckLivenessBeans());
        hcAllBeans.addAll(getHealthCheckReadinessBeans());
        hcAllBeans.addAll(getHealthCheckHealthBeans());

        return hcAllBeans;
    }

    @FFDCIgnore(NameNotFoundException.class)
    private Set<Object> getHealthCheckBeans(Annotation hcQualifier) {
        BeanManager beanManager = null;
        Set<Object> healthCheckBeans = new HashSet<Object>();
        Set<Bean<?>> beans;
        try {
            InitialContext context = new InitialContext();
            beanManager = (BeanManager) context.lookup("java:comp/BeanManager");
        } catch (NameNotFoundException e) {
            Tr.event(tc, "Catching NameNotFoundException looking up CDI java:comp/BeanManager.  Ignoring assuming the reason is because there are zero CDI managed beans");
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
        if (beanManager != null) {
            beans = beanManager.getBeans(HealthCheck.class, hcQualifier);
            for (Bean<?> bean : beans) {
                Tr.event(tc, "Bean Found: HealthCheck beanClass = " + bean.getBeanClass() + ", class = " + bean.getClass() + ", name = " + bean.getName());
                healthCheckBeans.add(beanManager.getReference(bean, bean.getBeanClass(), beanManager.createCreationalContext(bean)));
            }
        }
        return healthCheckBeans;
    }

}
