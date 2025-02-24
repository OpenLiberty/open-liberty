/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package io.openliberty.microprofile.health40.services.impl;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.util.AnnotationLiteral;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;
import org.eclipse.microprofile.health.Startup;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.ws.cdi.CDIService;
import com.ibm.ws.microprofile.health.services.HealthCheckBeanCallException;

import io.openliberty.microprofile.health.internal.common.HealthCheckConstants;
import io.openliberty.microprofile.health40.services.HealthCheck40CDIBeanInvoker;

@Component(service = HealthCheck40CDIBeanInvoker.class, configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM" })
public class HealthCheck40CDIBeanInvokerImpl implements HealthCheck40CDIBeanInvoker {

    private final static TraceComponent tc = Tr.register(io.openliberty.microprofile.health40.services.impl.HealthCheck40CDIBeanInvokerImpl.class);

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
            case HealthCheckConstants.HEALTH_CHECK_START:
                healthCheckBeans = getHealthCheckStartupBeans(beanManager);
                break;
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

    private Set<Object> getHealthCheckStartupBeans(BeanManager beanManager) {
        Set<Object> hcReadinessBeans = new HashSet<Object>();

        hcReadinessBeans = getHealthCheckBeans(beanManager, new AnnotationLiteral<Startup>() {
        });

        return hcReadinessBeans;
    }

    private Set<Object> getHealthCheckLivenessBeans(BeanManager beanManager) {
        Set<Object> hcLivenessBeans = new HashSet<Object>();

        hcLivenessBeans = getHealthCheckBeans(beanManager, new AnnotationLiteral<Liveness>() {
        });

        return hcLivenessBeans;
    }

    private Set<Object> getHealthCheckReadinessBeans(BeanManager beanManager) {
        Set<Object> hcReadinessBeans = new HashSet<Object>();

        hcReadinessBeans = getHealthCheckBeans(beanManager, new AnnotationLiteral<Readiness>() {
        });

        return hcReadinessBeans;
    }

    private Set<Object> getAllHealthCheckBeans(BeanManager beanManager) {
        Set<Object> hcAllBeans = new HashSet<Object>();

        hcAllBeans.addAll(getHealthCheckLivenessBeans(beanManager));
        hcAllBeans.addAll(getHealthCheckReadinessBeans(beanManager));
        hcAllBeans.addAll(getHealthCheckStartupBeans(beanManager));

        return hcAllBeans;
    }

    private Set<Object> getHealthCheckBeans(BeanManager beanManager, Annotation hcQualifier) {
        Set<Object> healthCheckBeans = new HashSet<Object>();
        Set<Bean<?>> beans;
        if (beanManager != null) {
            beans = beanManager.getBeans(HealthCheck.class, hcQualifier);
            Tr.event(tc, "Beans size : " + beans.size());
            for (Bean<?> bean : beans) {
                Set<Annotation> beanQualifiers = bean.getQualifiers();
                Tr.event(tc, "Bean Found: HealthCheck beanClass = " + bean.getBeanClass() + ", class = " + bean.getClass() + ", name = " + bean.getName() + ", qualifers = "
                             + beanQualifiers);
                if (beanQualifiers.contains(hcQualifier)) {
                    Tr.event(tc, "Adding Bean : beanClass = " + bean.getBeanClass() + ", class = " + bean.getClass() + ", qualifers = " + beanQualifiers);
                    healthCheckBeans.add(beanManager.getReference(bean, HealthCheck.class, beanManager.createCreationalContext(bean)));
                }
            }
        }
        return healthCheckBeans;
    }

    private BeanManager getBeanManager(String appName, String moduleName) {
        String key = appName + "#" + moduleName;
        beanManagers.entrySet().forEach(bm -> {
            Tr.event(tc, "BeanManager  : " + bm.getKey() + " " + bm.getValue());
        });
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
