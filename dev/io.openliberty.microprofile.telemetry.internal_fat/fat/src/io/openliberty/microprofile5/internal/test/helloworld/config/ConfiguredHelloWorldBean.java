/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile5.internal.test.helloworld.config;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.openliberty.microprofile5.internal.test.helloworld.HelloWorldBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.Span;

@ApplicationScoped
public class ConfiguredHelloWorldBean implements HelloWorldBean {

    @Inject
    @ConfigProperty(name = "message")
    private String message;

    @Inject
    Tracer tracer;

    @Inject
    Span span;
    
    @Override
    public String getMessage() {
        span = this.tracer.spanBuilder("Start my use case").startSpan();
        // execute my use case - here we simulate a wait
        doWork();
        span.addEvent("Returning Message" + message);
        span.end();
        return message;
    }

    private void doWork() {
        try {
        Thread.sleep(500);
        span.addEvent("Doing work");
        } catch (InterruptedException e) {
        // do the right thing here
        }
    }
}
