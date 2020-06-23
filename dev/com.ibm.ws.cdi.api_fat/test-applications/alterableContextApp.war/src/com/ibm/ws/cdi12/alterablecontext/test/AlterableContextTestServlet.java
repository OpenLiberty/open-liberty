/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.cdi12.alterablecontext.test;

import static org.junit.Assert.fail;

import java.util.List;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

import com.ibm.ws.cdi12.alterablecontext.test.extension.DirtySingleton;

import componenttest.app.FATServlet;

@WebServlet("/")
public class AlterableContextTestServlet extends FATServlet {

    private static final long serialVersionUID = 8549700799591343964L;
    private static String FIRST = "I got this from my alterablecontext: com.ibm.ws.cdi12.alterablecontext.test.extension.AlterableContextBean";
    private static String SECOND = "Now the command returns: null";

    @Test
    public void testBeanWasFound(HttpServletRequest request, HttpServletResponse response) throws Exception {
        List<String> strings = DirtySingleton.getStrings();
        assertContains(strings, FIRST);
    }

    @Test
    public void testBeanWasDestroyed() throws Exception {
        List<String> strings = DirtySingleton.getStrings();
        assertContains(strings, SECOND);
    }

    private static void assertContains(List<String> actual, String expected) {
        for (String s : actual) {
            if (s.contains(expected)) {
                return;
            }
        }
        fail("String not found: " + expected);
    }

}
