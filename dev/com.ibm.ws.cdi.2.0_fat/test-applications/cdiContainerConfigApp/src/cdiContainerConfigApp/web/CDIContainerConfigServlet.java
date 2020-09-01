/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package cdiContainerConfigApp.web;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/cdiContainerConfigApp")
public class CDIContainerConfigServlet extends FATServlet {

    @Inject
    MyBeanCDI20 mbInstance;

    @Test
    @Mode(TestMode.FULL)
    public void testimplicitBeanArchiveDisabled() throws Exception {
        assertFalse("MyExplicitBean was not found when it should have been", mbInstance.isExplicitUnsatisfied());
        assertTrue("MyImplicitBean was found when it should have been disabled", mbInstance.isImplicitUnsatisfied());
    }

}
