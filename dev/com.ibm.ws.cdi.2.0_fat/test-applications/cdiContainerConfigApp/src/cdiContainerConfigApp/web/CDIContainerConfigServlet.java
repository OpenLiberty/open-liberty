/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/cdiContainerConfigApp")
public class CDIContainerConfigServlet extends FATServlet {

    @Inject
    MyBeanCDI20 mbInstance;

    @Test
    public void testimplicitBeanArchiveDisabled() throws Exception {
        assertFalse("MyExplicitBean was not found when it should have been", mbInstance.isExplicitUnsatisfied());
        assertTrue("MyImplicitBean was found when it should have been disabled", mbInstance.isImplicitUnsatisfied());
    }

}
