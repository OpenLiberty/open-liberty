/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
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
    }

}
