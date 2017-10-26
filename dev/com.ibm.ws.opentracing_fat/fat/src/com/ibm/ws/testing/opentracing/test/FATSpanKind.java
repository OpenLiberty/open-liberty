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
package com.ibm.ws.testing.opentracing.test;

/**
 * <p>Enumeration of the types of completed spans which are possible.</p>
 *
 * <p>The span type is expected to be stored using tag {@link #TAG_SPAN_KIND},
 * as either {@link SPAN_KIND_SERVER}, {@link SPAN_KIND_CLIENT}, or unstored
 * for manually created spans.</p>
 */
public enum FATSpanKind {
    SERVER("server"),
    CLIENT("client"),
    MANUAL(null);

    private FATSpanKind(String tagValue) {
        this.tagValue = tagValue;
    }

    private final String tagValue;

    public String getTagValue() {
        return tagValue;
    }

    public boolean matches(String testTagValue) {
        if ( tagValue == null ) {
            return ( testTagValue == null );
        } else {
            return ( tagValue.equals(testTagValue) );
        }
    }
}
