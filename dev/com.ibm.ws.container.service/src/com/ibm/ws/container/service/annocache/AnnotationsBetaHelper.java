/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.container.service.annocache;

import java.security.AccessController;
import java.security.PrivilegedAction;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.kernel.security.thread.ThreadIdentityManager;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

public class AnnotationsBetaHelper {
    private static final TraceComponent tc = Tr.register(AnnotationsBetaHelper.class);

    // See "com.ibm.ws.artifact.zip.internal.SystemUtils"

    public static final String SOURCE_DEFAULTED = "defaulted";
    public static final String SOURCE_PROPERTY = "system property";

    /**
     * Run {@link System#getProperty(String)} as a privileged action.
     *
     * @param propertyName The name of the property which is to be retrieved.

     * @return The string property value.  Null if the property is not set.
     */
    public static String getProperty(final String propertyName) {
        Object token = ThreadIdentityManager.runAsServer();
        try {
            return AccessController.doPrivileged( new PrivilegedAction<String>() {
                @Override
                public String run() {
                    return System.getProperty(propertyName);
                }

            } );
        } finally {
            ThreadIdentityManager.reset(token);
        }
    }

    // See: "com.ibm.ws.artifact.zip.cache.ZipCachingProperties"

    @Trivial
    public static boolean getProperty(String methodName, String propertyName, boolean propertyDefaultValue) {
        String propertyText = AnnotationsBetaHelper.getProperty(propertyName);
        boolean propertyValue;
        boolean propertyDefaulted;
        if ( propertyDefaulted = (propertyText == null) ) {
            propertyValue = propertyDefaultValue;
        } else {
            propertyValue = Boolean.parseBoolean(propertyText);
        }
        AnnotationsBetaHelper.debugProperty(methodName, propertyName, propertyValue, propertyDefaulted);
        return propertyValue;
    }

    /**
     * Conditionally log information about a property: If the property was defaulted,
     * emit property information as debugging output.  If the property was explicitly
     * assigned (not default) emit property information as info output.
     *
     * @param methodName The name of the method requesting property logging.
     * @param propertyName The name of the property.
     * @param propertyValue The value of the property.
     * @param defaulted True or false telling if the property value was defaulted.
     */
    @Trivial
    public static void debugProperty(
        String methodName,
        String propertyName, boolean propertyValue, boolean defaulted) {

        if ( !TraceComponent.isAnyTracingEnabled() ) {
            return;
        } else if ( !tc.isDebugEnabled() && (!defaulted || !tc.isInfoEnabled()) ) {
            return;
        }

        String propertyText =
            "Property [ " + propertyName + " ]" +
            " [ " + propertyValue + " ]" +
            " (" + (defaulted ? SOURCE_DEFAULTED : SOURCE_PROPERTY) + ")";

        if ( !defaulted ) {
            Tr.info(tc, methodName, propertyText);
        } else {
            Tr.debug(tc, methodName, propertyText);
        }

        // System.out.println(propertyText);
    }

    //

    public static final String LIBERTY_BETA_PROPERTY_NAME = "anno.beta";
    public static final boolean LIBERTY_BETA_DEFAULT_VALUE = true;
    public static final boolean LIBERTY_BETA;

    @Trivial
    public static boolean getLibertyBeta() {
        return LIBERTY_BETA;
    }

    static {
        String methodName = "<static init>";

        LIBERTY_BETA = getProperty(methodName,
            LIBERTY_BETA_PROPERTY_NAME, LIBERTY_BETA_DEFAULT_VALUE);
    }

    //

    /**
     * Answer module annotations for a container.  Use the caching implementation when enabled
     * for the beta.  Otherwise, use the non-caching implementation.
     *
     * @param container The container for which to answer module annotations.
     *
     * @return Module annotations for the container.
     *
     * @throws UnableToAdaptException Thrown if module annotations could not be obtained.
     */
    public static com.ibm.ws.container.service.annotations.ModuleAnnotations getModuleAnnotations(Container container) 
        throws UnableToAdaptException {

        if ( AnnotationsBetaHelper.getLibertyBeta() ) {
            return container.adapt(com.ibm.ws.container.service.annocache.ModuleAnnotations.class);
        } else {
            return container.adapt(com.ibm.ws.container.service.annotations.ModuleAnnotations.class);
        }
    }

    /**
     * Answer web annotations for a container.  Use the caching implementation when enabled
     * for the beta.  Otherwise, use the non-caching implementation.
     *
     * @param container The container for which to answer web  annotations.
     *
     * @return Web annotations for the container.
     *
     * @throws UnableToAdaptException Thrown if web annotations could not be obtained.
     */
    public static com.ibm.ws.container.service.annotations.WebAnnotations getWebAnnotations(Container container) 
        throws UnableToAdaptException {

        if ( AnnotationsBetaHelper.getLibertyBeta() ) {
            return container.adapt(com.ibm.ws.container.service.annocache.WebAnnotations.class);
        } else {
            return container.adapt(com.ibm.ws.container.service.annotations.WebAnnotations.class);
        }
    }
}
