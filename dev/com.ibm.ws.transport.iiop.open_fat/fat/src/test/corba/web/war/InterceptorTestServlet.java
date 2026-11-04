/*
 * Copyright 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package test.corba.web.war;

import java.rmi.AccessException;

import javax.annotation.Resource;
import javax.servlet.annotation.WebServlet;

import org.junit.Assert;
import org.junit.Test;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.ORB;

import componenttest.app.FATServlet;
import shared.ClientUtil;

@WebServlet("/InterceptorTestServlet")
@SuppressWarnings("serial")
public class InterceptorTestServlet extends FATServlet {
    @Resource
    private ORB orb;

    @Test
    public void testInterceptorIsInstalledCorrectly() throws Throwable {
        try {
            ClientUtil.lookupBusinessBean(orb).takesString("This shouldn't work");
            Assert.fail("Invoking the bean should result in an exception");
        } catch (AccessException expected) {
            try {
                throw expected.getCause();
            } catch (NO_PERMISSION expectedToo) {
                Assert.assertEquals("Can't touch this.", expectedToo.getMessage());
            }
        }
    }
}
