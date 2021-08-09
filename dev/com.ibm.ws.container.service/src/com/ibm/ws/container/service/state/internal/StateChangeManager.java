/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container.service.state.internal;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSet;

/**
 * The manager of state changes for a type of deployed info.
 * 
 * @param <L> the deployed info listener type
 */
abstract class StateChangeManager<L> {
    /**
     * The listeners for this deployed info type.
     */
    protected final ConcurrentServiceReferenceSet<L> listeners;

    StateChangeManager(String listenerRefName) {
        listeners = new ConcurrentServiceReferenceSet<L>(listenerRefName);
    }

    void activate(ComponentContext cc) {
        listeners.activate(cc);
    }

    void deactivate(ComponentContext cc) {
        listeners.deactivate(cc);
    }

    final void addListener(ServiceReference<L> ref) {
        listeners.addReference(ref);
    }

    final void removeListener(ServiceReference<L> ref) {
        listeners.removeReference(ref);
    }
}
