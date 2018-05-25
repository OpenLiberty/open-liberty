/*******************************************************************************
 * Copyright (c) 2010, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.misc.jitdeploy.web;

import static org.junit.Assert.fail;

import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJBException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.ejbcontainer.remote.misc.jitdeploy.ejb.ExceptionBean;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/ExceptionServlet")
public class ExceptionServlet extends FATServlet {
    private static final String CLASS_NAME = ExceptionServlet.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    private ExceptionBean lookupBean() throws NamingException {
        return (ExceptionBean) new InitialContext().lookup("java:app/JitDeployEJB/ExceptionBean");
    }

    /**
     * This test verifies that declaring RuntimeException on the throws clause
     * of a business method does not cause RuntimeException to be considered an
     * application exception.
     */
    @Test
    @ExpectedFFDC({ "java.lang.RuntimeException" })
    public void testThrowsRuntimeException() throws Exception {
        try {
            lookupBean().throwsRuntimeException();
            fail("expected EJBException");
        } catch (EJBException ex) {
            svLogger.log(Level.INFO, "caught expected exception", ex);
        }
    }

    /**
     * This test verifies that declaring Error on the throws clause of a
     * business method does not cause Error to be considered an application
     * exception.
     */
    @Test
    @ExpectedFFDC({ "java.lang.Error" })
    public void testThrowsError() throws Exception {
        try {
            lookupBean().throwsError();
            fail("expected EJBException");
        } catch (EJBException ex) {
            svLogger.log(Level.INFO, "caught expected exception", ex);
        }
    }

    /**
     * This test verifies that declaring Exception on the throws clause of a
     * business method does not cause RuntimeException to be considered an
     * application exception.
     */
    @Test
    @ExpectedFFDC({ "java.lang.RuntimeException" })
    public void testThrowsException() throws Exception {
        try {
            lookupBean().throwsException();
            fail("expected EJBException");
        } catch (EJBException ex) {
            svLogger.log(Level.INFO, "caught expected exception", ex);
        }
    }

    /**
     * This test verifies that declaring RuntimeException on the throws clause
     * of an asynchronous business method does not cause RuntimeException to be
     * considered an application exception.
     */
    @Test
    @ExpectedFFDC({ "java.lang.RuntimeException" })
    public void testAsyncThrowsRuntimeException() throws Exception {
        try {
            lookupBean().asyncThrowsRuntimeException().get();
            fail("expected ExecutionException caused by EJBException");
        } catch (ExecutionException ex) {
            if (ex.getCause() instanceof EJBException) {
                svLogger.log(Level.INFO, "caught expected exception", ex);
            } else {
                throw ex;
            }
        }
    }

    /**
     * This test verifies that declaring Error on the throws clause of an
     * asynchronous business method does not cause Error to be considered an
     * application exception.
     */
    @Test
    @ExpectedFFDC({ "java.lang.Error" })
    public void testAsyncThrowsError() throws Exception {
        try {
            lookupBean().asyncThrowsError().get();
            fail("expected ExecutionException caused by EJBException");
        } catch (ExecutionException ex) {
            if (ex.getCause() instanceof EJBException) {
                svLogger.log(Level.INFO, "caught expected exception", ex);
            } else {
                throw ex;
            }
        }
    }

    /**
     * This test verifies that declaring Exception on the throws clause of an
     * asynchronous business method does not cause RuntimeException to be
     * considered an application exception.
     */
    @Test
    @ExpectedFFDC({ "java.lang.RuntimeException" })
    public void testAsyncThrowsException() throws Exception {
        try {
            lookupBean().asyncThrowsException().get();
            fail("expected ExecutionException caused by EJBException");
        } catch (ExecutionException ex) {
            if (ex.getCause() instanceof EJBException) {
                svLogger.log(Level.INFO, "caught expected exception", ex);
            } else {
                throw ex;
            }
        }
    }
}