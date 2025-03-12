/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.classloading.sharedclasses.war;

import static io.openliberty.classloading.sharedclasses.fat.FATSuite.SHARED_CLASSES_SERVER_LIB_PATH;

import javax.servlet.annotation.WebServlet;

import componenttest.app.FATServlet;

@WebServlet(SHARED_CLASSES_SERVER_LIB_PATH)
public class TestSharedClassesServerLib extends FATServlet{
    private static final long serialVersionUID = 1L;

    public void testServerLibClassesA() {
        new io.openliberty.classloading.sharedclasses.serverlib.a.A().toString();
    }

    public void testServerLibClassesB() {
        new io.openliberty.classloading.sharedclasses.serverlib.b.B().toString();
    }

}
