/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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

import javax.naming.InitialContext;
import javax.rmi.PortableRemoteObject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.ejbcontainer.exception.ejb.SLRemoteEx;
import com.ibm.ws.ejbcontainer.exception.ejb.SLRemoteExHome;
import com.ibm.ws.ejbcontainer.exception.ejb.SLRemoteException;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/ExceptionServlet")
public class ExceptionServlet extends FATServlet {

    private SLRemoteEx lookupSLRemoteExBean() throws Exception {
        Object stub = new InitialContext().lookup("java:app/ExceptionBean/SLRemoteExBean");
        SLRemoteExHome home = (SLRemoteExHome) PortableRemoteObject.narrow(stub, SLRemoteExHome.class);
        return home.create();
    }

    @Test
    @ExpectedFFDC({ "java.lang.IllegalStateException", "java.lang.RuntimeException", "java.rmi.RemoteException" })
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
            assertTrue("Exception is not IllegalStateException : " + ex.getClass().getName(), rootex.getClass() == IllegalStateException.class);
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
            assertTrue("Exception is not IllegalStateException : " + ex.getClass().getName(), rootex.getClass() == IllegalStateException.class);
            assertEquals("Wrong Exception message received", "IllegalStateException", rootex.getMessage());
        }
    }

    @Test
    @ExpectedFFDC({ "java.lang.IllegalStateException", "java.lang.RuntimeException" })
    public void testRMIRemoteMethodwithExceptionAndRemote() throws Exception {
        SLRemoteEx remoteEx = lookupSLRemoteExBean();

        // verify method with throws exception may be called without an exception
        remoteEx.testMethodwithExceptionAndRemote("none");

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
            assertTrue("Exception is not IllegalStateException : " + ex.getClass().getName(), rootex.getClass() == IllegalStateException.class);
            assertEquals("Wrong Exception message received", "IllegalStateException", rootex.getMessage());
        }
    }

}
