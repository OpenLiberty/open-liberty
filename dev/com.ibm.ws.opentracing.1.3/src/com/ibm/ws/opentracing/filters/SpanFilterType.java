/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.opentracing.filters;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Bit flags representing types of requests that filters apply to.
 */
public enum SpanFilterType {
    /**
     * Filter applies to incoming requests.
     */
    INCOMING(SpanFilter.SPAN_TYPE_INCOMING),

    /**
     * Filter applies to outgoing requests.
     */
    OUTGOING(SpanFilter.SPAN_TYPE_OUTGOING),

    /**
     * Filter applies to incoming or outgoing requests.
     */
    BOTH(SpanFilter.SPAN_TYPE_INCOMING | SpanFilter.SPAN_TYPE_OUTGOING);

    private final int value;

    /**
     * Create a filter type.
     *
     * @param value Bit flags of this type.
     */
    SpanFilterType(int value) {
        this.value = value;
    }

    /**
     * Return the bit flags of this type.
     *
     * @return Bit flags of this type.
     */
    @Trivial
    public int getValue() {
        return value;
    }
}
