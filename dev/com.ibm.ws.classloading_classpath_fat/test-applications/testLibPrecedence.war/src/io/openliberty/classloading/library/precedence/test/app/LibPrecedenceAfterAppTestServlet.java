/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.classloading.library.precedence.test.app;

import static io.openliberty.classloading.library.precedence.test.app.AbstractLibPrecedenceTestServlet.ExpectedCodeSource.testBundleAPIJar;

import javax.servlet.annotation.WebServlet;

@WebServlet("/LibPrecedenceAfterAppTestServlet")
public class LibPrecedenceAfterAppTestServlet extends AbstractLibPrecedenceTestServlet {

    public LibPrecedenceAfterAppTestServlet() {
        super(testBundleAPIJar, testBundleAPIJar, testBundleAPIJar, testBundleAPIJar);
    }

    private static final long serialVersionUID = 1L;
}
