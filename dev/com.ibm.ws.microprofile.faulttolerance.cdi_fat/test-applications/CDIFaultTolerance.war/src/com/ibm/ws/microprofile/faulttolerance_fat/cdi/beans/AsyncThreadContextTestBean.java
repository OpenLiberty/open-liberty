/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.Asynchronous;

@ApplicationScoped
@Asynchronous
public class AsyncThreadContextTestBean {

    @Inject
    ConfigInjectedBean configBean;

    public Future<String> getConfigValueFromInjectedBean() {
        return CompletableFuture.completedFuture(configBean.getValue());
    }

    public Future<BeanManager> getBeanManagerViaJndi() throws NamingException {
        BeanManager bm = (BeanManager) new InitialContext().lookup("java:comp/BeanManager");
        if (bm != null) {
            // If we have a bean manager, check it actually has our beans in it
            Set<?> beans = bm.getBeans(AsyncThreadContextTestBean.class);
            assertThat("Bean Manager does not know about expected beans", beans, not(empty()));
        }
        return CompletableFuture.completedFuture(bm);
    }

    public Future<CDI<Object>> getCdi() {
        return CompletableFuture.completedFuture(CDI.current());
    }

    public Future<Class<?>> loadClassWithTccl() throws ClassNotFoundException {
        Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(AsyncThreadContextTestBean.class.getName());
        return CompletableFuture.completedFuture(clazz);
    }

    @ApplicationScoped
    public static class ConfigInjectedBean {
        @Inject
        @ConfigProperty(name = "test/AsyncConfigValue")
        String asyncConfigValue;

        public String getValue() {
            return asyncConfigValue;
        }
    }
}
