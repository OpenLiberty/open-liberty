/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.ras;

/**
 * Simple change listener interface that is driven as {@link TraceComponent} instances are registered and updated.
 * Register through the {@link TrConfigurator#addTraceComponentListener(TraceComponentChangeListener)} method
 */
public interface TraceComponentChangeListener {

    /**
     * Callback indicating the specified trace component was registered.
     * 
     * @param tc
     *            the {@link TraceComponent} that was registered
     */
    public void traceComponentRegistered(TraceComponent tc);

    /**
     * Callback indicating the specified trace component was updated.
     * 
     * @param tc
     *            the {@link TraceComponent} that was updated
     */
    public void traceComponentUpdated(TraceComponent tc);
}
