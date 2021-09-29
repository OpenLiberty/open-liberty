/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.exception.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.ServerException;
import java.sql.SQLException;

import javax.ejb.EJBException;
import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

//import com.ibm.ejs.container.UnknownLocalException;
import com.ibm.ws.ejbcontainer.exception.ejb.SLRemoteEx;
import com.ibm.ws.ejbcontainer.exception.ejb.SLRemoteExHome;
import com.ibm.ws.ejbcontainer.exception.ejb.SLRemoteExLocal;
import com.ibm.ws.ejbcontainer.exception.ejb.SLRemoteExLocalHome;
import com.ibm.ws.ejbcontainer.exception.ejb.SLRemoteExLocalRemote;
import com.ibm.ws.ejbcontainer.exception.ejb.SLRemoteExLocalRemoteHome;
import com.ibm.ws.ejbcontainer.exception.ejb.SLRemoteException;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/ExceptionServlet")
public class ExceptionServlet extends FATServlet {

    private SLRemoteEx lookupSLRemoteExBean() throws Exception {
        SLRemoteExHome home = (SLRemoteExHome) new InitialContext().lookup("java:app/ExceptionBean/SLRemoteExBean!com.ibm.ws.ejbcontainer.exception.ejb.SLRemoteExHome");
        return home.create();
    }

    private SLRemoteExLocal lookupSLRemoteExBeanLocal() throws Exception {
        SLRemoteExLocalHome home = (SLRemoteExLocalHome) new InitialContext().lookup("java:app/ExceptionBean/SLRemoteExBean!com.ibm.ws.ejbcontainer.exception.ejb.SLRemoteExLocalHome");
        return home.create();
    }

    private SLRemoteExLocalRemote lookupSLRemoteExBeanLocalRemote() throws Exception {
        SLRemoteExLocalRemoteHome home = (SLRemoteExLocalRemoteHome) new InitialContext().lookup("java:app/ExceptionBean/SLRemoteExLocalRemoteBean!com.ibm.ws.ejbcontainer.exception.ejb.SLRemoteExLocalRemoteHome");
        return home.create();
    }

    @Test
    @ExpectedFFDC({ "java.lang.IllegalStateException", "java.lang.RuntimeException", "java.rmi.RemoteException", "com.ibm.ws.ejbcontainer.exception.ejb.SLRemoteException" })
    public void testRMIRemoteMethodwithException() throws Exception {
        SLRemoteEx remoteEx = lookupSLRemoteExBean();

        // verify method with throws exception may be called without an exception
        remoteEx.testMethodwithException("none");

        try {
            remoteEx.testMethodwithException("Exception");
            fail("Expected Exception was not thrown");
        } catch (Exception ex) {
            assertTrue("Exception is not Exception : " + ex.getClass().getName(), ex.getClass() == Exception.class);
            assertEquals("Wrong Exception message received", "Exception", ex.getMessage());
        }

        try {
            remoteEx.testMethodwithException("IOException");
            fail("Expected IOException was not thrown");
        } catch (IOException ex) {
            assertTrue("Exception is not IOException : " + ex.getClass().getName(), ex.getClass() == IOException.class);
            assertEquals("Wrong Exception message received", "IOException", ex.getMessage());
        }

        try {
            remoteEx.testMethodwithException("RemoteException");
            fail("Expected RemoteException was not thrown");
        } catch (ServerException ex) {
            Throwable rootex = ex.detail;
            assertTrue("Exception is not RemoteException : " + rootex.getClass().getName(), rootex.getClass() == RemoteException.class);
            assertEquals("Wrong Exception message received", "RemoteException", rootex.getMessage());
        }

        try {
            remoteEx.testMethodwithException("SLRemoteException");
            fail("Expected SLRemoteException was not thrown");
        } catch (ServerException ex) {
            Throwable rootex = ex.detail;
            assertTrue("Exception is not SLRemoteException : " + rootex.getClass().getName(), rootex.getClass() == SLRemoteException.class);
            assertEquals("Wrong Exception message received", "SLRemoteException", rootex.getMessage());
        }

        try {
            remoteEx.testMethodwithException("RuntimeException");
            fail("Expected RuntimeException was not thrown");
        } catch (ServerException ex) {
            Throwable rootex = ex.detail;
            assertTrue("Exception is not RemoteException : " + rootex.getClass().getName(), rootex.getClass() == RemoteException.class);
            rootex = rootex.getCause();
            assertTrue("Exception is not RuntimeException : " + rootex.getClass().getName(), rootex.getClass() == RuntimeException.class);
            assertEquals("Wrong Exception message received", "RuntimeException", rootex.getMessage());
        }

        try {
            remoteEx.testMethodwithException("IllegalStateException");
            fail("Expected IllegalStateException was not thrown");
        } catch (ServerException ex) {
            Throwable rootex = ex.detail;
            assertTrue("Exception is not RemoteException : " + rootex.getClass().getName(), rootex.getClass() == RemoteException.class);
            rootex = rootex.getCause();
            assertTrue("Exception is not IllegalStateException : " + rootex.getClass().getName(), rootex.getClass() == IllegalStateException.class);
            assertEquals("Wrong Exception message received", "IllegalStateException", rootex.getMessage());
        }

        try {
            remoteEx.testMethodwithException("SQLException");
            fail("Expected SQLException was not thrown");
        } catch (SQLException ex) {
            assertTrue("Exception is not SQLException : " + ex.getClass().getName(), ex.getClass() == SQLException.class);
            assertEquals("Wrong Exception message received", "SQLException", ex.getMessage());
        }
    }

    @Test
    @ExpectedFFDC({ "java.lang.IllegalStateException", "java.lang.RuntimeException" })
    public void testRMIRemoteMethodwithExceptionAndRemote() throws Exception {
        SLRemoteEx remoteEx = lookupSLRemoteExBean();

        // verify method with throws exception may be called without an exception
        remoteEx.testMethodwithExceptionAndRemote("none");

        try {
            remoteEx.testMethodwithExceptionAndRemote("Exception");
            fail("Expected Exception was not thrown");
        } catch (Exception ex) {
            assertTrue("Exception is not Exception : " + ex.getClass().getName(), ex.getClass() == Exception.class);
            assertEquals("Wrong Exception message received", "Exception", ex.getMessage());
        }

        try {
            remoteEx.testMethodwithExceptionAndRemote("IOException");
            fail("Expected IOException was not thrown");
        } catch (IOException ex) {
            assertTrue("Exception is not IOException : " + ex.getClass().getName(), ex.getClass() == IOException.class);
            assertEquals("Wrong Exception message received", "IOException", ex.getMessage());
        }

        try {
            remoteEx.testMethodwithExceptionAndRemote("RemoteException");
            fail("Expected RemoteException was not thrown");
        } catch (RemoteException ex) {
            assertTrue("Exception is not RemoteException : " + ex.getClass().getName(), ex.getClass() == RemoteException.class);
            assertEquals("Wrong Exception message received", "RemoteException", ex.getMessage());
        }

        try {
            remoteEx.testMethodwithExceptionAndRemote("SLRemoteException");
            fail("Expected SLRemoteException was not thrown");
        } catch (SLRemoteException ex) {
            assertTrue("Exception is not SLRemoteException : " + ex.getClass().getName(), ex.getClass() == SLRemoteException.class);
            assertEquals("Wrong Exception message received", "SLRemoteException", ex.getMessage());
        }

        try {
            remoteEx.testMethodwithExceptionAndRemote("RuntimeException");
            fail("Expected RuntimeException was not thrown");
        } catch (ServerException ex) {
            Throwable rootex = ex.detail;
            assertTrue("Exception is not RemoteException : " + rootex.getClass().getName(), rootex.getClass() == RemoteException.class);
            rootex = rootex.getCause();
            assertTrue("Exception is not RuntimeException : " + rootex.getClass().getName(), rootex.getClass() == RuntimeException.class);
            assertEquals("Wrong Exception message received", "RuntimeException", rootex.getMessage());
        }

        try {
            remoteEx.testMethodwithExceptionAndRemote("IllegalStateException");
            fail("Expected IllegalStateException was not thrown");
        } catch (ServerException ex) {
            Throwable rootex = ex.detail;
            assertTrue("Exception is not RemoteException : " + rootex.getClass().getName(), rootex.getClass() == RemoteException.class);
            rootex = rootex.getCause();
            assertTrue("Exception is not IllegalStateException : " + ex.getClass().getName(), rootex.getClass() == IllegalStateException.class);
            assertEquals("Wrong Exception message received", "IllegalStateException", rootex.getMessage());
        }
    }

    @Test
    @ExpectedFFDC({ "java.lang.IllegalStateException", "java.lang.RuntimeException", "java.rmi.RemoteException", "com.ibm.ws.ejbcontainer.exception.ejb.SLRemoteException" })
    public void testRMIRemoteMethodwithExceptionAndRemoteSub() throws Exception {
        SLRemoteEx remoteEx = lookupSLRemoteExBean();

        // verify method with throws exception may be called without an exception
        remoteEx.testMethodwithExceptionAndRemoteSub("none");

        try {
            remoteEx.testMethodwithExceptionAndRemoteSub("Exception");
            fail("Expected Exception was not thrown");
        } catch (Exception ex) {
            assertTrue("Exception is not Exception : " + ex.getClass().getName(), ex.getClass() == Exception.class);
            assertEquals("Wrong Exception message received", "Exception", ex.getMessage());
        }

        try {
            remoteEx.testMethodwithExceptionAndRemoteSub("IOException");
            fail("Expected IOException was not thrown");
        } catch (IOException ex) {
            assertTrue("Exception is not IOException : " + ex.getClass().getName(), ex.getClass() == IOException.class);
            assertEquals("Wrong Exception message received", "IOException", ex.getMessage());
        }

        try {
            remoteEx.testMethodwithExceptionAndRemoteSub("RemoteException");
            fail("Expected RemoteException was not thrown");
        } catch (ServerException ex) {
            Throwable rootex = ex.detail;
            assertTrue("Exception is not RemoteException : " + rootex.getClass().getName(), rootex.getClass() == RemoteException.class);
            assertEquals("Wrong Exception message received", "RemoteException", rootex.getMessage());
        }

        try {
            remoteEx.testMethodwithExceptionAndRemoteSub("SLRemoteException");
            fail("Expected SLRemoteException was not thrown");
        } catch (ServerException ex) {
            Throwable rootex = ex.detail;
            assertTrue("Exception is not SLRemoteException : " + rootex.getClass().getName(), rootex.getClass() == SLRemoteException.class);
            assertEquals("Wrong Exception message received", "SLRemoteException", rootex.getMessage());
        }

        try {
            remoteEx.testMethodwithExceptionAndRemoteSub("RuntimeException");
            fail("Expected RuntimeException was not thrown");
        } catch (ServerException ex) {
            Throwable rootex = ex.detail;
            assertTrue("Exception is not RemoteException : " + rootex.getClass().getName(), rootex.getClass() == RemoteException.class);
            rootex = rootex.getCause();
            assertTrue("Exception is not RuntimeException : " + rootex.getClass().getName(), rootex.getClass() == RuntimeException.class);
            assertEquals("Wrong Exception message received", "RuntimeException", rootex.getMessage());
        }

        try {
            remoteEx.testMethodwithExceptionAndRemoteSub("IllegalStateException");
            fail("Expected IllegalStateException was not thrown");
        } catch (ServerException ex) {
            Throwable rootex = ex.detail;
            assertTrue("Exception is not RemoteException : " + rootex.getClass().getName(), rootex.getClass() == RemoteException.class);
            rootex = rootex.getCause();
            assertTrue("Exception is not IllegalStateException : " + rootex.getClass().getName(), rootex.getClass() == IllegalStateException.class);
            assertEquals("Wrong Exception message received", "IllegalStateException", rootex.getMessage());
        }
    }

