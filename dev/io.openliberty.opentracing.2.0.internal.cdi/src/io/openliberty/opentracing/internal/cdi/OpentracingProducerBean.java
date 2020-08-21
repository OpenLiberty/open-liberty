/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.opentracing.internal.cdi;

import javax.enterprise.inject.Produces;

import io.openliberty.opentracing.internal.OpentracingTracerManager;
import io.opentracing.Tracer;

public class OpentracingProducerBean {
    @Produces
    public Tracer getTracer() {
        return OpentracingTracerManager.getTracer();
    }
}
