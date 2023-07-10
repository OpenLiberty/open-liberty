/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import javax.servlet.annotation.MultipartConfig;

@MultipartConfig(fileSizeThreshold = 1000000, location = "c:/temp", maxFileSize = 5000000, maxRequestSize = 5000000)
/**
 * Form Login Servlet
 */
public class FormLoginServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    public FormLoginServlet() {
        super("FormLoginServlet");
        System.out.println("FormLoginServlet has been called.");
    }

}
