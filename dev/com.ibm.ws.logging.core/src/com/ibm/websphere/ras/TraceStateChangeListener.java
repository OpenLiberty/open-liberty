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
 * The TraceStateChangeListener interface is provided to allow other logging
 * packages that utilize the systems management and TraceComponent aspects of Tr
 * to be informed when the trace state changes.
 * 
 * Typically a logging package that is implemented on top of Tr will map a trace
 * domain to a Tr <code>TraceComponent</code> and will map that packages trace
 * types to the existing Tr types. JRas for example maps a Jras Logger to a
 * Trace component using a common name. Therefore JRas Loggers will show up in
 * the systems management GUI as trace objects that are indistinguishable from
 * normal Tr TraceComponents. When the user enables trace for a JRas logger, the
 * logger must be called back to translate the Tr trace enable event to the JRas
 * trace type and set the JRas loggers trace mask accordingly.
 */
public interface TraceStateChangeListener {
    /**
     * Inform the object implementing this interface that the Tr trace state
     * managed by the TraceComponent object has changed
     */
    public void traceStateChanged();
}
