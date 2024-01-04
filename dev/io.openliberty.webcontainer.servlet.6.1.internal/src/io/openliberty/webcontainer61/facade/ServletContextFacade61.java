/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.webcontainer61.facade;

import java.nio.charset.Charset;

import com.ibm.ws.webcontainer40.facade.ServletContextFacade40;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;

/**
 * Facade wrapping the WebApp when returning a context to the user. This will
 * prevent users from exploiting public methods in WebApp which were intended
 * for internal use only.
 */
public class ServletContextFacade61 extends ServletContextFacade40 {

    public ServletContextFacade61(IServletContext context) {
        super(context);
    }

    /**
     * @see jakarta.servlet.ServletContext#setRequestCharacterEncoding(Charset encoding)
     */
    @Override
    public void setRequestCharacterEncoding(Charset encoding) {
        context.setRequestCharacterEncoding(encoding);
    }

    /**
     * @see jakarta.servlet.ServletContext#setResponseCharacterEncoding(Charset encoding)
     */
    @Override
    public void setResponseCharacterEncoding(Charset encoding) {
        context.setResponseCharacterEncoding(encoding);
    }
}
