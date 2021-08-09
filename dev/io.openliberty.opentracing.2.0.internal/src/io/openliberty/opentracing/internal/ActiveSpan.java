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
package io.openliberty.opentracing.internal;

import io.opentracing.Scope;
import io.opentracing.Span;

/**
 *
 */
public class ActiveSpan {

    private final Span span;
    private final Scope scope;

    public ActiveSpan(Span span, Scope scope) {
        this.span = span;
        this.scope = scope;
    }

    /**
     * @return the span
     */
    public Span getSpan() {
        return span;
    }

    /**
     * @return the scope
     */
    public Scope getScope() {
        return scope;
    }

}
