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
package com.ibm.ws.concurrent.mp;

import org.eclipse.microprofile.concurrent.ThreadContext;
import org.eclipse.microprofile.concurrent.ThreadContextBuilder;

/**
 * Builder that programmatically configures and creates ThreadContext instances.
 */
class ThreadContextBuilderImpl implements ThreadContextBuilder {
    private String[] cleared;
    private String[] propagated;
    private String[] unchanged;

    @Override
    public ThreadContext build() {
        return new ThreadContextImpl(cleared, propagated, unchanged); // TODO
    }

    // TODO @Override
    public ThreadContextBuilder cleared(String... types) {
        // TODO
        return this;
    }

    @Override
    public ThreadContextBuilder propagated(String... types) {
        // TODO
        return this;
    }

    @Override
    public ThreadContextBuilder unchanged(String... types) {
        // TODO
        return this;
    }
}