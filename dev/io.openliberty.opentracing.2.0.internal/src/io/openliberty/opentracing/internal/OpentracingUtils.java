/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package io.openliberty.opentracing.internal;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

import io.opentracing.Tracer;

public class OpentracingUtils {
    private static final TraceComponent tc = Tr.register(OpentracingUtils.class);

    /**
     * <p>Answer a text string for a tracer for use in trace output.</p>
     *
     * <p>This implementation answers the tracer simple class name plus the
     * tracers hash code.</p>
     *
     * <p>The usual <code>toString</code> value is not used, since that is very
     * large for the mock tracer. Use of the usual <code>toString</code> value
     * would bloat trace output.</p>
     *
     * @param tracer The tracer for which to obtain a text string.
     *
     * @return A text string for the tracer.
     */
    @Trivial
    public static String getTracerText(Tracer tracer) {
        if (tracer == null) {
            return "null";
        } else {
            return tracer.getClass().getSimpleName() + "@" + Integer.toHexString(tracer.hashCode());
        }
    }

    //

    private static final String DEFAULT_SERVICE_NAME = "com.ibm.ws.opentracing.defaultServiceName";

    /**
     * <p>Answer the name of the current application name.</p>
     *
     * <p>For use when retrieving tracers, for which exactly one exists
     * per application.</p>
     *
     * @return The current application name. {@link #DEFAULT_SERVICE_NAME} if
     *         the name cannot be retrieved by doing a context lookup.
     */
    @Trivial
    public static String lookupAppNameFallback() {
        String appName;
        try {
            appName = (String) new InitialContext().lookup("java:app/AppName");
        } catch (NamingException e) {
            Tr.error(tc, "OPENTRACING_NO_APPNAME_FOUND_IN_JNDI"); // Should never happen
            appName = DEFAULT_SERVICE_NAME;
        }
        return appName;
    }

    /**
     * This is a more efficient way to obtain the current application name.
     *
     * @return The current application name.
     */
    @Trivial
    public static String lookupAppName() {
        String appName = null;
        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        if (cmd != null) {
            appName = cmd.getModuleMetaData().getApplicationMetaData().getName();
        }
        if (appName == null) {
            appName = lookupAppNameFallback();
        }
        return appName;
    }

}
