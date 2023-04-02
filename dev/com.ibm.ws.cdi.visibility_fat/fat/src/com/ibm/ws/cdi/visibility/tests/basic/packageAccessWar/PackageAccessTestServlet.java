/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.visibility.tests.basic.packageAccessWar;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.cdi.visibility.tests.basic.packageAccessWar.bean.MyExecutor;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * Servlet implementation class PackageAccessTestServlet
 */
@WebServlet("/run")
public class PackageAccessTestServlet extends FATServlet {
    private static final long serialVersionUID = 1L;

    @Inject
    MyExecutor executor;

    @Test
    @Mode(TestMode.LITE)
    public void testPublicMethod() {
        executor.testPublicMethod();
    }

    @Test
    @Mode(TestMode.LITE)
    public void testPackageMethod() {
        executor.testPackageMethod();
    }

}
