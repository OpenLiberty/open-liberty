/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.tx.jta.web;

import static org.junit.Assert.assertNotNull;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

import componenttest.annotation.SkipForRepeat;
import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/SimpleServlet")
public class SimpleServlet extends FATServlet {

    /**
     * Message written to servlet to indicate that is has been successfully invoked.
     */
    public static final String SUCCESS_MESSAGE = "COMPLETED SUCCESSFULLY";

    // By extending FATServlet and using @TestServlet in the client side test class,
    // @Test annotations can be added directly to the test servlet.  In this test servlet,
    // each @Test method is invoked in its own HTTP GET request.

    @Test
    public void testJavaseTxApiIsVisible(HttpServletRequest request, HttpServletResponse response) throws Exception {
        final String JavaSeTxApiClazzName = "javax.transaction.xa.XAResource";
        assertNotNull("Java SE Transaction API class " + JavaSeTxApiClazzName + " should be visible to the application classloader, but is not",
                      loadClazz(JavaSeTxApiClazzName));
    }

    @Test
    @SkipForRepeat(SkipForRepeat.EE9_FEATURES)
    public void testJavaeeTxApiIsVisible(HttpServletRequest request, HttpServletResponse response) throws Exception {
        final String JavaEeTxApiClazzName = "javax.transaction.UserTransaction";
        assertNotNull("Java EE Transaction API class " + JavaEeTxApiClazzName + " should be visible to the application classloader, but is not",
                      loadClazz(JavaEeTxApiClazzName));
    }

    @Test
    @SkipForRepeat({ SkipForRepeat.NO_MODIFICATION, SkipForRepeat.EE8_FEATURES })
    public void testJakartaTxApiIsVisible(HttpServletRequest request, HttpServletResponse response) throws Exception {
        final String JakartaTxApiClazzName = "jakarta.transaction.UserTransaction";
        assertNotNull("Jakarta Transaction API class " + JakartaTxApiClazzName + " should be visible to the application classloader, but is not",
                      loadClazz(JakartaTxApiClazzName));
    }

    Class<?> loadClazz(String clazzName) {
        Class<?> clazz = null;
        try {
            clazz = this.getClass().getClassLoader().loadClass(clazzName);
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        return clazz;
    }
}
