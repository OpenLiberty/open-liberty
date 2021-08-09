/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jp.test;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import jp.test.bean.MyExecutor;

/**
 * Servlet implementation class RunServlet
 */
@WebServlet("/run")
public class RunServlet extends FATServlet {
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
