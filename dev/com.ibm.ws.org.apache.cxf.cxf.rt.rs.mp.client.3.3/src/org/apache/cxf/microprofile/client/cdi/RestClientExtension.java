/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
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

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

public class RestClientExtension implements Extension {
    private Set<Class<?>> restClientClasses = new LinkedHashSet<>(); //Liberty change - removed static
    private Set<Throwable> errors = new LinkedHashSet<>(); //Liberty change - removed static
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

