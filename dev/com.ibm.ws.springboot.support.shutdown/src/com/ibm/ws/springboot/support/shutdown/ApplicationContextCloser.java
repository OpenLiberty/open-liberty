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
package com.ibm.ws.springboot.support.shutdown;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.env.ConfigurableEnvironment;

import com.ibm.ws.app.manager.springboot.container.SpringBootConfigFactory;

public class ApplicationContextCloser implements EnvironmentPostProcessor {
    private static final Object token = new Object() {};

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication app) {
        app.addInitializers(new AppContextInitializer());
    }

    private static class AppContextInitializer implements ApplicationContextInitializer, ApplicationListener {

        final SpringBootConfigFactory factory = SpringBootConfigFactory.findFactory(token);

        @Override
        public void initialize(ConfigurableApplicationContext c) {
            factory.addShutdownHook(new Runnable() {
                @Override
                public void run() {
                    c.close();
                }
            });
            c.addApplicationListener(this);
        }

        @Override
        public void onApplicationEvent(ApplicationEvent e) {
            if (e instanceof ContextClosedEvent) {
                factory.rootContextClosed();
            } else if (e instanceof ApplicationReadyEvent) {
                factory.getApplicationReadyLatch().countDown();
            }
        }
    }
}
