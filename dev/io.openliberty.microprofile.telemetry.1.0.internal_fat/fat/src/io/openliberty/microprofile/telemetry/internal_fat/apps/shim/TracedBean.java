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
package io.openliberty.microprofile.telemetry.internal_fat.apps.shim;

import javax.enterprise.context.ApplicationScoped;

import io.opentelemetry.instrumentation.annotations.WithSpan;

import io.opentracing.Span;
import io.opentracing.Tracer;

import org.junit.Assert;

/**
 * POJO to test Traced annotation.
 */
@ApplicationScoped
public class TracedBean {
    /**
     * Method that we expect to be Traced implicitly.
     */
    @WithSpan
    public void annotatedClassMethodImplicitlyTraced(Tracer tracer) {
        System.out.println("Called annotatedClassMethodImplicitlyTraced");
        Span span = tracer.activeSpan();
        Assert.assertNotNull(span);
    }

}
