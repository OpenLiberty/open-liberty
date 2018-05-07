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
package com.ibm.ws.springboot.support.web.server.version15.container;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.BeansException;
import org.springframework.boot.context.embedded.AbstractEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 *
 */
@ConfigurationProperties(prefix = "server.liberty", ignoreUnknownFields = true)
public class LibertyServletContainerFactory extends AbstractEmbeddedServletContainerFactory implements ApplicationContextAware {
    private boolean useDefaultHost = true;
    private ApplicationContext context;
    private final AtomicReference<LibertyServletContainer> usingDefaultHost = new AtomicReference<>();

    @Override
    public EmbeddedServletContainer getEmbeddedServletContainer(ServletContextInitializer... initializers) {
        return new LibertyServletContainer(this, mergeInitializers(initializers));
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = context;
    }

    String getApplicationID() {
        return context.getId();
    }

    public void setUseDefaultHost(boolean useDefaultHost) {
        this.useDefaultHost = useDefaultHost;
    }

    boolean shouldUseDefaultHost(LibertyServletContainer container) {
        // only use default host if configured to and
        // this is the root application context
        return useDefaultHost && context.getParent() == null && usingDefaultHost.compareAndSet(null, container);
    }

    void stopUsingDefaultHost(LibertyServletContainer container) {
        usingDefaultHost.compareAndSet(container, null);
    }
}