    @Test
    @ExpectedFFDC({ "java.lang.IllegalStateException", "java.lang.RuntimeException", "java.rmi.RemoteException", "com.ibm.ws.ejbcontainer.exception.ejb.SLRemoteException" })
    public void testRMIRemoteMethodwithExceptionAndRemoteAndRemoteSub() throws Exception {
        SLRemoteEx remoteEx = lookupSLRemoteExBean();

        // verify method with throws exception may be called without an exception
        remoteEx.testMethodwithExceptionAndRemoteAndRemoteSub("none");

        try {
            remoteEx.testMethodwithExceptionAndRemoteAndRemoteSub("Exception");
            fail("Expected Exception was not thrown");
        } catch (Exception ex) {
            assertTrue("Exception is not Exception : " + ex.getClass().getName(), ex.getClass() == Exception.class);
            assertEquals("Wrong Exception message received", "Exception", ex.getMessage());
        }

        try {
            remoteEx.testMethodwithExceptionAndRemoteAndRemoteSub("IOException");
            fail("Expected IOException was not thrown");
        } catch (IOException ex) {
            assertTrue("Exception is not IOException : " + ex.getClass().getName(), ex.getClass() == IOException.class);
            assertEquals("Wrong Exception message received", "IOException", ex.getMessage());
        }

        try {
            remoteEx.testMethodwithExceptionAndRemoteAndRemoteSub("RemoteException");
            fail("Expected RemoteException was not thrown");
        } catch (ServerException ex) {
            Throwable rootex = ex.detail;
            assertTrue("Exception is not RemoteException : " + rootex.getClass().getName(), rootex.getClass() == RemoteException.class);
            assertEquals("Wrong Exception message received", "RemoteException", rootex.getMessage());
        }

        try {
            remoteEx.testMethodwithExceptionAndRemoteAndRemoteSub("SLRemoteException");
            fail("Expected SLRemoteException was not thrown");
        } catch (ServerException ex) {
            Throwable rootex = ex.detail;
            assertTrue("Exception is not SLRemoteException : " + rootex.getClass().getName(), rootex.getClass() == SLRemoteException.class);
            assertEquals("Wrong Exception message received", "SLRemoteException", rootex.getMessage());
        }

        try {
            remoteEx.testMethodwithExceptionAndRemoteAndRemoteSub("RuntimeException");
            fail("Expected RuntimeException was not thrown");
        } catch (ServerException ex) {
            Throwable rootex = ex.detail;
            assertTrue("Exception is not RemoteException : " + rootex.getClass().getName(), rootex.getClass() == RemoteException.class);
            rootex = rootex.getCause();
            assertTrue("Exception is not RuntimeException : " + rootex.getClass().getName(), rootex.getClass() == RuntimeException.class);
            assertEquals("Wrong Exception message received", "RuntimeException", rootex.getMessage());
        }

        try {
            remoteEx.testMethodwithExceptionAndRemoteAndRemoteSub("IllegalStateException");
            fail("Expected IllegalStateException was not thrown");
        } catch (ServerException ex) {
            Throwable rootex = ex.detail;
            assertTrue("Exception is not RemoteException : " + rootex.getClass().getName(), rootex.getClass() == RemoteException.class);
            rootex = rootex.getCause();
            assertTrue("Exception is not IllegalStateException : " + rootex.getClass().getName(), rootex.getClass() == IllegalStateException.class);
            assertEquals("Wrong Exception message received", "IllegalStateException", rootex.getMessage());
        }
    }

    @Test
    @ExpectedFFDC({ "java.lang.IllegalStateException", "java.lang.RuntimeException" })
    public void testRMIRemoteMethodwithIOException() throws Exception {
        SLRemoteEx remoteEx = lookupSLRemoteExBean();

        // verify method with throws exception may be called without an exception
        remoteEx.testMethodwithIOException("none");

        try {
            remoteEx.testMethodwithIOException("IOException");
            fail("Expected IOException was not thrown");
        } catch (IOException ex) {
            assertTrue("Exception is not IOException : " + ex.getClass().getName(), ex.getClass() == IOException.class);
            assertEquals("Wrong Exception message received", "IOException", ex.getMessage());
        }

        try {
            remoteEx.testMethodwithIOException("RemoteException");
            fail("Expected RemoteException was not thrown");
        } catch (RemoteException ex) {
            assertTrue("Exception is not RemoteException : " + ex.getClass().getName(), ex.getClass() == RemoteException.class);
            assertEquals("Wrong Exception message received", "RemoteException", ex.getMessage());
        }

        try {
            remoteEx.testMethodwithIOException("SLRemoteException");
            fail("Expected SLRemoteException was not thrown");
        } catch (SLRemoteException ex) {
            assertTrue("Exception is not SLRemoteException : " + ex.getClass().getName(), ex.getClass() == SLRemoteException.class);
            assertEquals("Wrong Exception message received", "SLRemoteException", ex.getMessage());
        }

        try {
            remoteEx.testMethodwithIOException("RuntimeException");
            fail("Expected RuntimeException was not thrown");
        } catch (ServerException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RemoteException : " + rootex.getClass().getName(), rootex.getClass() == RemoteException.class);
            rootex = rootex.getCause();
            assertTrue("Exception is not RuntimeException : " + rootex.getClass().getName(), rootex.getClass() == RuntimeException.class);
            assertEquals("Wrong Exception message received", "RuntimeException", rootex.getMessage());
        }

        try {
            remoteEx.testMethodwithIOException("IllegalStateException");
            fail("Expected IllegalStateException was not thrown");
        } catch (ServerException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RemoteException : " + rootex.getClass().getName(), rootex.getClass() == RemoteException.class);
            rootex = rootex.getCause();
            assertTrue("Exception is not IllegalStateException : " + rootex.getClass().getName(), rootex.getClass() == IllegalStateException.class);
            assertEquals("Wrong Exception message received", "IllegalStateException", rootex.getMessage());
        }

        try {
            remoteEx.testMethodwithIOException("FileNotFoundException");
            fail("Expected FileNotFoundException was not thrown");
        } catch (FileNotFoundException ex) {
            assertTrue("Exception is not FileNotFoundException : " + ex.getClass().getName(), ex.getClass() == FileNotFoundException.class);
            assertEquals("Wrong Exception message received", "FileNotFoundException", ex.getMessage());
        }
    }

    @Test
    @ExpectedFFDC({ "java.lang.IllegalStateException", "java.lang.RuntimeException", "java.rmi.RemoteException", "com.ibm.ws.ejbcontainer.exception.ejb.SLRemoteException" })
    public void testRMIRemoteMethodwithIOExceptionAndRemote() throws Exception {
        SLRemoteEx remoteEx = lookupSLRemoteExBean();

        // verify method with throws exception may be called without an exception
        remoteEx.testMethodwithIOExceptionAndRemote("none");

        try {
            remoteEx.testMethodwithIOExceptionAndRemote("IOException");
            fail("Expected IOException was not thrown");
        } catch (IOException ex) {
            assertTrue("Exception is not IOException : " + ex.getClass().getName(), ex.getClass() == IOException.class);
            assertEquals("Wrong Exception message received", "IOException", ex.getMessage());
        }

        try {
            remoteEx.testMethodwithIOExceptionAndRemote("RemoteException");
            fail("Expected RemoteException was not thrown");
        } catch (ServerException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RemoteException : " + rootex.getClass().getName(), rootex.getClass() == RemoteException.class);
            assertEquals("Wrong Exception message received", "RemoteException", rootex.getMessage());
        }

        try {
            remoteEx.testMethodwithIOExceptionAndRemote("SLRemoteException");
            fail("Expected SLRemoteException was not thrown");
        } catch (ServerException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not SLRemoteException : " + rootex.getClass().getName(), rootex.getClass() == SLRemoteException.class);
            assertEquals("Wrong Exception message received", "SLRemoteException", rootex.getMessage());
        }

        try {
            remoteEx.testMethodwithIOExceptionAndRemote("RuntimeException");
            fail("Expected RuntimeException was not thrown");
        } catch (ServerException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RemoteException : " + rootex.getClass().getName(), rootex.getClass() == RemoteException.class);
            rootex = rootex.getCause();
            assertTrue("Exception is not RuntimeException : " + rootex.getClass().getName(), rootex.getClass() == RuntimeException.class);
            assertEquals("Wrong Exception message received", "RuntimeException", rootex.getMessage());
        }

        try {
            remoteEx.testMethodwithIOExceptionAndRemote("IllegalStateException");
            fail("Expected IllegalStateException was not thrown");
        } catch (ServerException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RemoteException : " + rootex.getClass().getName(), rootex.getClass() == RemoteException.class);
            rootex = rootex.getCause();
            assertTrue("Exception is not IllegalStateException : " + rootex.getClass().getName(), rootex.getClass() == IllegalStateException.class);
            assertEquals("Wrong Exception message received", "IllegalStateException", rootex.getMessage());
        }

        try {
            remoteEx.testMethodwithIOExceptionAndRemote("FileNotFoundException");
            fail("Expected FileNotFoundException was not thrown");
        } catch (FileNotFoundException ex) {
            assertTrue("Exception is not FileNotFoundException : " + ex.getClass().getName(), ex.getClass() == FileNotFoundException.class);
            assertEquals("Wrong Exception message received", "FileNotFoundException", ex.getMessage());
        }
    }

