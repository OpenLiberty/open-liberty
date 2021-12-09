/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.restfulWS.internal.logging.filter;

import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

/**
 * Determines whether or not to apply the {@link LoggingFilter}.
 */
@Provider
public class LoggingFilterFeature implements DynamicFeature {

    private static final String WILDCARD = ".*";
    private static final String FILTER_TERM = AccessController.doPrivileged((PrivilegedAction<String>) () -> 
        System.getProperty("io.openliberty.restfulWS.logging.filter.regex", WILDCARD));


    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext featureContext) {
        //TODO: determine via config property whether to register the filter or not
        if (FILTER_TERM.equals(WILDCARD)
            || (resourceInfo.getClass() + "#" + resourceInfo.getResourceMethod()).matches(FILTER_TERM)) {

            featureContext.register(LoggingFilter.class);
        }
    }
}