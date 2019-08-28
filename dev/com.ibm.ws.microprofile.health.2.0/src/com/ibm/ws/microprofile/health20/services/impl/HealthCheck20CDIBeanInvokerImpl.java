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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.util.AnnotationLiteral;

import org.eclipse.microprofile.health.Health;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.ws.cdi.CDIService;
import com.ibm.ws.microprofile.health.services.HealthCheckBeanCallException;
import com.ibm.ws.microprofile.health20.internal.HealthCheckConstants;
import com.ibm.ws.microprofile.health20.services.HealthCheck20CDIBeanInvoker;

@Component(service = HealthCheck20CDIBeanInvoker.class, configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM" })
public class HealthCheck20CDIBeanInvokerImpl implements HealthCheck20CDIBeanInvoker {

    private final static TraceComponent tc = Tr.register(com.ibm.ws.microprofile.health20.services.impl.HealthCheck20CDIBeanInvokerImpl.class);

    private final Map<String, BeanManager> beanManagers = new HashMap<>();

    private CDIService cdiService;

    @Reference(service = CDIService.class)
    protected void setCdiService(CDIService cdiService) {
        this.cdiService = cdiService;
    }

    protected void unsetCdiService(CDIService cdiService) {
        if (this.cdiService == cdiService) {
            this.cdiService = null;
        }
    }

    @Override
    public Set<HealthCheckResponse> checkAllBeans(String appName, String moduleName, String healthCheckProcedure) throws HealthCheckBeanCallException {
        BeanManager beanManager = getBeanManager(appName, moduleName);
        Set<HealthCheckResponse> retVal = new HashSet<HealthCheckResponse>();
        Set<Object> healthCheckBeans = new HashSet<Object>();

        switch (healthCheckProcedure) {
            case HealthCheckConstants.HEALTH_CHECK_LIVE:
                healthCheckBeans = getHealthCheckLivenessBeans(beanManager);
                break;
            case HealthCheckConstants.HEALTH_CHECK_READY:
                healthCheckBeans = getHealthCheckReadinessBeans(beanManager);
                break;
            default:
                healthCheckBeans = getAllHealthCheckBeans(beanManager);
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

    private Set<Object> getHealthCheckLivenessBeans(BeanManager beanManager) {
        Set<Object> hcLivenessBeans = new HashSet<Object>();
        Annotation hcLivenessQualifier = new AnnotationLiteral<Liveness>() {
        };
        hcLivenessBeans = getHealthCheckBeans(beanManager, hcLivenessQualifier);

        return hcLivenessBeans;
    }

    private Set<Object> getHealthCheckReadinessBeans(BeanManager beanManager) {
        Set<Object> hcReadinessBeans = new HashSet<Object>();

        Annotation hcReadinessQualifier = new AnnotationLiteral<Readiness>() {
        };
        hcReadinessBeans = getHealthCheckBeans(beanManager, hcReadinessQualifier);

        return hcReadinessBeans;
    }

    private Set<Object> getHealthCheckHealthBeans(BeanManager beanManager) {
        Set<Object> hcHealthBeans = new HashSet<Object>();

        Annotation hcHealthQualifier = new AnnotationLiteral<Health>() {
        };
        hcHealthBeans = getHealthCheckBeans(beanManager, hcHealthQualifier);

        return hcHealthBeans;
    }

    private Set<Object> getAllHealthCheckBeans(BeanManager beanManager) {
        Set<Object> hcAllBeans = new HashSet<Object>();

        hcAllBeans.addAll(getHealthCheckLivenessBeans(beanManager));
        hcAllBeans.addAll(getHealthCheckReadinessBeans(beanManager));
        hcAllBeans.addAll(getHealthCheckHealthBeans(beanManager));

        return hcAllBeans;
    }

    private Set<Object> getHealthCheckBeans(BeanManager beanManager, Annotation hcQualifier) {
        Set<Object> healthCheckBeans = new HashSet<Object>();
        Set<Bean<?>> beans;
        if (beanManager != null) {
            beans = beanManager.getBeans(HealthCheck.class, hcQualifier);
            for (Bean<?> bean : beans) {
                Tr.event(tc, "Bean Found: HealthCheck beanClass = " + bean.getBeanClass() + ", class = " + bean.getClass() + ", name = " + bean.getName());
                healthCheckBeans.add(beanManager.getReference(bean, HealthCheck.class, beanManager.createCreationalContext(bean)));
            }
        }
        return healthCheckBeans;
    }

    private BeanManager getBeanManager(String appName, String moduleName) {
        String key = appName + "#" + moduleName;
        BeanManager manager = beanManagers.get(key);
        if (manager == null) {
            if (beanManagers.containsKey(key) || cdiService == null) {
                return null;
            }
            manager = cdiService.getCurrentModuleBeanManager();
            beanManagers.put(key, manager);
        }
        return manager;
    }

    @Override
    public void removeModuleReferences(String appName, String moduleName) {
        String key = appName + "#" + moduleName;
        beanManagers.remove(key);
    }
}