    @Test
    @ExpectedFFDC({ "java.lang.IllegalStateException", "java.lang.RuntimeException", "java.rmi.RemoteException", "com.ibm.ws.ejbcontainer.exception.ejb.SLRemoteException" })
    public void testRMIRemoteMethodwithIOExceptionAndRemoteSub() throws Exception {
        SLRemoteEx remoteEx = lookupSLRemoteExBean();

        // verify method with throws exception may be called without an exception
        remoteEx.testMethodwithIOExceptionAndRemoteSub("none");

        try {
            remoteEx.testMethodwithIOExceptionAndRemoteSub("IOException");
            fail("Expected IOException was not thrown");
        } catch (IOException ex) {
            assertTrue("Exception is not IOException : " + ex.getClass().getName(), ex.getClass() == IOException.class);
            assertEquals("Wrong Exception message received", "IOException", ex.getMessage());
        }

        try {
            remoteEx.testMethodwithIOExceptionAndRemoteSub("RemoteException");
            fail("Expected RemoteException was not thrown");
        } catch (ServerException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RemoteException : " + rootex.getClass().getName(), rootex.getClass() == RemoteException.class);
            assertEquals("Wrong Exception message received", "RemoteException", rootex.getMessage());
        }

        try {
            remoteEx.testMethodwithIOExceptionAndRemoteSub("SLRemoteException");
            fail("Expected SLRemoteException was not thrown");
        } catch (ServerException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not SLRemoteException : " + rootex.getClass().getName(), rootex.getClass() == SLRemoteException.class);
            assertEquals("Wrong Exception message received", "SLRemoteException", rootex.getMessage());
        }

        try {
            remoteEx.testMethodwithIOExceptionAndRemoteSub("RuntimeException");
            fail("Expected RuntimeException was not thrown");
        } catch (ServerException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RemoteException : " + rootex.getClass().getName(), rootex.getClass() == RemoteException.class);
            rootex = rootex.getCause();
            assertTrue("Exception is not RuntimeException : " + rootex.getClass().getName(), rootex.getClass() == RuntimeException.class);
            assertEquals("Wrong Exception message received", "RuntimeException", rootex.getMessage());
        }

        try {
            remoteEx.testMethodwithIOExceptionAndRemoteSub("IllegalStateException");
            fail("Expected IllegalStateException was not thrown");
        } catch (ServerException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RemoteException : " + rootex.getClass().getName(), rootex.getClass() == RemoteException.class);
            rootex = rootex.getCause();
            assertTrue("Exception is not IllegalStateException : " + rootex.getClass().getName(), rootex.getClass() == IllegalStateException.class);
            assertEquals("Wrong Exception message received", "IllegalStateException", rootex.getMessage());
        }

        try {
            remoteEx.testMethodwithIOExceptionAndRemoteSub("FileNotFoundException");
            fail("Expected FileNotFoundException was not thrown");
        } catch (FileNotFoundException ex) {
            assertTrue("Exception is not FileNotFoundException : " + ex.getClass().getName(), ex.getClass() == FileNotFoundException.class);
            assertEquals("Wrong Exception message received", "FileNotFoundException", ex.getMessage());
        }
    }

    @Test
    @ExpectedFFDC({ "java.lang.IllegalStateException", "java.lang.RuntimeException", "java.rmi.RemoteException", "com.ibm.ws.ejbcontainer.exception.ejb.SLRemoteException" })
    public void testRMIRemoteMethodwithIOExceptionAndRemoteAndRemoteSub() throws Exception {
        SLRemoteEx remoteEx = lookupSLRemoteExBean();

        // verify method with throws exception may be called without an exception
        remoteEx.testMethodwithIOExceptionAndRemoteAndRemoteSub("none");

        try {
            remoteEx.testMethodwithIOExceptionAndRemoteAndRemoteSub("IOException");
            fail("Expected IOException was not thrown");
        } catch (IOException ex) {
            assertTrue("Exception is not IOException : " + ex.getClass().getName(), ex.getClass() == IOException.class);
            assertEquals("Wrong Exception message received", "IOException", ex.getMessage());
        }

        try {
            remoteEx.testMethodwithIOExceptionAndRemoteAndRemoteSub("RemoteException");
            fail("Expected RemoteException was not thrown");
        } catch (ServerException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RemoteException : " + rootex.getClass().getName(), rootex.getClass() == RemoteException.class);
            assertEquals("Wrong Exception message received", "RemoteException", rootex.getMessage());
        }

        try {
            remoteEx.testMethodwithIOExceptionAndRemoteAndRemoteSub("SLRemoteException");
            fail("Expected SLRemoteException was not thrown");
        } catch (ServerException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not SLRemoteException : " + rootex.getClass().getName(), rootex.getClass() == SLRemoteException.class);
            assertEquals("Wrong Exception message received", "SLRemoteException", rootex.getMessage());
        }

        try {
            remoteEx.testMethodwithIOExceptionAndRemoteAndRemoteSub("RuntimeException");
            fail("Expected RuntimeException was not thrown");
        } catch (ServerException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RemoteException : " + rootex.getClass().getName(), rootex.getClass() == RemoteException.class);
            rootex = rootex.getCause();
            assertTrue("Exception is not RuntimeException : " + rootex.getClass().getName(), rootex.getClass() == RuntimeException.class);
            assertEquals("Wrong Exception message received", "RuntimeException", rootex.getMessage());
        }

        try {
            remoteEx.testMethodwithIOExceptionAndRemoteAndRemoteSub("IllegalStateException");
            fail("Expected IllegalStateException was not thrown");
        } catch (ServerException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RemoteException : " + rootex.getClass().getName(), rootex.getClass() == RemoteException.class);
            rootex = rootex.getCause();
            assertTrue("Exception is not IllegalStateException : " + rootex.getClass().getName(), rootex.getClass() == IllegalStateException.class);
            assertEquals("Wrong Exception message received", "IllegalStateException", rootex.getMessage());
        }

        try {
            remoteEx.testMethodwithIOExceptionAndRemoteAndRemoteSub("FileNotFoundException");
            fail("Expected FileNotFoundException was not thrown");
        } catch (FileNotFoundException ex) {
            assertTrue("Exception is not FileNotFoundException : " + ex.getClass().getName(), ex.getClass() == FileNotFoundException.class);
            assertEquals("Wrong Exception message received", "FileNotFoundException", ex.getMessage());
        }
    }

    @Test
    @ExpectedFFDC({ "java.lang.IllegalStateException", "java.lang.RuntimeException", "com.ibm.ws.ejbcontainer.exception.ejb.SLRemoteException", "java.rmi.RemoteException" })
    public void testRMIRemoteMethodwithRemoteEx() throws Exception {
        SLRemoteEx remoteEx = lookupSLRemoteExBean();

        // verify method with throws exception may be called without an exception
        remoteEx.testMethodwithRemoteEx("none");

        try {
            remoteEx.testMethodwithRemoteEx("RemoteException");
            fail("Expected RemoteException was not thrown");
        } catch (ServerException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RemoteException : " + rootex.getClass().getName(), rootex.getClass() == RemoteException.class);
            assertEquals("Wrong Exception message received", "RemoteException", rootex.getMessage());
        }

        try {
            remoteEx.testMethodwithRemoteEx("SLRemoteException");
            fail("Expected SLRemoteException was not thrown");
        } catch (ServerException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not SLRemoteException : " + rootex.getClass().getName(), rootex.getClass() == SLRemoteException.class);
            assertEquals("Wrong Exception message received", "SLRemoteException", rootex.getMessage());
        }

        try {
            remoteEx.testMethodwithRemoteEx("RuntimeException");
            fail("Expected RuntimeException was not thrown");
        } catch (ServerException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RemoteException : " + rootex.getClass().getName(), rootex.getClass() == RemoteException.class);
            rootex = rootex.getCause();
            assertTrue("Exception is not RuntimeException : " + rootex.getClass().getName(), rootex.getClass() == RuntimeException.class);
            assertEquals("Wrong Exception message received", "RuntimeException", rootex.getMessage());
        }

        try {
            remoteEx.testMethodwithRemoteEx("IllegalStateException");
            fail("Expected IllegalStateException was not thrown");
        } catch (ServerException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RemoteException : " + rootex.getClass().getName(), rootex.getClass() == RemoteException.class);
            rootex = rootex.getCause();
            assertTrue("Exception is not IllegalStateException : " + rootex.getClass().getName(), rootex.getClass() == IllegalStateException.class);
            assertEquals("Wrong Exception message received", "IllegalStateException", rootex.getMessage());
        }
    }

    @Test
    @ExpectedFFDC({ "java.lang.IllegalStateException", "java.lang.RuntimeException", "com.ibm.ws.ejbcontainer.exception.ejb.SLRemoteException", "java.rmi.RemoteException" })
    public void testRMIRemoteMethodwithRemoteExAndSub() throws Exception {
        SLRemoteEx remoteEx = lookupSLRemoteExBean();

        // verify method with throws exception may be called without an exception
        remoteEx.testMethodwithRemoteExAndSub("none");

        try {
            remoteEx.testMethodwithRemoteExAndSub("RemoteException");
            fail("Expected RemoteException was not thrown");
        } catch (ServerException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RemoteException : " + rootex.getClass().getName(), rootex.getClass() == RemoteException.class);
            assertEquals("Wrong Exception message received", "RemoteException", rootex.getMessage());
        }

        try {
            remoteEx.testMethodwithRemoteExAndSub("SLRemoteException");
            fail("Expected SLRemoteException was not thrown");
        } catch (ServerException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not SLRemoteException : " + rootex.getClass().getName(), rootex.getClass() == SLRemoteException.class);
            assertEquals("Wrong Exception message received", "SLRemoteException", rootex.getMessage());
        }

        try {
            remoteEx.testMethodwithRemoteExAndSub("RuntimeException");
            fail("Expected RuntimeException was not thrown");
        } catch (ServerException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RemoteException : " + rootex.getClass().getName(), rootex.getClass() == RemoteException.class);
            rootex = rootex.getCause();
            assertTrue("Exception is not RuntimeException : " + rootex.getClass().getName(), rootex.getClass() == RuntimeException.class);
            assertEquals("Wrong Exception message received", "RuntimeException", rootex.getMessage());
        }

        try {
            remoteEx.testMethodwithRemoteExAndSub("IllegalStateException");
            fail("Expected IllegalStateException was not thrown");
        } catch (ServerException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RemoteException : " + rootex.getClass().getName(), rootex.getClass() == RemoteException.class);
            rootex = rootex.getCause();
            assertTrue("Exception is not IllegalStateException : " + rootex.getClass().getName(), rootex.getClass() == IllegalStateException.class);
            assertEquals("Wrong Exception message received", "IllegalStateException", rootex.getMessage());
        }
    }

