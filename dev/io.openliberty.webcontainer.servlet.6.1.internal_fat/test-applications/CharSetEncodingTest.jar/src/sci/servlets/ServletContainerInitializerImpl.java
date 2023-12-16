/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package sci.servlets;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.logging.Logger;

import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HandlesTypes;

/*
 * SCI set ServletContext.setRequestCharacterEncoding(Charset) , setResponseCharacterEncoding(Charset)
 */
@HandlesTypes(jakarta.servlet.Servlet.class)
public class ServletContainerInitializerImpl implements ServletContainerInitializer {
    private static final String CLASS_NAME = ServletContainerInitializerImpl.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);

    @Override
    public void onStartup(Set<Class<?>> setOfClassesInterestedIn, ServletContext context) throws ServletException {
        LOG.info("ServletContainerInitializerImpl.onStartup ENTER");
        context.setRequestCharacterEncoding(Charset.defaultCharset());
        context.setResponseCharacterEncoding(StandardCharsets.US_ASCII);
        LOG.info("ServletContainerInitializerImpl.onStartup RETURN");
    }
}
