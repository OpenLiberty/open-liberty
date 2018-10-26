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
package com.ibm.ws.concurrent.mp.context;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.concurrent.spi.ThreadContextProvider;
import org.eclipse.microprofile.concurrent.spi.ThreadContextSnapshot;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * This interface allows for the conversion from MicroProfile ThreadContextProvider
 * back to one or more instances of com.ibm.wsspi.threadcontext.ThreadContextProvider.
 */
@Trivial
public abstract class ContainerContextProvider implements ThreadContextProvider {
    public abstract com.ibm.wsspi.threadcontext.ThreadContextProvider[] toContainerProviders();

    @Override
    public ThreadContextSnapshot defaultContext(Map<String, String> props) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ThreadContextSnapshot currentContext(Map<String, String> props) {
        throw new UnsupportedOperationException();
    }

    // TODO remove once default impl provided by spec
    @Override
    public Set<String> getPrerequisites() {
        return Collections.emptySet();
    }
}
