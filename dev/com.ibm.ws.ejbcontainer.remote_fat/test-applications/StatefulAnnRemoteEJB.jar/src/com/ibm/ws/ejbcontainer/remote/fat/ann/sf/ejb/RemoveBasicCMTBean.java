/*******************************************************************************
 * Copyright (c) 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.fat.ann.sf.ejb;

import static javax.ejb.TransactionAttributeType.MANDATORY;
import static javax.ejb.TransactionAttributeType.NEVER;
import static javax.ejb.TransactionAttributeType.NOT_SUPPORTED;
import static javax.ejb.TransactionAttributeType.REQUIRED;
import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;
import static javax.ejb.TransactionAttributeType.SUPPORTS;
import static javax.ejb.TransactionManagementType.CONTAINER;

import java.rmi.RemoteException;

import javax.annotation.Resource;
import javax.ejb.EJBException;
import javax.ejb.Remote;
import javax.ejb.Remove;
import javax.ejb.RemoveException;
import javax.ejb.SessionContext;
import javax.ejb.SessionSynchronization;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionManagement;

/**
 * Bean implementation class for Enterprise Bean: RemoveCMTLocal / Remote
 **/
@Stateful(name = "RemoveBasicCMTBean")
@Remote(RemoveCMTRemote.class)
@TransactionManagement(CONTAINER)
public class RemoveBasicCMTBean implements SessionSynchronization {

    @Resource
    private SessionContext ivContext;

    private String ivString = "RemoveBasicCMTBean";

    /** Return the String value state of Stateful bean. **/
    @TransactionAttribute(SUPPORTS)
    public String getString() {
        return ivString;
    }

    /** Remove method with default REQUIRED transaction attribute. **/
    @Remove
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
    @Remove
    @TransactionAttribute(REQUIRED)
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
    @Remove
    @TransactionAttribute(REQUIRES_NEW)
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
    @Remove
    @TransactionAttribute(NOT_SUPPORTED)
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
    @Remove
    @TransactionAttribute(SUPPORTS)
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
    @Remove
    @TransactionAttribute(MANDATORY)
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
    @Remove
    @TransactionAttribute(NEVER)
    public String remove_Never(String string) {
        ivString += ":remove_Never:" + string;

        if (string != null) {
            if (string.contains("EJBException"))
                throw new EJBException("Test Exception");
        }

        return (ivString);
    }

    /** Remove method with RETAIN & REQUIRED transaction attribute. **/
    @Remove(retainIfException = true)
    @TransactionAttribute(REQUIRED)
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
    @Remove(retainIfException = true)
    @TransactionAttribute(NOT_SUPPORTED)
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
    @Remove
    @TransactionAttribute(REQUIRED)
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
    public RemoveBasicCMTBean() {
    }
}
