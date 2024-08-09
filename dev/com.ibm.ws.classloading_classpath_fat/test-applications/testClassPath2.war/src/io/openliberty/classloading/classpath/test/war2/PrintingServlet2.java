/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.classloading.classpath.test.war2;

import static io.openliberty.classloading.classpath.util.PrintingUtils.printClassPathStuff;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

/**
 *
 */
@WebServlet(urlPatterns = "/print", loadOnStartup = 1)
public class PrintingServlet2 extends HttpServlet {
    @Override
    public void init() throws ServletException {
        printClassPathStuff(getClass());
    }
}
