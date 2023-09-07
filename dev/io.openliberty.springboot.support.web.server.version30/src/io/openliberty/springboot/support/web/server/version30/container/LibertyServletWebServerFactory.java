/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package io.openliberty.springboot.support.web.server.version30.container;

import org.springframework.beans.BeansException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.server.AbstractServletWebServerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import io.openliberty.checkpoint.spi.CheckpointPhase;

/**
 *
 */
@ConfigurationProperties(prefix = "server.liberty", ignoreUnknownFields = true)
public class LibertyServletWebServerFactory extends AbstractServletWebServerFactory implements ApplicationContextAware, LibertyFactoryBase {
    private boolean useDefaultHost = true;
    private ApplicationContext context;

    @Override
    public WebServer getWebServer(ServletContextInitializer... initializers) {
        CheckpointPhase phase = CheckpointPhase.getPhase();
        if (!phase.restored()) {
            // for InstantOn we create a wrapper so that we can recreate the LibertyWebServer on restart
            return new WebServer() {
                LibertyWebServer webServer = new LibertyWebServer(LibertyServletWebServerFactory.this, LibertyServletWebServerFactory.this, mergeInitializers(initializers));

                @Override
                public synchronized void start() throws WebServerException {
                    if (webServer == null) {
                        webServer = new LibertyWebServer(LibertyServletWebServerFactory.this, LibertyServletWebServerFactory.this, mergeInitializers(initializers));;
                    }
                    webServer.start();
                }

                @Override
                public synchronized void stop() throws WebServerException {
                    if (webServer != null) {
                        webServer.stop();
                        webServer = null;
                    }
                }

                @Override
                public synchronized int getPort() {
                    return webServer != null ? webServer.getPort() : 0;
                }
            };
        } else {
            return new LibertyWebServer(this, this, mergeInitializers(initializers));
        }
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

    @Override
    public boolean shouldUseDefaultHost(LibertyWebServer container) {
        // only use default host if configured to and
        // we can acquire the default host
        return useDefaultHost && acquireDefaultHost(container);
    }
}
