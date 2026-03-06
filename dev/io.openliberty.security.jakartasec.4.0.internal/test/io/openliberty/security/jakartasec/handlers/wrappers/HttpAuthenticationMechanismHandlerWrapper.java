/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.jakartasec.handlers.wrappers;

import io.openliberty.security.jakartasec.handlers.HttpAuthenticationMechanismHandlerImpl;
import jakarta.enterprise.inject.spi.CDI;

/**
 * A subclass so of HttpAuthenticationMechanismHandlerImpl so we can override internal methods
 * which return objects/components we can't easily mock.
 */

public class HttpAuthenticationMechanismHandlerWrapper extends HttpAuthenticationMechanismHandlerImpl {

    private final String APPLICATION_NAME = "io.openliberty.security.jakartasec.handlers.HttpAuthenticationMechanismHandlerImplTest";
    private final String MODULE_NAME = "HAMHandlerImplTest.war";

    private CDI<?> cdi;

    @SuppressWarnings("unused")
    private HttpAuthenticationMechanismHandlerWrapper() {
    }

    public HttpAuthenticationMechanismHandlerWrapper(CDI<?> cdi) {
        super();
        this.cdi = cdi;
    }

    @Override
    protected CDI<?> getCDI() {
        return cdi;
    }

    @Override
    protected String getModuleName() {
        return MODULE_NAME;
    }

    @Override
    protected String getApplicationName() {
        // the implementation of getApplicationName() uses ModulePropertiesUtils() which
        //   is very difficult to mock, as it drags in many other classes
        return APPLICATION_NAME;
    }
}
