/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.annotations.fat.apps.dependentScopedProducer;

import static org.junit.Assert.fail;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.cdi.annotations.fat.apps.dependentScopedProducer.beans.NonNullBeanTwo;

import componenttest.app.FATServlet;

//This servlet should return a resource injection exception when accessed.
@WebServlet("/failAppScopedMethod")

public class AppScopedMethodServlet extends FATServlet {

    @Inject
    NonNullBeanTwo nullBean;

    @Test
    public void testAppScopedMethod() throws IOException {
        try {
            nullBean.toString(); //calling a method as a proxy gets injected.
            fail("A nullBean was injected. Test Failed");
        } catch (Exception e) { //I'm doing it this way to avoid adding a dependency on weld.
            if (!e.getMessage().contains("WELD-000052")) {
                fail("The wrong exception was thrown: " + e.getMessage());
            }
        }
    }

}
