/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.springboot.support.web.server.initializer;

import java.util.function.UnaryOperator;

import javax.servlet.ServletContext;

public final class WebInitializer {
    private final String contextPath;
    private final UnaryOperator<ServletContext> contextInitializer;

    public WebInitializer(String contextPath, UnaryOperator<ServletContext> contextInitializer) {
        super();
        this.contextPath = contextPath;
        this.contextInitializer = contextInitializer;
    }

    public String getContextPath() {
        return contextPath;
    }

    public UnaryOperator<ServletContext> getContextInitializer() {
        return contextInitializer;
    }
}
