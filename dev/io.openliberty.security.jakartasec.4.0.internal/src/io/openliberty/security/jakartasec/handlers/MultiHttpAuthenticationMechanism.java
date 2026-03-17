/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.jakartasec.handlers;

import java.lang.annotation.Annotation;
import java.util.Collections;

import jakarta.annotation.Priority;
import jakarta.security.enterprise.AuthenticationException;
import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import jakarta.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Add Multi HAM specific functionality to the HttpAuthenticationMechanism (Form, Basic, etc ...)
 * to augment (decorate) existing functionality.
 *
 * Only a single implementation/subclass of this Decorator pattern, so no need to make this class
 * abstract and then extend it as we only have a single concrete subclass.
 */
public class MultiHttpAuthenticationMechanism implements HttpAuthenticationMechanism {

    private HttpAuthenticationMechanism wrappedHttpAuthenticationMechanism;
    private final String cachedSimpleName;

    // ensure only the single-arg constructor can be used
    @SuppressWarnings("unused")
    private MultiHttpAuthenticationMechanism() {
        cachedSimpleName = null;
    }

    public MultiHttpAuthenticationMechanism(HttpAuthenticationMechanism wrappedHttpAuthenticationMechanism) {
        this.wrappedHttpAuthenticationMechanism = wrappedHttpAuthenticationMechanism;
        this.cachedSimpleName = calculateSimpleName();
    }

    // interface methods from HttpAuthenticationMechanism to wrap
    @Override
    public AuthenticationStatus validateRequest(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
                                                HttpMessageContext httpMessageContext) throws AuthenticationException {
        return wrappedHttpAuthenticationMechanism.validateRequest(httpServletRequest, httpServletResponse, httpMessageContext);
    }

    @Override
    public AuthenticationStatus secureResponse(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
                                               HttpMessageContext httpMessageContext) throws AuthenticationException {
        return wrappedHttpAuthenticationMechanism.secureResponse(httpServletRequest, httpServletResponse, httpMessageContext);
    }

    @Override
    public void cleanSubject(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
                             HttpMessageContext httpMessageContext) {
        wrappedHttpAuthenticationMechanism.cleanSubject(httpServletRequest, httpServletResponse, httpMessageContext);
    }

    /**
     * Get just the class name, minus the package, taking into account Proxies
     * wrapping packages and class names.
     *
     * i.e. if the object class name is "com.foo.bar.foobar$23729$Proxy$_$$_WeldClientProxy",
     * returned would be "foobar".
     *
     * @return a string with the class name only, or null if mechanism was null.
     */

    private String calculateSimpleName() {
        if (wrappedHttpAuthenticationMechanism != null) {
            String className = wrappedHttpAuthenticationMechanism.getClass().getSimpleName();
            // Handle proxy classes that contain '$'
            return className.split("\\$")[0];
        }
        return null;
    }

    // new methods to decorate/mix-in for multi HAM purposes

    protected String getSimpleName() {
        return cachedSimpleName;
    }

    /**
     * Gets the priority value for an authentication mechanism.
     *
     * For in-built http authentication mechanisms, then there is a priority order
     * defined by static class variable hamClassPriorities.
     *
     * For custom http authentication mechanisms, these should come above any in-built one,
     * and if there are more than one custom http authentication mechanism, then
     * the priority is used.
     *
     * @return The priority value (default is one greater than in-built ones to
     *         ensure they are chosen over those).
     */

    protected int getPriority() {
        if (wrappedHttpAuthenticationMechanism != null) {
            String simpleName = getSimpleName();
            Integer hamPriority = HttpAuthenticationMechanismHandlerImpl.hamClassPriorities.get(simpleName);
            // first check for an in-built (i.e. Basic/Form/etc ...) HAM and get the fixed priorities
            if (hamPriority != null) {
                return hamPriority;
            }

            // must be an application HAM so extract @Priority value
            Priority priority = getAnnotation(Priority.class);
            if (priority != null) {
                return priority.value();
            }
        }

        // if no priority for application HAM, ensure it is greater than the built-in HAMs
        // this could lead to two application HAMs having the same priority if not
        //   explicitly specified, but a clear error message will show this situation,
        //   and the user can set @Priority on the HAMs, or write their own custom HAM handler.
        return (Collections.max(HttpAuthenticationMechanismHandlerImpl.hamClassPriorities.values())) + 1;
    }

    /**
     * Generic(ish) function to return an annotation for a given annotation class.
     * Looks up the class hierarchy to find the annotation in case the initial class
     * is wrapped in a Proxy or similar.
     *
     * @param annotationClass is the annotated class to get the annotation for (i.e. Priority.class)
     * @return the annotation itself, or null if not found.
     */

    private <A extends Annotation> A getAnnotation(final Class<A> annotationClass) {
        if (wrappedHttpAuthenticationMechanism != null) {
            Class<?> classType = wrappedHttpAuthenticationMechanism.getClass();
            while (!classType.getName().equals(Object.class.getName())) {
                if (classType.isAnnotationPresent(annotationClass)) {
                    return classType.getAnnotation(annotationClass);
                }
                classType = classType.getSuperclass();
            }
        }
        return null;
    }
}
