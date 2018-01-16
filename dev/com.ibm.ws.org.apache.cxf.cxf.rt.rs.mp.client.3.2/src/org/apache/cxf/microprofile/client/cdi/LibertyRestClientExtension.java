/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.apache.cxf.microprofile.client.cdi;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;

import org.apache.cxf.microprofile.client.cdi.RestClientBean;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.cdi.extension.WebSphereCDIExtension;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
immediate = true,
property = { "api.classes=" +
             "org.eclipse.microprofile.rest.client.inject.RegisterRestClient;" +
             "org.eclipse.microprofile.rest.client.inject.RestClient;",
             "service.vendor=IBM"
})
public class LibertyRestClientExtension implements WebSphereCDIExtension, Extension {
    private static Set<Class<?>> restClientClasses = new LinkedHashSet<>();
    private static Set<Throwable> errors = new LinkedHashSet<>();
    public void findClients(@Observes @WithAnnotations({RegisterRestClient.class}) ProcessAnnotatedType<?> pat) {
        Class<?> restClient = pat.getAnnotatedType().getJavaClass();
        if (restClient.isInterface()) {
            restClientClasses.add(restClient);
            pat.veto();
        } else {
            errors.add(new IllegalArgumentException("The class " + restClient
                    + " is not an interface"));
        }
    }

    public void registerClientBeans(@Observes AfterBeanDiscovery afterBeanDiscovery, BeanManager beanManager) {
        restClientClasses.stream().map(c -> new RestClientBean(c, beanManager)).forEach(afterBeanDiscovery::addBean);
    }

    public void registerErrors(@Observes AfterDeploymentValidation afterDeploymentValidation) {
        errors.forEach(afterDeploymentValidation::addDeploymentProblem);
    }
}
