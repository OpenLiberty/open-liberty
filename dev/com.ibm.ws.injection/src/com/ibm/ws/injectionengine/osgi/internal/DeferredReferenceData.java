/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injectionengine.osgi.internal;

import java.io.PrintWriter;

import com.ibm.wsspi.injectionengine.InjectionException;

/**
 * Represents a set of reference data that can be processed just-in-time to
 * provide non-java:comp references.
 */
public interface DeferredReferenceData {
    /**
     * Processes any deferred reference data.
     *
     * @return true if any reference data was successfully processed
     * @throws InjectionException
     */
    boolean processDeferredReferenceData() throws InjectionException;

    /**
     * Method gets called when dump gets executed on a server, it will
     * traverse through the DeferredReferenceData and output useful data
     * that can help the user understand the current configuration of
     * the Java: Namespace
     *
     * @param writer the writer used to output
     * @param indent a String containing single or multiples "\t" for indenting purposes
     */
    void introspectDeferredReferenceData(PrintWriter writer, String indent);
}