    @Test
    @ExpectedFFDC({ "java.lang.IllegalStateException", "java.lang.RuntimeException" })
    public void testLocalMethodwithNoEx() throws Exception {
        SLRemoteExLocal remoteExLocal = lookupSLRemoteExBeanLocal();

        // verify method with throws exception may be called without an exception
        remoteExLocal.testMethodwithNoEx("none");

        try {
            remoteExLocal.testMethodwithNoEx("RuntimeException");
            fail("Expected RuntimeException was not thrown");
        } catch (EJBException ex) { // (UnknownLocalException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RuntimeException : " + rootex.getClass().getName(), rootex.getClass() == RuntimeException.class);
            assertEquals("Wrong Exception message received", "RuntimeException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithNoEx("IllegalStateException");
            fail("Expected IllegalStateException was not thrown");
        } catch (EJBException ex) { // (UnknownLocalException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not IllegalStateException : " + rootex.getClass().getName(), rootex.getClass() == IllegalStateException.class);
            assertEquals("Wrong Exception message received", "IllegalStateException", rootex.getMessage());
        }
    }

    @Test
    @ExpectedFFDC({ "java.lang.IllegalStateException", "java.lang.RuntimeException", "java.rmi.RemoteException", "com.ibm.ws.ejbcontainer.exception.ejb.SLRemoteException" })
    public void testLocalMethodwithException() throws Exception {
        SLRemoteExLocal remoteExLocal = lookupSLRemoteExBeanLocal();

        // verify method with throws exception may be called without an exception
        remoteExLocal.testMethodwithException("none");

        try {
            remoteExLocal.testMethodwithException("Exception");
            fail("Expected Exception was not thrown");
        } catch (Exception ex) {
            assertTrue("Exception is not Exception : " + ex.getClass().getName(), ex.getClass() == Exception.class);
            assertEquals("Wrong Exception message received", "Exception", ex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithException("IOException");
            fail("Expected IOException was not thrown");
        } catch (IOException ex) {
            assertTrue("Exception is not IOException : " + ex.getClass().getName(), ex.getClass() == IOException.class);
            assertEquals("Wrong Exception message received", "IOException", ex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithException("RemoteException");
            fail("Expected RemoteException was not thrown");
        } catch (EJBException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RemoteException : " + rootex.getClass().getName(), rootex.getClass() == RemoteException.class);
            assertEquals("Wrong Exception message received", "RemoteException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithException("SLRemoteException");
            fail("Expected SLRemoteException was not thrown");
        } catch (EJBException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not SLRemoteException : " + rootex.getClass().getName(), rootex.getClass() == SLRemoteException.class);
            assertEquals("Wrong Exception message received", "SLRemoteException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithException("RuntimeException");
            fail("Expected RuntimeException was not thrown");
        } catch (EJBException ex) { // (UnknownLocalException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RuntimeException : " + rootex.getClass().getName(), rootex.getClass() == RuntimeException.class);
            assertEquals("Wrong Exception message received", "RuntimeException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithException("IllegalStateException");
            fail("Expected IllegalStateException was not thrown");
        } catch (EJBException ex) { // (UnknownLocalException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not IllegalStateException : " + rootex.getClass().getName(), rootex.getClass() == IllegalStateException.class);
            assertEquals("Wrong Exception message received", "IllegalStateException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithException("SQLException");
            fail("Expected SQLException was not thrown");
        } catch (SQLException ex) {
            assertTrue("Exception is not SQLException : " + ex.getClass().getName(), ex.getClass() == SQLException.class);
            assertEquals("Wrong Exception message received", "SQLException", ex.getMessage());
        }
    }

    @Test
    @ExpectedFFDC({ "java.lang.IllegalStateException", "java.lang.RuntimeException", "java.rmi.RemoteException", "com.ibm.ws.ejbcontainer.exception.ejb.SLRemoteException" })
    public void testLocalMethodwithExceptionAndRemote() throws Exception {
        SLRemoteExLocal remoteExLocal = lookupSLRemoteExBeanLocal();

        // verify method with throws exception may be called without an exception
        remoteExLocal.testMethodwithExceptionAndRemote("none");

        try {
            remoteExLocal.testMethodwithExceptionAndRemote("Exception");
            fail("Expected Exception was not thrown");
        } catch (Exception ex) {
            assertTrue("Exception is not Exception : " + ex.getClass().getName(), ex.getClass() == Exception.class);
            assertEquals("Wrong Exception message received", "Exception", ex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithExceptionAndRemote("IOException");
            fail("Expected IOException was not thrown");
        } catch (IOException ex) {
            assertTrue("Exception is not IOException : " + ex.getClass().getName(), ex.getClass() == IOException.class);
            assertEquals("Wrong Exception message received", "IOException", ex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithExceptionAndRemote("RemoteException");
            fail("Expected RemoteException was not thrown");
        } catch (EJBException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RemoteException : " + rootex.getClass().getName(), rootex.getClass() == RemoteException.class);
            assertEquals("Wrong Exception message received", "RemoteException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithExceptionAndRemote("SLRemoteException");
            fail("Expected SLRemoteException was not thrown");
        } catch (EJBException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not SLRemoteException : " + rootex.getClass().getName(), rootex.getClass() == SLRemoteException.class);
            assertEquals("Wrong Exception message received", "SLRemoteException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithExceptionAndRemote("RuntimeException");
            fail("Expected RuntimeException was not thrown");
        } catch (EJBException ex) { // (UnknownLocalException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RuntimeException : " + rootex.getClass().getName(), rootex.getClass() == RuntimeException.class);
            assertEquals("Wrong Exception message received", "RuntimeException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithExceptionAndRemote("IllegalStateException");
            fail("Expected IllegalStateException was not thrown");
        } catch (EJBException ex) { // (UnknownLocalException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not IllegalStateException : " + rootex.getClass().getName(), rootex.getClass() == IllegalStateException.class);
            assertEquals("Wrong Exception message received", "IllegalStateException", rootex.getMessage());
        }
    }

    @Test
    @ExpectedFFDC({ "java.lang.IllegalStateException", "java.lang.RuntimeException", "java.rmi.RemoteException", "com.ibm.ws.ejbcontainer.exception.ejb.SLRemoteException" })
    public void testLocalMethodwithExceptionAndRemoteSub() throws Exception {
        SLRemoteExLocal remoteExLocal = lookupSLRemoteExBeanLocal();

        // verify method with throws exception may be called without an exception
        remoteExLocal.testMethodwithExceptionAndRemoteSub("none");

        try {
            remoteExLocal.testMethodwithExceptionAndRemoteSub("Exception");
            fail("Expected Exception was not thrown");
        } catch (Exception ex) {
            assertTrue("Exception is not Exception : " + ex.getClass().getName(), ex.getClass() == Exception.class);
            assertEquals("Wrong Exception message received", "Exception", ex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithExceptionAndRemoteSub("IOException");
            fail("Expected IOException was not thrown");
        } catch (IOException ex) {
            assertTrue("Exception is not IOException : " + ex.getClass().getName(), ex.getClass() == IOException.class);
            assertEquals("Wrong Exception message received", "IOException", ex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithExceptionAndRemoteSub("RemoteException");
            fail("Expected RemoteException was not thrown");
        } catch (EJBException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RemoteException : " + rootex.getClass().getName(), rootex.getClass() == RemoteException.class);
            assertEquals("Wrong Exception message received", "RemoteException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithExceptionAndRemoteSub("SLRemoteException");
            fail("Expected SLRemoteException was not thrown");
        } catch (EJBException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not SLRemoteException : " + rootex.getClass().getName(), rootex.getClass() == SLRemoteException.class);
            assertEquals("Wrong Exception message received", "SLRemoteException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithExceptionAndRemoteSub("RuntimeException");
            fail("Expected RuntimeException was not thrown");
        } catch (EJBException ex) { // (UnknownLocalException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RuntimeException : " + rootex.getClass().getName(), rootex.getClass() == RuntimeException.class);
            assertEquals("Wrong Exception message received", "RuntimeException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithExceptionAndRemoteSub("IllegalStateException");
            fail("Expected IllegalStateException was not thrown");
        } catch (EJBException ex) { // (UnknownLocalException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not IllegalStateException : " + rootex.getClass().getName(), rootex.getClass() == IllegalStateException.class);
            assertEquals("Wrong Exception message received", "IllegalStateException", rootex.getMessage());
        }
    }

    @Test
    @ExpectedFFDC({ "java.lang.IllegalStateException", "java.lang.RuntimeException", "java.rmi.RemoteException", "com.ibm.ws.ejbcontainer.exception.ejb.SLRemoteException" })
    public void testLocalMethodwithExceptionAndRemoteAndRemoteSub() throws Exception {
        SLRemoteExLocal remoteExLocal = lookupSLRemoteExBeanLocal();

        // verify method with throws exception may be called without an exception
        remoteExLocal.testMethodwithExceptionAndRemoteAndRemoteSub("none");

        try {
            remoteExLocal.testMethodwithExceptionAndRemoteAndRemoteSub("Exception");
            fail("Expected Exception was not thrown");
        } catch (Exception ex) {
            assertTrue("Exception is not Exception : " + ex.getClass().getName(), ex.getClass() == Exception.class);
            assertEquals("Wrong Exception message received", "Exception", ex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithExceptionAndRemoteAndRemoteSub("IOException");
            fail("Expected IOException was not thrown");
        } catch (IOException ex) {
            assertTrue("Exception is not IOException : " + ex.getClass().getName(), ex.getClass() == IOException.class);
            assertEquals("Wrong Exception message received", "IOException", ex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithExceptionAndRemoteAndRemoteSub("RemoteException");
            fail("Expected RemoteException was not thrown");
        } catch (EJBException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RemoteException : " + rootex.getClass().getName(), rootex.getClass() == RemoteException.class);
            assertEquals("Wrong Exception message received", "RemoteException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithExceptionAndRemoteAndRemoteSub("SLRemoteException");
            fail("Expected SLRemoteException was not thrown");
        } catch (EJBException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not SLRemoteException : " + rootex.getClass().getName(), rootex.getClass() == SLRemoteException.class);
            assertEquals("Wrong Exception message received", "SLRemoteException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithExceptionAndRemoteAndRemoteSub("RuntimeException");
            fail("Expected RuntimeException was not thrown");
        } catch (EJBException ex) { // (UnknownLocalException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RuntimeException : " + rootex.getClass().getName(), rootex.getClass() == RuntimeException.class);
            assertEquals("Wrong Exception message received", "RuntimeException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithExceptionAndRemoteAndRemoteSub("IllegalStateException");
            fail("Expected IllegalStateException was not thrown");
        } catch (EJBException ex) { // (UnknownLocalException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not IllegalStateException : " + rootex.getClass().getName(), rootex.getClass() == IllegalStateException.class);
            assertEquals("Wrong Exception message received", "IllegalStateException", rootex.getMessage());
        }
    }

    @Test
    @ExpectedFFDC({ "java.lang.IllegalStateException", "java.lang.RuntimeException", "java.rmi.RemoteException", "com.ibm.ws.ejbcontainer.exception.ejb.SLRemoteException" })
    public void testLocalMethodwithIOException() throws Exception {
        SLRemoteExLocal remoteExLocal = lookupSLRemoteExBeanLocal();

        // verify method with throws exception may be called without an exception
        remoteExLocal.testMethodwithIOException("none");

        try {
            remoteExLocal.testMethodwithIOException("IOException");
            fail("Expected IOException was not thrown");
        } catch (IOException ex) {
            assertTrue("Exception is not IOException : " + ex.getClass().getName(), ex.getClass() == IOException.class);
            assertEquals("Wrong Exception message received", "IOException", ex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithIOException("RemoteException");
            fail("Expected RemoteException was not thrown");
        } catch (EJBException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RemoteException : " + rootex.getClass().getName(), rootex.getClass() == RemoteException.class);
            assertEquals("Wrong Exception message received", "RemoteException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithIOException("SLRemoteException");
            fail("Expected SLRemoteException was not thrown");
        } catch (EJBException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not SLRemoteException : " + rootex.getClass().getName(), rootex.getClass() == SLRemoteException.class);
            assertEquals("Wrong Exception message received", "SLRemoteException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithIOException("RuntimeException");
            fail("Expected RuntimeException was not thrown");
        } catch (EJBException ex) { // (UnknownLocalException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RuntimeException : " + rootex.getClass().getName(), rootex.getClass() == RuntimeException.class);
            assertEquals("Wrong Exception message received", "RuntimeException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithIOException("IllegalStateException");
            fail("Expected IllegalStateException was not thrown");
        } catch (EJBException ex) { // (UnknownLocalException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not IllegalStateException : " + rootex.getClass().getName(), rootex.getClass() == IllegalStateException.class);
            assertEquals("Wrong Exception message received", "IllegalStateException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithIOException("FileNotFoundException");
            fail("Expected FileNotFoundException was not thrown");
        } catch (FileNotFoundException ex) {
            assertTrue("Exception is not FileNotFoundException : " + ex.getClass().getName(), ex.getClass() == FileNotFoundException.class);
            assertEquals("Wrong Exception message received", "FileNotFoundException", ex.getMessage());
        }
    }

    @Test
    @ExpectedFFDC({ "java.lang.IllegalStateException", "java.lang.RuntimeException", "java.rmi.RemoteException", "com.ibm.ws.ejbcontainer.exception.ejb.SLRemoteException" })
    public void testLocalMethodwithIOExceptionAndRemote() throws Exception {
        SLRemoteExLocal remoteExLocal = lookupSLRemoteExBeanLocal();

        // verify method with throws exception may be called without an exception
        remoteExLocal.testMethodwithIOExceptionAndRemote("none");

        try {
            remoteExLocal.testMethodwithIOExceptionAndRemote("IOException");
            fail("Expected IOException was not thrown");
        } catch (IOException ex) {
            assertTrue("Exception is not IOException : " + ex.getClass().getName(), ex.getClass() == IOException.class);
            assertEquals("Wrong Exception message received", "IOException", ex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithIOExceptionAndRemote("RemoteException");
            fail("Expected RemoteException was not thrown");
        } catch (EJBException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RemoteException : " + rootex.getClass().getName(), rootex.getClass() == RemoteException.class);
            assertEquals("Wrong Exception message received", "RemoteException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithIOExceptionAndRemote("SLRemoteException");
            fail("Expected SLRemoteException was not thrown");
        } catch (EJBException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not SLRemoteException : " + rootex.getClass().getName(), rootex.getClass() == SLRemoteException.class);
            assertEquals("Wrong Exception message received", "SLRemoteException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithIOExceptionAndRemote("RuntimeException");
            fail("Expected RuntimeException was not thrown");
        } catch (EJBException ex) { // (UnknownLocalException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RuntimeException : " + rootex.getClass().getName(), rootex.getClass() == RuntimeException.class);
            assertEquals("Wrong Exception message received", "RuntimeException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithIOExceptionAndRemote("IllegalStateException");
            fail("Expected IllegalStateException was not thrown");
        } catch (EJBException ex) { // (UnknownLocalException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not IllegalStateException : " + rootex.getClass().getName(), rootex.getClass() == IllegalStateException.class);
            assertEquals("Wrong Exception message received", "IllegalStateException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithIOExceptionAndRemote("FileNotFoundException");
            fail("Expected FileNotFoundException was not thrown");
        } catch (FileNotFoundException ex) {
            assertTrue("Exception is not FileNotFoundException : " + ex.getClass().getName(), ex.getClass() == FileNotFoundException.class);
            assertEquals("Wrong Exception message received", "FileNotFoundException", ex.getMessage());
        }
    }

    @Test
    @ExpectedFFDC({ "java.lang.IllegalStateException", "java.lang.RuntimeException", "java.rmi.RemoteException", "com.ibm.ws.ejbcontainer.exception.ejb.SLRemoteException" })
    public void testLocalMethodwithIOExceptionAndRemoteSub() throws Exception {
        SLRemoteExLocal remoteExLocal = lookupSLRemoteExBeanLocal();

        // verify method with throws exception may be called without an exception
        remoteExLocal.testMethodwithIOExceptionAndRemoteSub("none");

        try {
            remoteExLocal.testMethodwithIOExceptionAndRemoteSub("IOException");
            fail("Expected IOException was not thrown");
        } catch (IOException ex) {
            assertTrue("Exception is not IOException : " + ex.getClass().getName(), ex.getClass() == IOException.class);
            assertEquals("Wrong Exception message received", "IOException", ex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithIOExceptionAndRemoteSub("RemoteException");
            fail("Expected RemoteException was not thrown");
        } catch (EJBException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RemoteException : " + rootex.getClass().getName(), rootex.getClass() == RemoteException.class);
            assertEquals("Wrong Exception message received", "RemoteException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithIOExceptionAndRemoteSub("SLRemoteException");
            fail("Expected SLRemoteException was not thrown");
        } catch (EJBException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not SLRemoteException : " + rootex.getClass().getName(), rootex.getClass() == SLRemoteException.class);
            assertEquals("Wrong Exception message received", "SLRemoteException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithIOExceptionAndRemoteSub("RuntimeException");
            fail("Expected RuntimeException was not thrown");
        } catch (EJBException ex) { // (UnknownLocalException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RuntimeException : " + rootex.getClass().getName(), rootex.getClass() == RuntimeException.class);
            assertEquals("Wrong Exception message received", "RuntimeException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithIOExceptionAndRemoteSub("IllegalStateException");
            fail("Expected IllegalStateException was not thrown");
        } catch (EJBException ex) { // (UnknownLocalException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not IllegalStateException : " + rootex.getClass().getName(), rootex.getClass() == IllegalStateException.class);
            assertEquals("Wrong Exception message received", "IllegalStateException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithIOExceptionAndRemoteSub("FileNotFoundException");
            fail("Expected FileNotFoundException was not thrown");
        } catch (FileNotFoundException ex) {
            assertTrue("Exception is not FileNotFoundException : " + ex.getClass().getName(), ex.getClass() == FileNotFoundException.class);
            assertEquals("Wrong Exception message received", "FileNotFoundException", ex.getMessage());
        }
    }

    @Test
    @ExpectedFFDC({ "java.lang.IllegalStateException", "java.lang.RuntimeException", "java.rmi.RemoteException", "com.ibm.ws.ejbcontainer.exception.ejb.SLRemoteException" })
    public void testLocalMethodwithIOExceptionAndRemoteAndRemoteSub() throws Exception {
        SLRemoteExLocal remoteExLocal = lookupSLRemoteExBeanLocal();

        // verify method with throws exception may be called without an exception
        remoteExLocal.testMethodwithIOExceptionAndRemoteAndRemoteSub("none");

        try {
            remoteExLocal.testMethodwithIOExceptionAndRemoteAndRemoteSub("IOException");
            fail("Expected IOException was not thrown");
        } catch (IOException ex) {
            assertTrue("Exception is not IOException : " + ex.getClass().getName(), ex.getClass() == IOException.class);
            assertEquals("Wrong Exception message received", "IOException", ex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithIOExceptionAndRemoteAndRemoteSub("RemoteException");
            fail("Expected RemoteException was not thrown");
        } catch (EJBException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RemoteException : " + rootex.getClass().getName(), rootex.getClass() == RemoteException.class);
            assertEquals("Wrong Exception message received", "RemoteException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithIOExceptionAndRemoteAndRemoteSub("SLRemoteException");
            fail("Expected SLRemoteException was not thrown");
        } catch (EJBException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not SLRemoteException : " + rootex.getClass().getName(), rootex.getClass() == SLRemoteException.class);
            assertEquals("Wrong Exception message received", "SLRemoteException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithIOExceptionAndRemoteAndRemoteSub("RuntimeException");
            fail("Expected RuntimeException was not thrown");
        } catch (EJBException ex) { // (UnknownLocalException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RuntimeException : " + rootex.getClass().getName(), rootex.getClass() == RuntimeException.class);
            assertEquals("Wrong Exception message received", "RuntimeException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithIOExceptionAndRemoteAndRemoteSub("IllegalStateException");
            fail("Expected IllegalStateException was not thrown");
        } catch (EJBException ex) { // (UnknownLocalException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not IllegalStateException : " + rootex.getClass().getName(), rootex.getClass() == IllegalStateException.class);
            assertEquals("Wrong Exception message received", "IllegalStateException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithIOExceptionAndRemoteAndRemoteSub("FileNotFoundException");
            fail("Expected FileNotFoundException was not thrown");
        } catch (FileNotFoundException ex) {
            assertTrue("Exception is not FileNotFoundException : " + ex.getClass().getName(), ex.getClass() == FileNotFoundException.class);
            assertEquals("Wrong Exception message received", "FileNotFoundException", ex.getMessage());
        }
    }

    @Test
    @ExpectedFFDC({ "java.lang.IllegalStateException", "java.lang.RuntimeException", "java.rmi.RemoteException", "com.ibm.ws.ejbcontainer.exception.ejb.SLRemoteException" })
    public void testLocalMethodwithRemoteEx() throws Exception {
        SLRemoteExLocal remoteExLocal = lookupSLRemoteExBeanLocal();

        // verify method with throws exception may be called without an exception
        remoteExLocal.testMethodwithRemoteEx("none");

        try {
            remoteExLocal.testMethodwithRemoteEx("RemoteException");
            fail("Expected RemoteException was not thrown");
        } catch (EJBException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RemoteException : " + rootex.getClass().getName(), rootex.getClass() == RemoteException.class);
            assertEquals("Wrong Exception message received", "RemoteException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithRemoteEx("SLRemoteException");
            fail("Expected SLRemoteException was not thrown");
        } catch (EJBException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not SLRemoteException : " + rootex.getClass().getName(), rootex.getClass() == SLRemoteException.class);
            assertEquals("Wrong Exception message received", "SLRemoteException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithRemoteEx("RuntimeException");
            fail("Expected RuntimeException was not thrown");
        } catch (EJBException ex) { // (UnknownLocalException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RuntimeException : " + rootex.getClass().getName(), rootex.getClass() == RuntimeException.class);
            assertEquals("Wrong Exception message received", "RuntimeException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithRemoteEx("IllegalStateException");
            fail("Expected IllegalStateException was not thrown");
        } catch (EJBException ex) { // (UnknownLocalException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not IllegalStateException : " + rootex.getClass().getName(), rootex.getClass() == IllegalStateException.class);
            assertEquals("Wrong Exception message received", "IllegalStateException", rootex.getMessage());
        }
    }

    @Test
    @ExpectedFFDC({ "java.lang.IllegalStateException", "java.lang.RuntimeException", "java.rmi.RemoteException", "com.ibm.ws.ejbcontainer.exception.ejb.SLRemoteException" })
    public void testLocalMethodwithRemoteExAndSub() throws Exception {
        SLRemoteExLocal remoteExLocal = lookupSLRemoteExBeanLocal();

        // verify method with throws exception may be called without an exception
        remoteExLocal.testMethodwithRemoteExAndSub("none");

        try {
            remoteExLocal.testMethodwithRemoteExAndSub("RemoteException");
            fail("Expected RemoteException was not thrown");
        } catch (EJBException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RemoteException : " + rootex.getClass().getName(), rootex.getClass() == RemoteException.class);
            assertEquals("Wrong Exception message received", "RemoteException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithRemoteExAndSub("SLRemoteException");
            fail("Expected SLRemoteException was not thrown");
        } catch (EJBException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not SLRemoteException : " + rootex.getClass().getName(), rootex.getClass() == SLRemoteException.class);
            assertEquals("Wrong Exception message received", "SLRemoteException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithRemoteExAndSub("RuntimeException");
            fail("Expected RuntimeException was not thrown");
        } catch (EJBException ex) { // (UnknownLocalException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RuntimeException : " + rootex.getClass().getName(), rootex.getClass() == RuntimeException.class);
            assertEquals("Wrong Exception message received", "RuntimeException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithRemoteExAndSub("IllegalStateException");
            fail("Expected IllegalStateException was not thrown");
        } catch (EJBException ex) { // (UnknownLocalException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not IllegalStateException : " + rootex.getClass().getName(), rootex.getClass() == IllegalStateException.class);
            assertEquals("Wrong Exception message received", "IllegalStateException", rootex.getMessage());
        }
    }

    @Test
    @ExpectedFFDC({ "java.lang.IllegalStateException", "java.lang.RuntimeException", "com.ibm.ws.ejbcontainer.exception.ejb.SLRemoteException" })
    public void testLocalMethodwithRemoteExSub() throws Exception {
        SLRemoteExLocal remoteExLocal = lookupSLRemoteExBeanLocal();

        // verify method with throws exception may be called without an exception
        remoteExLocal.testMethodwithRemoteExSub("none");

        try {
            remoteExLocal.testMethodwithRemoteExSub("SLRemoteException");
            fail("Expected SLRemoteException was not thrown");
        } catch (EJBException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not SLRemoteException : " + rootex.getClass().getName(), rootex.getClass() == SLRemoteException.class);
            assertEquals("Wrong Exception message received", "SLRemoteException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithRemoteExSub("RuntimeException");
            fail("Expected RuntimeException was not thrown");
        } catch (EJBException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RuntimeException : " + rootex.getClass().getName(), rootex.getClass() == RuntimeException.class);
            assertEquals("Wrong Exception message received", "RuntimeException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithRemoteExSub("IllegalStateException");
            fail("Expected IllegalStateException was not thrown");
        } catch (EJBException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not IllegalStateException : " + rootex.getClass().getName(), rootex.getClass() == IllegalStateException.class);
            assertEquals("Wrong Exception message received", "IllegalStateException", rootex.getMessage());
        }
    }

    @Test
    @ExpectedFFDC({ "java.lang.IllegalStateException", "java.lang.RuntimeException" })
    public void testLocalRemoteMethodwithNoEx() throws Exception {
        SLRemoteExLocalRemote remoteExLocal = lookupSLRemoteExBeanLocalRemote();

        // verify method with throws exception may be called without an exception
        remoteExLocal.testMethodwithNoEx("none");

        try {
            remoteExLocal.testMethodwithNoEx("RuntimeException");
            fail("Expected RuntimeException was not thrown");
        } catch (EJBException ex) { // (UnknownLocalException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RuntimeException : " + rootex.getClass().getName(), rootex.getClass() == RuntimeException.class);
            assertEquals("Wrong Exception message received", "RuntimeException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithNoEx("IllegalStateException");
            fail("Expected IllegalStateException was not thrown");
        } catch (EJBException ex) { // (UnknownLocalException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not IllegalStateException : " + rootex.getClass().getName(), rootex.getClass() == IllegalStateException.class);
            assertEquals("Wrong Exception message received", "IllegalStateException", rootex.getMessage());
        }
    }

    @Test
    @ExpectedFFDC({ "java.lang.IllegalStateException", "java.lang.RuntimeException", "java.rmi.RemoteException", "com.ibm.ws.ejbcontainer.exception.ejb.SLRemoteException" })
    public void testLocalRemoteMethodwithException() throws Exception {
        SLRemoteExLocalRemote remoteExLocal = lookupSLRemoteExBeanLocalRemote();

        // verify method with throws exception may be called without an exception
        remoteExLocal.testMethodwithException("none");

        try {
            remoteExLocal.testMethodwithException("Exception");
            fail("Expected Exception was not thrown");
        } catch (Exception ex) {
            assertTrue("Exception is not Exception : " + ex.getClass().getName(), ex.getClass() == Exception.class);
            assertEquals("Wrong Exception message received", "Exception", ex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithException("IOException");
            fail("Expected IOException was not thrown");
        } catch (IOException ex) {
            assertTrue("Exception is not IOException : " + ex.getClass().getName(), ex.getClass() == IOException.class);
            assertEquals("Wrong Exception message received", "IOException", ex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithException("RemoteException");
            fail("Expected RemoteException was not thrown");
        } catch (EJBException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RemoteException : " + rootex.getClass().getName(), rootex.getClass() == RemoteException.class);
            assertEquals("Wrong Exception message received", "RemoteException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithException("SLRemoteException");
            fail("Expected SLRemoteException was not thrown");
        } catch (EJBException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not SLRemoteException : " + rootex.getClass().getName(), rootex.getClass() == SLRemoteException.class);
            assertEquals("Wrong Exception message received", "SLRemoteException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithException("RuntimeException");
            fail("Expected RuntimeException was not thrown");
        } catch (EJBException ex) { // (UnknownLocalException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RuntimeException : " + rootex.getClass().getName(), rootex.getClass() == RuntimeException.class);
            assertEquals("Wrong Exception message received", "RuntimeException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithException("IllegalStateException");
            fail("Expected IllegalStateException was not thrown");
        } catch (EJBException ex) { // (UnknownLocalException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not IllegalStateException : " + rootex.getClass().getName(), rootex.getClass() == IllegalStateException.class);
            assertEquals("Wrong Exception message received", "IllegalStateException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithException("SQLException");
            fail("Expected SQLException was not thrown");
        } catch (SQLException ex) {
            assertTrue("Exception is not SQLException : " + ex.getClass().getName(), ex.getClass() == SQLException.class);
            assertEquals("Wrong Exception message received", "SQLException", ex.getMessage());
        }
    }

    @Test
    @ExpectedFFDC({ "java.lang.IllegalStateException", "java.lang.RuntimeException", "java.rmi.RemoteException", "com.ibm.ws.ejbcontainer.exception.ejb.SLRemoteException" })
    public void testLocalRemoteMethodwithExceptionAndRemote() throws Exception {
        SLRemoteExLocalRemote remoteExLocal = lookupSLRemoteExBeanLocalRemote();

        // verify method with throws exception may be called without an exception
        remoteExLocal.testMethodwithExceptionAndRemote("none");

        try {
            remoteExLocal.testMethodwithExceptionAndRemote("Exception");
            fail("Expected Exception was not thrown");
        } catch (Exception ex) {
            assertTrue("Exception is not Exception : " + ex.getClass().getName(), ex.getClass() == Exception.class);
            assertEquals("Wrong Exception message received", "Exception", ex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithExceptionAndRemote("IOException");
            fail("Expected IOException was not thrown");
        } catch (IOException ex) {
            assertTrue("Exception is not IOException : " + ex.getClass().getName(), ex.getClass() == IOException.class);
            assertEquals("Wrong Exception message received", "IOException", ex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithExceptionAndRemote("RemoteException");
            fail("Expected RemoteException was not thrown");
        } catch (EJBException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RemoteException : " + rootex.getClass().getName(), rootex.getClass() == RemoteException.class);
            assertEquals("Wrong Exception message received", "RemoteException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithExceptionAndRemote("SLRemoteException");
            fail("Expected SLRemoteException was not thrown");
        } catch (EJBException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not SLRemoteException : " + rootex.getClass().getName(), rootex.getClass() == SLRemoteException.class);
            assertEquals("Wrong Exception message received", "SLRemoteException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithExceptionAndRemote("RuntimeException");
            fail("Expected RuntimeException was not thrown");
        } catch (EJBException ex) { // (UnknownLocalException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RuntimeException : " + rootex.getClass().getName(), rootex.getClass() == RuntimeException.class);
            assertEquals("Wrong Exception message received", "RuntimeException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithExceptionAndRemote("IllegalStateException");
            fail("Expected IllegalStateException was not thrown");
        } catch (EJBException ex) { // (UnknownLocalException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not IllegalStateException : " + rootex.getClass().getName(), rootex.getClass() == IllegalStateException.class);
            assertEquals("Wrong Exception message received", "IllegalStateException", rootex.getMessage());
        }
    }

    @Test
    @ExpectedFFDC({ "java.lang.IllegalStateException", "java.lang.RuntimeException", "java.rmi.RemoteException", "com.ibm.ws.ejbcontainer.exception.ejb.SLRemoteException" })
    public void testLocalRemoteMethodwithExceptionAndRemoteSub() throws Exception {
        SLRemoteExLocalRemote remoteExLocal = lookupSLRemoteExBeanLocalRemote();

        // verify method with throws exception may be called without an exception
        remoteExLocal.testMethodwithExceptionAndRemoteSub("none");

        try {
            remoteExLocal.testMethodwithExceptionAndRemoteSub("Exception");
            fail("Expected Exception was not thrown");
        } catch (Exception ex) {
            assertTrue("Exception is not Exception : " + ex.getClass().getName(), ex.getClass() == Exception.class);
            assertEquals("Wrong Exception message received", "Exception", ex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithExceptionAndRemoteSub("IOException");
            fail("Expected IOException was not thrown");
        } catch (IOException ex) {
            assertTrue("Exception is not IOException : " + ex.getClass().getName(), ex.getClass() == IOException.class);
            assertEquals("Wrong Exception message received", "IOException", ex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithExceptionAndRemoteSub("RemoteException");
            fail("Expected RemoteException was not thrown");
        } catch (EJBException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RemoteException : " + rootex.getClass().getName(), rootex.getClass() == RemoteException.class);
            assertEquals("Wrong Exception message received", "RemoteException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithExceptionAndRemoteSub("SLRemoteException");
            fail("Expected SLRemoteException was not thrown");
        } catch (EJBException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not SLRemoteException : " + rootex.getClass().getName(), rootex.getClass() == SLRemoteException.class);
            assertEquals("Wrong Exception message received", "SLRemoteException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithExceptionAndRemoteSub("RuntimeException");
            fail("Expected RuntimeException was not thrown");
        } catch (EJBException ex) { // (UnknownLocalException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RuntimeException : " + rootex.getClass().getName(), rootex.getClass() == RuntimeException.class);
            assertEquals("Wrong Exception message received", "RuntimeException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithExceptionAndRemoteSub("IllegalStateException");
            fail("Expected IllegalStateException was not thrown");
        } catch (EJBException ex) { // (UnknownLocalException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not IllegalStateException : " + rootex.getClass().getName(), rootex.getClass() == IllegalStateException.class);
            assertEquals("Wrong Exception message received", "IllegalStateException", rootex.getMessage());
        }
    }

    @Test
    @ExpectedFFDC({ "java.lang.IllegalStateException", "java.lang.RuntimeException", "java.rmi.RemoteException", "com.ibm.ws.ejbcontainer.exception.ejb.SLRemoteException" })
    public void testLocalRemoteMethodwithExceptionAndRemoteAndRemoteSub() throws Exception {
        SLRemoteExLocalRemote remoteExLocal = lookupSLRemoteExBeanLocalRemote();

        // verify method with throws exception may be called without an exception
        remoteExLocal.testMethodwithExceptionAndRemoteAndRemoteSub("none");

        try {
            remoteExLocal.testMethodwithExceptionAndRemoteAndRemoteSub("Exception");
            fail("Expected Exception was not thrown");
        } catch (Exception ex) {
            assertTrue("Exception is not Exception : " + ex.getClass().getName(), ex.getClass() == Exception.class);
            assertEquals("Wrong Exception message received", "Exception", ex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithExceptionAndRemoteAndRemoteSub("IOException");
            fail("Expected IOException was not thrown");
        } catch (IOException ex) {
            assertTrue("Exception is not IOException : " + ex.getClass().getName(), ex.getClass() == IOException.class);
            assertEquals("Wrong Exception message received", "IOException", ex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithExceptionAndRemoteAndRemoteSub("RemoteException");
            fail("Expected RemoteException was not thrown");
        } catch (EJBException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RemoteException : " + rootex.getClass().getName(), rootex.getClass() == RemoteException.class);
            assertEquals("Wrong Exception message received", "RemoteException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithExceptionAndRemoteAndRemoteSub("SLRemoteException");
            fail("Expected SLRemoteException was not thrown");
        } catch (EJBException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not SLRemoteException : " + rootex.getClass().getName(), rootex.getClass() == SLRemoteException.class);
            assertEquals("Wrong Exception message received", "SLRemoteException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithExceptionAndRemoteAndRemoteSub("RuntimeException");
            fail("Expected RuntimeException was not thrown");
        } catch (EJBException ex) { // (UnknownLocalException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RuntimeException : " + rootex.getClass().getName(), rootex.getClass() == RuntimeException.class);
            assertEquals("Wrong Exception message received", "RuntimeException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithExceptionAndRemoteAndRemoteSub("IllegalStateException");
            fail("Expected IllegalStateException was not thrown");
        } catch (EJBException ex) { // (UnknownLocalException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not IllegalStateException : " + rootex.getClass().getName(), rootex.getClass() == IllegalStateException.class);
            assertEquals("Wrong Exception message received", "IllegalStateException", rootex.getMessage());
        }
    }

    @Test
    @ExpectedFFDC({ "java.lang.IllegalStateException", "java.lang.RuntimeException", "java.rmi.RemoteException", "com.ibm.ws.ejbcontainer.exception.ejb.SLRemoteException" })
    public void testLocalRemoteMethodwithIOException() throws Exception {
        SLRemoteExLocalRemote remoteExLocal = lookupSLRemoteExBeanLocalRemote();

        // verify method with throws exception may be called without an exception
        remoteExLocal.testMethodwithIOException("none");

        try {
            remoteExLocal.testMethodwithIOException("IOException");
            fail("Expected IOException was not thrown");
        } catch (IOException ex) {
            assertTrue("Exception is not IOException : " + ex.getClass().getName(), ex.getClass() == IOException.class);
            assertEquals("Wrong Exception message received", "IOException", ex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithIOException("RemoteException");
            fail("Expected RemoteException was not thrown");
        } catch (EJBException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RemoteException : " + rootex.getClass().getName(), rootex.getClass() == RemoteException.class);
            assertEquals("Wrong Exception message received", "RemoteException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithIOException("SLRemoteException");
            fail("Expected SLRemoteException was not thrown");
        } catch (EJBException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not SLRemoteException : " + rootex.getClass().getName(), rootex.getClass() == SLRemoteException.class);
            assertEquals("Wrong Exception message received", "SLRemoteException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithIOException("RuntimeException");
            fail("Expected RuntimeException was not thrown");
        } catch (EJBException ex) { // (UnknownLocalException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RuntimeException : " + rootex.getClass().getName(), rootex.getClass() == RuntimeException.class);
            assertEquals("Wrong Exception message received", "RuntimeException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithIOException("IllegalStateException");
            fail("Expected IllegalStateException was not thrown");
        } catch (EJBException ex) { // (UnknownLocalException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not IllegalStateException : " + rootex.getClass().getName(), rootex.getClass() == IllegalStateException.class);
            assertEquals("Wrong Exception message received", "IllegalStateException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithIOException("FileNotFoundException");
            fail("Expected FileNotFoundException was not thrown");
        } catch (FileNotFoundException ex) {
            assertTrue("Exception is not FileNotFoundException : " + ex.getClass().getName(), ex.getClass() == FileNotFoundException.class);
            assertEquals("Wrong Exception message received", "FileNotFoundException", ex.getMessage());
        }
    }

    @Test
    @ExpectedFFDC({ "java.lang.IllegalStateException", "java.lang.RuntimeException", "java.rmi.RemoteException", "com.ibm.ws.ejbcontainer.exception.ejb.SLRemoteException" })
    public void testLocalRemoteMethodwithIOExceptionAndRemote() throws Exception {
        SLRemoteExLocalRemote remoteExLocal = lookupSLRemoteExBeanLocalRemote();

        // verify method with throws exception may be called without an exception
        remoteExLocal.testMethodwithIOExceptionAndRemote("none");

        try {
            remoteExLocal.testMethodwithIOExceptionAndRemote("IOException");
            fail("Expected IOException was not thrown");
        } catch (IOException ex) {
            assertTrue("Exception is not IOException : " + ex.getClass().getName(), ex.getClass() == IOException.class);
            assertEquals("Wrong Exception message received", "IOException", ex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithIOExceptionAndRemote("RemoteException");
            fail("Expected RemoteException was not thrown");
        } catch (EJBException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RemoteException : " + rootex.getClass().getName(), rootex.getClass() == RemoteException.class);
            assertEquals("Wrong Exception message received", "RemoteException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithIOExceptionAndRemote("SLRemoteException");
            fail("Expected SLRemoteException was not thrown");
        } catch (EJBException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not SLRemoteException : " + rootex.getClass().getName(), rootex.getClass() == SLRemoteException.class);
            assertEquals("Wrong Exception message received", "SLRemoteException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithIOExceptionAndRemote("RuntimeException");
            fail("Expected RuntimeException was not thrown");
        } catch (EJBException ex) { // (UnknownLocalException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RuntimeException : " + rootex.getClass().getName(), rootex.getClass() == RuntimeException.class);
            assertEquals("Wrong Exception message received", "RuntimeException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithIOExceptionAndRemote("IllegalStateException");
            fail("Expected IllegalStateException was not thrown");
        } catch (EJBException ex) { // (UnknownLocalException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not IllegalStateException : " + rootex.getClass().getName(), rootex.getClass() == IllegalStateException.class);
            assertEquals("Wrong Exception message received", "IllegalStateException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithIOExceptionAndRemote("FileNotFoundException");
            fail("Expected FileNotFoundException was not thrown");
        } catch (FileNotFoundException ex) {
            assertTrue("Exception is not FileNotFoundException : " + ex.getClass().getName(), ex.getClass() == FileNotFoundException.class);
            assertEquals("Wrong Exception message received", "FileNotFoundException", ex.getMessage());
        }
    }

    @Test
    @ExpectedFFDC({ "java.lang.IllegalStateException", "java.lang.RuntimeException", "java.rmi.RemoteException", "com.ibm.ws.ejbcontainer.exception.ejb.SLRemoteException" })
    public void testLocalRemoteMethodwithIOExceptionAndRemoteSub() throws Exception {
        SLRemoteExLocalRemote remoteExLocal = lookupSLRemoteExBeanLocalRemote();

        // verify method with throws exception may be called without an exception
        remoteExLocal.testMethodwithIOExceptionAndRemoteSub("none");

        try {
            remoteExLocal.testMethodwithIOExceptionAndRemoteSub("IOException");
            fail("Expected IOException was not thrown");
        } catch (IOException ex) {
            assertTrue("Exception is not IOException : " + ex.getClass().getName(), ex.getClass() == IOException.class);
            assertEquals("Wrong Exception message received", "IOException", ex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithIOExceptionAndRemoteSub("RemoteException");
            fail("Expected RemoteException was not thrown");
        } catch (EJBException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RemoteException : " + rootex.getClass().getName(), rootex.getClass() == RemoteException.class);
            assertEquals("Wrong Exception message received", "RemoteException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithIOExceptionAndRemoteSub("SLRemoteException");
            fail("Expected SLRemoteException was not thrown");
        } catch (EJBException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not SLRemoteException : " + rootex.getClass().getName(), rootex.getClass() == SLRemoteException.class);
            assertEquals("Wrong Exception message received", "SLRemoteException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithIOExceptionAndRemoteSub("RuntimeException");
            fail("Expected RuntimeException was not thrown");
        } catch (EJBException ex) { // (UnknownLocalException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RuntimeException : " + rootex.getClass().getName(), rootex.getClass() == RuntimeException.class);
            assertEquals("Wrong Exception message received", "RuntimeException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithIOExceptionAndRemoteSub("IllegalStateException");
            fail("Expected IllegalStateException was not thrown");
        } catch (EJBException ex) { // (UnknownLocalException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not IllegalStateException : " + rootex.getClass().getName(), rootex.getClass() == IllegalStateException.class);
            assertEquals("Wrong Exception message received", "IllegalStateException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithIOExceptionAndRemoteSub("FileNotFoundException");
            fail("Expected FileNotFoundException was not thrown");
        } catch (FileNotFoundException ex) {
            assertTrue("Exception is not FileNotFoundException : " + ex.getClass().getName(), ex.getClass() == FileNotFoundException.class);
            assertEquals("Wrong Exception message received", "FileNotFoundException", ex.getMessage());
        }
    }

    @Test
    @ExpectedFFDC({ "java.lang.IllegalStateException", "java.lang.RuntimeException", "java.rmi.RemoteException", "com.ibm.ws.ejbcontainer.exception.ejb.SLRemoteException" })
    public void testLocalRemoteMethodwithIOExceptionAndRemoteAndRemoteSub() throws Exception {
        SLRemoteExLocalRemote remoteExLocal = lookupSLRemoteExBeanLocalRemote();

        // verify method with throws exception may be called without an exception
        remoteExLocal.testMethodwithIOExceptionAndRemoteAndRemoteSub("none");

        try {
            remoteExLocal.testMethodwithIOExceptionAndRemoteAndRemoteSub("IOException");
            fail("Expected IOException was not thrown");
        } catch (IOException ex) {
            assertTrue("Exception is not IOException : " + ex.getClass().getName(), ex.getClass() == IOException.class);
            assertEquals("Wrong Exception message received", "IOException", ex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithIOExceptionAndRemoteAndRemoteSub("RemoteException");
            fail("Expected RemoteException was not thrown");
        } catch (EJBException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RemoteException : " + rootex.getClass().getName(), rootex.getClass() == RemoteException.class);
            assertEquals("Wrong Exception message received", "RemoteException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithIOExceptionAndRemoteAndRemoteSub("SLRemoteException");
            fail("Expected SLRemoteException was not thrown");
        } catch (EJBException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not SLRemoteException : " + rootex.getClass().getName(), rootex.getClass() == SLRemoteException.class);
            assertEquals("Wrong Exception message received", "SLRemoteException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithIOExceptionAndRemoteAndRemoteSub("RuntimeException");
            fail("Expected RuntimeException was not thrown");
        } catch (EJBException ex) { // (UnknownLocalException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RuntimeException : " + rootex.getClass().getName(), rootex.getClass() == RuntimeException.class);
            assertEquals("Wrong Exception message received", "RuntimeException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithIOExceptionAndRemoteAndRemoteSub("IllegalStateException");
            fail("Expected IllegalStateException was not thrown");
        } catch (EJBException ex) { // (UnknownLocalException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not IllegalStateException : " + rootex.getClass().getName(), rootex.getClass() == IllegalStateException.class);
            assertEquals("Wrong Exception message received", "IllegalStateException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithIOExceptionAndRemoteAndRemoteSub("FileNotFoundException");
            fail("Expected FileNotFoundException was not thrown");
        } catch (FileNotFoundException ex) {
            assertTrue("Exception is not FileNotFoundException : " + ex.getClass().getName(), ex.getClass() == FileNotFoundException.class);
            assertEquals("Wrong Exception message received", "FileNotFoundException", ex.getMessage());
        }
    }

    @Test
    @ExpectedFFDC({ "java.lang.IllegalStateException", "java.lang.RuntimeException", "java.rmi.RemoteException", "com.ibm.ws.ejbcontainer.exception.ejb.SLRemoteException" })
    public void testLocalRemoteMethodwithRemoteEx() throws Exception {
        SLRemoteExLocalRemote remoteExLocal = lookupSLRemoteExBeanLocalRemote();

        // verify method with throws exception may be called without an exception
        remoteExLocal.testMethodwithRemoteEx("none");

        try {
            remoteExLocal.testMethodwithRemoteEx("RemoteException");
            fail("Expected RemoteException was not thrown");
        } catch (EJBException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RemoteException : " + rootex.getClass().getName(), rootex.getClass() == RemoteException.class);
            assertEquals("Wrong Exception message received", "RemoteException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithRemoteEx("SLRemoteException");
            fail("Expected SLRemoteException was not thrown");
        } catch (EJBException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not SLRemoteException : " + rootex.getClass().getName(), rootex.getClass() == SLRemoteException.class);
            assertEquals("Wrong Exception message received", "SLRemoteException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithRemoteEx("RuntimeException");
            fail("Expected RuntimeException was not thrown");
        } catch (EJBException ex) { // (UnknownLocalException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RuntimeException : " + rootex.getClass().getName(), rootex.getClass() == RuntimeException.class);
            assertEquals("Wrong Exception message received", "RuntimeException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithRemoteEx("IllegalStateException");
            fail("Expected IllegalStateException was not thrown");
        } catch (EJBException ex) { // (UnknownLocalException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not IllegalStateException : " + rootex.getClass().getName(), rootex.getClass() == IllegalStateException.class);
            assertEquals("Wrong Exception message received", "IllegalStateException", rootex.getMessage());
        }
    }

    @Test
    @ExpectedFFDC({ "java.lang.IllegalStateException", "java.lang.RuntimeException", "java.rmi.RemoteException", "com.ibm.ws.ejbcontainer.exception.ejb.SLRemoteException" })
    public void testLocalRemoteMethodwithRemoteExAndSub() throws Exception {
        SLRemoteExLocalRemote remoteExLocal = lookupSLRemoteExBeanLocalRemote();

        // verify method with throws exception may be called without an exception
        remoteExLocal.testMethodwithRemoteExAndSub("none");

        try {
            remoteExLocal.testMethodwithRemoteExAndSub("RemoteException");
            fail("Expected RemoteException was not thrown");
        } catch (EJBException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RemoteException : " + rootex.getClass().getName(), rootex.getClass() == RemoteException.class);
            assertEquals("Wrong Exception message received", "RemoteException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithRemoteExAndSub("SLRemoteException");
            fail("Expected SLRemoteException was not thrown");
        } catch (EJBException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not SLRemoteException : " + rootex.getClass().getName(), rootex.getClass() == SLRemoteException.class);
            assertEquals("Wrong Exception message received", "SLRemoteException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithRemoteExAndSub("RuntimeException");
            fail("Expected RuntimeException was not thrown");
        } catch (EJBException ex) { // (UnknownLocalException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RuntimeException : " + rootex.getClass().getName(), rootex.getClass() == RuntimeException.class);
            assertEquals("Wrong Exception message received", "RuntimeException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithRemoteExAndSub("IllegalStateException");
            fail("Expected IllegalStateException was not thrown");
        } catch (EJBException ex) { // (UnknownLocalException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not IllegalStateException : " + rootex.getClass().getName(), rootex.getClass() == IllegalStateException.class);
            assertEquals("Wrong Exception message received", "IllegalStateException", rootex.getMessage());
        }
    }

    @Test
    @ExpectedFFDC({ "java.lang.IllegalStateException", "java.lang.RuntimeException", "com.ibm.ws.ejbcontainer.exception.ejb.SLRemoteException" })
    public void testLocalRemoteMethodwithRemoteExSub() throws Exception {
        SLRemoteExLocalRemote remoteExLocal = lookupSLRemoteExBeanLocalRemote();

        // verify method with throws exception may be called without an exception
        remoteExLocal.testMethodwithRemoteExSub("none");

        try {
            remoteExLocal.testMethodwithRemoteExSub("SLRemoteException");
            fail("Expected SLRemoteException was not thrown");
        } catch (EJBException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not SLRemoteException : " + rootex.getClass().getName(), rootex.getClass() == SLRemoteException.class);
            assertEquals("Wrong Exception message received", "SLRemoteException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithRemoteExSub("RuntimeException");
            fail("Expected RuntimeException was not thrown");
        } catch (EJBException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not RuntimeException : " + rootex.getClass().getName(), rootex.getClass() == RuntimeException.class);
            assertEquals("Wrong Exception message received", "RuntimeException", rootex.getMessage());
        }

        try {
            remoteExLocal.testMethodwithRemoteExSub("IllegalStateException");
            fail("Expected IllegalStateException was not thrown");
        } catch (EJBException ex) {
            Throwable rootex = ex.getCause();
            assertTrue("Exception is not IllegalStateException : " + rootex.getClass().getName(), rootex.getClass() == IllegalStateException.class);
            assertEquals("Wrong Exception message received", "IllegalStateException", rootex.getMessage());
        }
    }

}
