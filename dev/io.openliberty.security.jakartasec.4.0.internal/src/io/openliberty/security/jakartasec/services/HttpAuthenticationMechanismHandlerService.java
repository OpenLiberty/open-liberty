/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.jakartasec.services;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.javaeesec.CDIHelper;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.security.enterprise.AuthenticationException;
import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanismHandler;
import jakarta.security.enterprise.authentication.mechanism.http.HttpMessageContext;

/**
 * Implementation for Jakarta Security 4.0, used by the AuthModule.
 *
 * This HAMHandler Service delegates the its methods to a HAMHandler
 * (part of the Jakarta Security 4.0 specification) which actually determines
 * which *single* HAM (from potentially multiple HAMs available) to use.
 *
 * It works very similar to the IdentityStoreHandler, but that handler
 * gathers and *all* identity stores and fires methods on each, whereas the
 * HAMHandler only uses a single HAM.
 *
 * Jakarta Security 4.0 (appSecurity-6.0) =
 * io.openliberty.security.jakartasec.4.0.internal_1.0.<VER>.jar
 *
 */

public class HttpAuthenticationMechanismHandlerService {

    private static final TraceComponent tc = Tr.register(HttpAuthenticationMechanismHandlerService.class);

    // cache the last logged handler name to avoid duplicate logging
    private static volatile String lastLoggedHandlerName = null;

    /**
     * Logs the handler name only **to the debug output** if it's different
     * from the last logged one.
     *
     * This handles application reloads where the handler instance may change.
     *
     * @param handler is the HttpAuthenticationMechanismHandler to log, cannot be null
     */
    private static void logHandlerToDebugIfChanged(HttpAuthenticationMechanismHandler handler) {
        if (tc.isDebugEnabled()) {
            String currentHandlerName = handler.getClass().getSimpleName().split("\\$")[0];
            // only output if log handler name has changed
            if (!currentHandlerName.equals(lastLoggedHandlerName)) {
                Tr.debug(tc, "The HttpAuthenticationMechanismHandler being used is: " + currentHandlerName);
                lastLoggedHandlerName = currentHandlerName;
            }
        }
    }

    /**
     * Here to reduce repeated identical code and in the case we want to do something
     * different when a handler is not found such as throw an exception.
     */
    private static AuthenticationStatus handleAuthMechNotFound() {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, """
                            An HttpAuthenticationMechanismHandler was not found within the application's bean manager or the CDI
                            (or if it was found in the CDI, the handler was ambiguous or unsatisfied and couldn't be used).""");
        }

        return AuthenticationStatus.NOT_DONE;
    }

    /**
     * Execute the validate request of the http authentication mechanism handler.
     * The handler will fetch a single HAM and execute the functionality directly on the HAM.
     *
     * @param httpMessageContext is the calculated Http context
     * @param authMech           ** Not used **
     * @return the status of the secure response
     * @throws AuthenticationException raised by the auth mechanism
     */
    public static AuthenticationStatus validateRequest(HttpMessageContext httpMessageContext,
                                                       HttpAuthenticationMechanism authMech) throws AuthenticationException {
        AuthenticationStatus authenticationStatus = AuthenticationStatus.NOT_DONE;
        CDI<?> cdi = getCDI();
        HttpAuthenticationMechanismHandler httpAuthenticationMechanismHandler = getHttpAuthenticationMechanismHandler(cdi);
        if (httpAuthenticationMechanismHandler != null) {
            logHandlerToDebugIfChanged(httpAuthenticationMechanismHandler);
            authenticationStatus = httpAuthenticationMechanismHandler.validateRequest(httpMessageContext.getRequest(),
                                                                                      httpMessageContext.getResponse(),
                                                                                      httpMessageContext);
        } else {
            return handleAuthMechNotFound();
        }

        return authenticationStatus;
    }

    /**
     * Execute the secure response functionality of the http authentication mechanism handler.
     * The handler will fetch a single HAM and execute the functionality directly on the HAM.
     *
     * @param httpMessageContext is the calculated Http context
     * @param authMech           ** Not used **
     * @return the status of the secure response
     * @throws AuthenticationException raised by the auth mechanism
     */
    public static AuthenticationStatus secureResponse(HttpMessageContext httpMessageContext,
                                                      HttpAuthenticationMechanism authMech) throws AuthenticationException {
        AuthenticationStatus authenticationStatus = AuthenticationStatus.NOT_DONE;
        CDI<?> cdi = getCDI();
        HttpAuthenticationMechanismHandler httpAuthenticationMechanismHandler = getHttpAuthenticationMechanismHandler(cdi);
        if (httpAuthenticationMechanismHandler != null) {
            logHandlerToDebugIfChanged(httpAuthenticationMechanismHandler);
            authenticationStatus = httpAuthenticationMechanismHandler.secureResponse(httpMessageContext.getRequest(),
                                                                                     httpMessageContext.getResponse(),
                                                                                     httpMessageContext);
        } else {
            return handleAuthMechNotFound();
        }

        return authenticationStatus;
    }

    /**
     * Execute the clean subject functionality of the http authentication mechanism handler.
     * The handler will fetch a single HAM and execute the functionality directly on the HAM.
     *
     * NOTE: Unlike validateRequest() and secureResponse(), the http authentication mechanism
     * handler does not throw any exceptions, hence none thrown here.
     *
     * @param httpMessageContext is the calculated Http context
     * @param authMech           ** Not used **
     * @return the status of the secure response
     */

    public static void cleanSubject(HttpMessageContext httpMessageContext, HttpAuthenticationMechanism authMech) {
        CDI<?> cdi = getCDI();
        HttpAuthenticationMechanismHandler httpAuthenticationMechanismHandler = getHttpAuthenticationMechanismHandler(cdi);
        if (httpAuthenticationMechanismHandler != null) {
            logHandlerToDebugIfChanged(httpAuthenticationMechanismHandler);
            httpAuthenticationMechanismHandler.cleanSubject(httpMessageContext.getRequest(),
                                                            httpMessageContext.getResponse(),
                                                            httpMessageContext);
        } else {
            handleAuthMechNotFound(); // ignore return value not used in this flow
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static HttpAuthenticationMechanismHandler getHttpAuthenticationMechanismHandler(CDI cdi) {

        HttpAuthenticationMechanismHandler httpAuthenticationMechanismHandler = null;
        Instance<HttpAuthenticationMechanismHandler> httpAuthenticationMechanismHandlerInstance = cdi.select(HttpAuthenticationMechanismHandler.class);
        if (httpAuthenticationMechanismHandlerInstance.isUnsatisfied() == false && httpAuthenticationMechanismHandlerInstance.isAmbiguous() == false) {
            httpAuthenticationMechanismHandler = httpAuthenticationMechanismHandlerInstance.get();
        }
        // if the ham is from the extension, then the httpAuthenticationMechanismHandler from the application needs to be found using the app's bean manager.
        if (httpAuthenticationMechanismHandler == null && cdi.getBeanManager().equals(CDIHelper.getBeanManager()) == false) {
            httpAuthenticationMechanismHandler = (HttpAuthenticationMechanismHandler) CDIHelper.getBeanFromCurrentModule(HttpAuthenticationMechanismHandler.class);
        }
        return httpAuthenticationMechanismHandler;
    }

    /**
     * Gets the CDI instance.
     * This method can be overridden for testing.
     *
     * @return The CDI instance
     */
    protected static CDI<?> getCDI() {
        return CDI.current();
    }
}
