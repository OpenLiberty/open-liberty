/*
 * =============================================================================
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * =============================================================================
 */
package com.ibm.ws.jndi.global.fat.data;

/**
 *
 */
public enum ServletName {
    JNDI_TEST_SERVLET("JNDITestServlet"), JNDI_REF_SERVLET("JNDIRefTestServlet"), PARENTLAST_JNDI_SERVLET("ParentLastJndiServlet");

    private final String servletName;

    private ServletName(String servletName) {
        this.servletName = servletName;
    }

    public String getServletName() {
        return servletName;
    }

}
