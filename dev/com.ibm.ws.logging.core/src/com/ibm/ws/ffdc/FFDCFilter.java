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

package com.ibm.ws.ffdc;

/**
 * This class provides static methods to write first failure data capture (FFDC) records
 * to assist in debugging problems. When an unexpected exception is caught, the processException methods
 * can be used to record the exception stack.
 * For example:
 * <pre>
 * <code>
 * catch (Exception x){
 * FFDCFilter.processException(x, getClass().getName(), "129", this);
 * throw x;
 * }
 * </code>
 * </pre>
 * The source and probe identifiers combine to provide the unique
 * location in the source code where the FFDC record originated. Objects can also be passed for introspection
 * that will also be included in the FFDC record. By default, the FFDC files are written to the
 * ${server.output.dir}/logs/ffdc directory.
 */
public final class FFDCFilter {
    /**
     * Write a first failure data capture record for the provided throwable
     * 
     * @param th
     *            The throwable
     * @param sourceId
     *            An identifier for the source of this record, for example the package and class name
     * @param probeId
     *            A unique identifier within the source of this record, for example the source file line number
     */
    public static void processException(Throwable th, String sourceId, String probeId) {
        FFDCConfigurator.getDelegate().processException(th, sourceId, probeId);
    }

    /**
     * Write a first failure data capture record for the provided throwable
     * 
     * @param th
     *            The throwable
     * @param sourceId
     *            An identifier for the source of this record, for example the package and class name
     * @param probeId
     *            A unique identifier within the source of this record, for example the source file line number
     * @param callerThis
     *            The object making this call, which will be introspected for inclusion in the FFDC record
     */
    public static void processException(Throwable th, String sourceId, String probeId, Object callerThis) {
        FFDCConfigurator.getDelegate().processException(th, sourceId, probeId, callerThis);
    }

    /**
     * Write a first failure data capture record for the provided throwable
     * 
     * @param th
     *            The throwable
     * @param sourceId
     *            An identifier for the source of this record, for example the package and class name
     * @param probeId
     *            A unique identifier within the source of this record, for example the source file line number
     * @param objectArray
     *            An array of objects which will be introspected for inclusion in the FFDC record
     */
    public static void processException(Throwable th, String sourceId, String probeId, Object[] objectArray) {
        FFDCConfigurator.getDelegate().processException(th, sourceId, probeId, objectArray);
    }

    /**
     * Write a first failure data capture record for the provided throwable
     * 
     * @param th
     *            The throwable
     * @param sourceId
     *            An identifier for the source of this record, for example the package and class name
     * @param probeId
     *            A unique identifier within the source of this record, for example the source file line number
     * @param callerThis
     *            The object making this call, which will be introspected for inclusion in the FFDC record
     * @param objectArray
     *            An array of objects which will be introspected for inclusion in the FFDC record
     */
    public static void processException(Throwable th, String sourceId, String probeId, Object callerThis, Object[] objectArray) {
        FFDCConfigurator.getDelegate().processException(th, sourceId, probeId, callerThis, objectArray);
    }

    /**
     * Static-only method invocation, prevent instantiation by blocking the
     * default ctor
     */
    protected FFDCFilter() {}
}
