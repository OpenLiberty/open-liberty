/*******************************************************************************
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
 *******************************************************************************/
package val31.web;

import javax.servlet.annotation.WebServlet;

import org.junit.Assert;
import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/Validation31TestServlet")
public class Validation31TestServlet extends FATServlet {

    @Test
    public void basicTest() throws Exception {
        System.out.println("Test is running in an HttpServlet");
        Assert.assertTrue("Can also use JUnit assertions", true);
    }

}
