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
package com.ibm.ws.microprofile.reactive.streams.test.context;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;

import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * A bean that does things which require the correct thread context
 */
@ApplicationScoped
public class ThreadContextBean {

    @Inject
    ConfigInjectedBean configBean;

    public String getConfigValueFromInjectedBean() {
        return configBean.getValue();
    }

    public BeanManager getBeanManagerViaJndi() {
        try {
            BeanManager bm = (BeanManager) new InitialContext().lookup("java:comp/BeanManager");
            if (bm != null) {
                // If we have a bean manager, check it actually has our beans in it
                Set<?> beans = bm.getBeans(ThreadContextBean.class);
                assertThat("Bean Manager does not know about expected beans", beans, not(empty()));
            }
            return bm;
        } catch (NamingException ex) {
            throw new RuntimeException(ex);
        }
    }

    public CDI<Object> getCdi() {
        return CDI.current();
    }

    public Class<?> loadClassWithTccl() {
        try {
            Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(ThreadContextBean.class.getName());
            return clazz;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
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
