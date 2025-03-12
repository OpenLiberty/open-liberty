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

import static io.openliberty.classloading.sharedclasses.fat.FATSuite.SHARED_CLASSES_EAR_PATH;

import javax.servlet.annotation.WebServlet;

import componenttest.app.FATServlet;

@WebServlet(SHARED_CLASSES_EAR_PATH)
public class TestSharedClassesEar extends FATServlet{
    private static final long serialVersionUID = 1L;

    public void testWarClassesA() {
        new io.openliberty.classloading.sharedclasses.war.a.A().toString();
    }

    public void testWarClassesB() {
        new io.openliberty.classloading.sharedclasses.war.b.B().toString();
    }

    public void testWarLibA() {
        new io.openliberty.classloading.sharedclasses.warlib.a.A().toString();
    }

    public void testWarLibB() {
        new io.openliberty.classloading.sharedclasses.warlib.b.B().toString();
    }

    public void testEjbClassesA() throws Exception {
        new io.openliberty.classloading.sharedclasses.ejb.a.A().toString();
    }

    public void testEjbClassesB() throws Exception {
        new io.openliberty.classloading.sharedclasses.ejb.b.B().toString();
    }

    public void testEarLibA() throws Exception {
        new io.openliberty.classloading.sharedclasses.earlib.a.A().toString();
    }

    public void testEarLibB() throws Exception {
        new io.openliberty.classloading.sharedclasses.earlib.b.B().toString();
    }

    public void testResoureAdaptorClassesA() throws Exception {
        new io.openliberty.classloading.sharedclasses.resourceadaptor.a.A().toString();
    }

    public void testResoureAdaptorClassesB() throws Exception {
        new io.openliberty.classloading.sharedclasses.resourceadaptor.b.B().toString();
    }

    public void testRarClassesA() throws Exception {
        new io.openliberty.classloading.sharedclasses.rar.a.A().toString();
    }

    public void testRarClassesB() throws Exception {
        new io.openliberty.classloading.sharedclasses.rar.b.B().toString();
    }
}
