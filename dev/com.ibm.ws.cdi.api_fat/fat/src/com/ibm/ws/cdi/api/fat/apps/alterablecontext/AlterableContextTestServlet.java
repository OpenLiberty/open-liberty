/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
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

package com.ibm.ws.cdi.api.fat.apps.alterablecontext;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;

import java.util.List;

import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.cdi.api.fat.apps.alterablecontext.extension.AlterableContextBean;
import com.ibm.ws.cdi.api.fat.apps.alterablecontext.extension.DirtySingleton;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@WebServlet("/")
public class AlterableContextTestServlet extends FATServlet {

    private static final long serialVersionUID = 8549700799591343964L;
    private static String FIRST = "I got this from my alterablecontext: " + AlterableContextBean.class.getName();
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
