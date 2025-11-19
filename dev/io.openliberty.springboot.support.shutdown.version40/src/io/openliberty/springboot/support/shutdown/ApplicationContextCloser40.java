/*******************************************************************************
 * Copyright (c) 2023, 2025 IBM Corporation and others.
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
package io.openliberty.springboot.support.shutdown;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.env.ConfigurableEnvironment;

import com.ibm.ws.app.manager.springboot.container.SpringBootConfigFactory;

public class ApplicationContextCloser40 implements EnvironmentPostProcessor {
    private static final Object token = new Object() {
    };

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication app) {
        if (env.getPropertySources().contains("bootstrap")) {
            return;
        }
        final SpringBootConfigFactory factory = SpringBootConfigFactory.findFactory(token);
        app.addInitializers((c) -> {
            factory.addShutdownHook(() -> {
                c.close();
            });
            c.addApplicationListener((e) -> {
                if (e instanceof ContextClosedEvent) {
                    factory.rootContextClosed();
                } else if (e instanceof ApplicationReadyEvent) {
                    factory.getApplicationReadyLatch().countDown();
                }
            });
        });
    }
}
