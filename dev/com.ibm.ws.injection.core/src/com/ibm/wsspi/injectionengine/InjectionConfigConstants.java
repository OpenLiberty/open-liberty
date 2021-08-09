/*******************************************************************************
 * Copyright (c) 2006, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.injectionengine;

/**
 * Holds the configuration and property constants for the WebSphere injection component.
 **/
public class InjectionConfigConstants
{
    /**
     * Constant used to specify the injection message file for use in trace.<p>
     * <B>Value:</B>
     * messageFile<p>
     * <B>Usage:</B>
     * Internal injection component constant for trace purposes<p>
     * <B>Property values:</B> <p>
     * "com.ibm.wsspi.injectionengine"<p>
     * <B>Description:</B> <p>
     * This constant is used internally by the code within the WAS.injection component to
     * indicate the message file to be used by the WebSphere RAS component for trace
     * and error reporting.<p>
     *
     * @see com.ibm.websphere.ras.TraceComponent
     **/

    public static final String messageFile = "com.ibm.wsspi.injectionengine.injection"; //448046

    /**
     * Constant used to specify the trace string used in the WAS.injection code.<p>
     * <B>Value:</B>
     * traceString<p>
     * <B>Usage:</B>
     * Internal injection component constant for trace purposes<p>
     * <B>Property values:</B> <p>
     * "InjectionEngine"<p>
     * <B>Description:</B> <p>
     * This constant is used internally by the code within the WAS.injection component to
     * indicate the trace string to be used by the WebSphere RAS component for trace
     * and error reporting.<p>
     *
     * @see com.ibm.websphere.ras.TraceComponent
     **/

    public static final String traceString = "Injection";

    /**
     * Constant used to specify the trace string used in the
     * WAS.injection code for detailed tracing.<p>
     * <B>Value:</B>
     * traceString<p>
     * <B>Usage:</B>
     * Internal injection component constant for detailed
     * trace purposes<p>
     * <B>Property values:</B> <p>
     * "InjectionEngineDetail"<p>
     * <B>Description:</B> <p>
     * This constant is used internally by the code within the WAS.injection component to
     * indicate the trace string to be used by the WebSphere RAS component
     * for detailed trace and error reporting.<p>
     *
     * @see com.ibm.websphere.ras.TraceComponent
     **/

    public static final String detailTraceString = "InjectionDetail";

    /**
     * Property that indicates whether or not container behavior should be
     * compatible with EE5 (EJB 3) rather than later releases (EJB 3.1+). This
     * property should only be checked for those cases where the EE
     * specifications have mandated a change in behavior between releases.
     */
    // d657801
    public static final boolean EE5Compatibility = Boolean.getBoolean("com.ibm.websphere.ejbcontainer.EE5Compatibility");

}
