/*******************************************************************************
 * Copyright (c) 2006, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.fat.xml.sf.ejb;

import java.rmi.RemoteException;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.RemoveException;
import javax.ejb.SessionContext;
import javax.ejb.SessionSynchronization;

/**
 * Bean implementation class for Enterprise Bean: RemoveCMTLocal / Remote
 **/
public class RemoveCompViewCMTBean implements SessionSynchronization {

    private SessionContext ivContext;

    private String ivString = "RemoveCompViewCMTBean";

    /** Return the String value state of Stateful bean. **/

    public String getString() {
        return ivString;
    }

    /** Just another method to be different :-) **/
    public String howdy(String name) {
        ivString += ":Hi " + name + "!";
        return ivString;
    }

    public String remove(int x, int y) {
        int sum = x + y;
        ivString += ":remove:" + sum;
        return (ivString);
    }

    /** Remove method with default REQUIRED transaction attribute. **/
    public String remove(String string) throws TestAppException {
        ivString += ":remove:" + string;

        if (string != null) {
            if (string.contains("ROLLBACK"))
                ivContext.setRollbackOnly();
            if (string.contains("EJBException"))
                throw new EJBException("Test Exception");
            else if (string.contains("AppException"))
                throw new TestAppException();
        }

        return (ivString);
    }

    /** Remove method with REQUIRED transaction attribute. **/
    public String remove_Required(String string) throws TestAppException {
        ivString += ":remove_Required:" + string;

        if (string != null) {
            if (string.contains("ROLLBACK"))
                ivContext.setRollbackOnly();
            if (string.contains("EJBException"))
                throw new EJBException("Test Exception");
            else if (string.contains("AppException"))
                throw new TestAppException();
        }

        return (ivString);
    }

    /** Remove method with REQUIRES_NEW transaction attribute. **/
    public String remove_RequiresNew(String string) {
        ivString += ":remove_RequiresNew:" + string;

        if (string != null) {
            if (string.contains("ROLLBACK"))
                ivContext.setRollbackOnly();
            if (string.contains("EJBException"))
                throw new EJBException("Test Exception");
        }

        return (ivString);
    }

    /** Remove method with NOT_SUPPORTED transaction attribute. **/
    public String remove_NotSupported(String string) throws TestAppException {
        ivString += ":remove_NotSupported:" + string;

        if (string != null) {
            if (string.contains("EJBException"))
                throw new EJBException("Test Exception");
            else if (string.contains("AppException"))
                throw new TestAppException();
        }

        return (ivString);
    }

    /** Remove method with SUPPORTS transaction attribute. **/
    public String remove_Supports(String string) {
        ivString += ":remove_Supports:" + string;

        if (string != null) {
            if (string.contains("ROLLBACK"))
                ivContext.setRollbackOnly();
            if (string.contains("EJBException"))
                throw new EJBException("Test Exception");
        }

        return (ivString);
    }

    /** Remove method with MANDATORY transaction attribute. **/
    public String remove_Mandatory(String string) {
        ivString += ":remove_Mandatory:" + string;

        if (string != null) {
            if (string.contains("ROLLBACK"))
                ivContext.setRollbackOnly();
            if (string.contains("EJBException"))
                throw new EJBException("Test Exception");
        }

        return (ivString);
    }

    /** Remove method with NEVER transaction attribute. **/
    public String remove_Never(String string) {
        ivString += ":remove_Never:" + string;

        if (string != null) {
            if (string.contains("EJBException"))
                throw new EJBException("Test Exception");
        }

        return (ivString);
    }

    /** Remove method with RETAIN & REQUIRED transaction attribute. **/
    public String remove_retain_Required(String string) throws TestAppException {
        ivString += ":remove_retain_Required:" + string;

        if (string != null) {
            if (string.contains("ROLLBACK"))
                ivContext.setRollbackOnly();
            if (string.contains("EJBException"))
                throw new EJBException("Test Exception");
            else if (string.contains("AppException"))
                throw new TestAppException();
        }

        return (ivString);
    }

    /** Remove method with RETAIN & NOT_SUPPORTED transaction attribute. **/
    public String remove_retain_NotSupported(String string) throws TestAppException {
        ivString += ":remove_retain_NotSupported:" + string;

        if (string != null) {
            if (string.contains("EJBException"))
                throw new EJBException("Test Exception");
            else if (string.contains("AppException"))
                throw new TestAppException();
        }

        return (ivString);
    }

    /** Remove method with REQUIRED transaction attribute. **/
    public String remove_Required_RemoveEx() throws RemoveException {
        throw new RemoveException(ivString + ":remove_Required_RemoveEx");
    }

    // --------------------------------------------------------------------------
    // SessionSynchronization Interface Methods
    // --------------------------------------------------------------------------
    @Override
    public void afterBegin() throws EJBException, RemoteException {
        ivString += ":afterBegin";
    }

    @Override
    public void beforeCompletion() throws EJBException, RemoteException {
        ivString += ":beforeCompletion";
    }

    @Override
    public void afterCompletion(boolean committed) throws EJBException, RemoteException {
        ivString += ":afterCompletion:" + committed;
    }

    /** Required default constructor **/
    public RemoveCompViewCMTBean() {
    }

    public void ejbCreate() throws CreateException {
    }

    public void ejbCreate(String string) throws CreateException {
        ivString += string;
    }
}
