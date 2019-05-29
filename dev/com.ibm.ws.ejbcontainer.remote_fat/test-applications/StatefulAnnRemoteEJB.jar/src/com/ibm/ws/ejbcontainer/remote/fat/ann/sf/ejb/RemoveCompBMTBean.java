/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.fat.ann.sf.ejb;

import static javax.ejb.TransactionManagementType.BEAN;

import javax.annotation.PostConstruct;
import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.Remote;
import javax.ejb.RemoteHome;
import javax.ejb.Remove;
import javax.ejb.RemoveException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.transaction.UserTransaction;

/**
 * Bean implementation class for Enterprise Bean: RemoveBMTLocal
 **/
@SuppressWarnings("serial")
@Stateful(name = "RemoveCompBMTBean")
@Remote(RemoveBMTRemote.class)
@RemoteHome(RemoveBMTEJBRemoteHome.class)
@TransactionManagement(BEAN)
public class RemoveCompBMTBean implements SessionBean {
    private SessionContext ivContext;

    private UserTransaction ivUserTran;

    private String ivString = "RemoveCompBMTBean";

    @PostConstruct
    private void initUserTransaction() {
        ivUserTran = ivContext.getUserTransaction();
    }

    /** Begins a 'sticky' global transaction. **/
    public String begin(String string) {
        ivString += ":begin:" + string;

        try {
            ivUserTran.begin();
        } catch (Exception ex) {
            throw new EJBException("UserTran Error : ", ex);
        }

        return ivString;
    }

    /** Commits a 'sticky' global transaction. **/
    public String commit(String string) {
        ivString += ":commit:" + string;

        try {
            ivUserTran.commit();
        } catch (Exception ex) {
            throw new EJBException("UserTran Error : ", ex);
        }

        return ivString;
    }

    /** Rolls back a 'sticky' global transaction. **/
    public String rollback(String string) {
        ivString += ":rollback:" + string;

        try {
            ivUserTran.rollback();
        } catch (Exception ex) {
            throw new EJBException("UserTran Error : ", ex);
        }

        return ivString;
    }

    /** Return the String value state of Stateful bean - no tx change. **/
    public String getString() {
        return ivString;
    }

    /** Just another method to be different :-) **/
    public String howdy(String name) {
        ivString += ":Hi " + name + "!";
        return ivString;
    }

    /** Remove method with no transaction context change. **/
    @Remove
    public String remove(String string) throws TestAppException {
        ivString += ":remove:" + string;

        if (string != null) {
            if (string.contains("EJBException"))
                throw new EJBException("Test Exception");
            else if (string.contains("AppException"))
                throw new TestAppException();
        }

        return (ivString);
    }

    @Remove
    public String remove_begin(String string) {
        ivString += ":remove_begin:" + string;

        try {
            ivUserTran.begin();
        } catch (Exception ex) {
            throw new EJBException("UserTran Error : ", ex);
        }

        if (string != null) {
            if (string.contains("EJBException"))
                throw new EJBException("Test Exception");
        }

        return (ivString);
    }

    @Remove
    public String remove_commit(String string) {
        ivString += ":remove_commit:" + string;

        try {
            ivUserTran.commit();
        } catch (Exception ex) {
            throw new EJBException("UserTran Error : ", ex);
        }

        if (string != null) {
            if (string.contains("EJBException"))
                throw new EJBException("Test Exception");
        }

        return (ivString);
    }

    @Remove
    public String remove_rollback(String string) {
        ivString += ":remove_rollback:" + string;

        try {
            ivUserTran.rollback();
        } catch (Exception ex) {
            throw new EJBException("UserTran Error : ", ex);
        }

        if (string != null) {
            if (string.contains("EJBException"))
                throw new EJBException("Test Exception");
        }

        return (ivString);
    }

    @Remove
    public String remove_Transaction(String string) {
        ivString += ":remove_Transaction:" + string;

        try {
            ivUserTran.begin();

            if ("ROLLBACK".equals(string))
                ivUserTran.rollback();
            else
                ivUserTran.commit();
        } catch (Exception ex) {
            throw new EJBException("UserTran Error : ", ex);
        }

        if (string != null) {
            if (string.contains("EJBException"))
                throw new EJBException("Test Exception");
        }

        return (ivString);
    }

    @Remove(retainIfException = true)
    public String remove_retain(String string) throws TestAppException {
        ivString += ":remove_retain:" + string;

        try {
            if ("ROLLBACK".equals(string)) {
                ivUserTran.begin();
                ivUserTran.rollback();
            } else if ("COMMIT".equals(string)) {
                ivUserTran.begin();
                ivUserTran.commit();
            }
        } catch (Exception ex) {
            throw new EJBException("UserTran Error : ", ex);
        }

        if (string != null) {
            if (string.contains("EJBException"))
                throw new EJBException("Test Exception");
            else if (string.contains("AppException"))
                throw new TestAppException();
        }

        return (ivString);
    }

    @Remove
    public String remove_RemoveEx() throws RemoveException {
        ivString += ":remove_RemoveEx";
        return (ivString);
    }

    /** Required default constructor **/
    public RemoveCompBMTBean() {
    }

    @Override
    public void ejbRemove() {
    }

    @Override
    public void ejbActivate() {
    }

    @Override
    public void ejbPassivate() {
    }

    @Override
    public void setSessionContext(SessionContext sc) {
        ivContext = sc;
    }

    public void ejbCreate() throws CreateException {
    }

    public void ejbCreate(String string) throws CreateException {
        ivString += string;
    }
}