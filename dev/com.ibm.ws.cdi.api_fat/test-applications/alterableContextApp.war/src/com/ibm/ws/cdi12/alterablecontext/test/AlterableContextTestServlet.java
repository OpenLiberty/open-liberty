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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;

import java.util.List;

import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.cdi12.alterablecontext.test.extension.DirtySingleton;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@WebServlet("/")
public class AlterableContextTestServlet extends FATServlet {

    private static final long serialVersionUID = 8549700799591343964L;
    private static String FIRST = "I got this from my alterablecontext: com.ibm.ws.cdi12.alterablecontext.test.extension.AlterableContextBean";
    private static String SECOND = "Now the command returns: null";

    @Test
    @Mode(TestMode.FULL)
    public void testBeanWasFound() throws Exception {
        List<String> strings = DirtySingleton.getStrings();
        assertThat(strings, hasItem(containsString(FIRST)));
    }

    @Test
    @Mode(TestMode.FULL)
    public void testBeanWasDestroyed() throws Exception {
        List<String> strings = DirtySingleton.getStrings();
        assertThat(strings, hasItem(containsString(SECOND)));
    }
}
