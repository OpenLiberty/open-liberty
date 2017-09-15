package com.ibm.ws.objectManager.utils;

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

/**
 * @author Andrew_Banks
 * 
 *         Make concrete instances of Trace.
 */
public class TraceFactoryImpl
                extends TraceFactory {

    /**
     * @param nls for info tracing.
     */
    public TraceFactoryImpl(NLS nls) {
        super(nls);
    } // TraceFactoryImpl().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.utils.TraceFactory#getTrace(java.lang.Class, java.lang.String)
     */
    public Trace getTrace(Class sourceClass, String traceGroup) {
        return new TraceImpl(sourceClass, traceGroup, this);
    } // getTrace().

} // class TraceFactoryImpl.