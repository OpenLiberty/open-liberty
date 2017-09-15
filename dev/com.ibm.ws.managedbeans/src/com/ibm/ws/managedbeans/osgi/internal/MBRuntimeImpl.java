/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.managedbeans.osgi.internal;

import org.osgi.service.component.ComponentContext;

import com.ibm.ws.ejbcontainer.osgi.ManagedBeanRuntime;

/**
 * Provides the managed bean runtime environment which enables managed beans
 * in the core container.
 */
public class MBRuntimeImpl implements ManagedBeanRuntime {

    public void activate(ComponentContext cc) {
        // Nothing currently needs to be done during service activation,
        // just the presence of this service enables managed beans.
    }

    public void deactivate(ComponentContext cc) {}
}
