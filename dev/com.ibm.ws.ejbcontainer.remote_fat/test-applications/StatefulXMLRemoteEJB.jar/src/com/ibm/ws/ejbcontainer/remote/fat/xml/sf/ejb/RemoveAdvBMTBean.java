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
package com.ibm.ws.ejbcontainer.remote.fat.xml.sf.ejb;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.RemoveException;
import javax.ejb.SessionContext;
import javax.transaction.UserTransaction;

/**
 * Bean implementation class for Enterprise Bean: RemoveBMTLocal/RemoteBMTRemote
 **/
public class RemoveAdvBMTBean {

    private SessionContext ivContext;

    private UserTransaction ivUserTran;

    private String ivString = "RemoveAdvBMTBean";

    @SuppressWarnings("unused")
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

    public String remove(int x, int y) {
        int sum = x + y;
        ivString += ":remove:" + sum;
        return (ivString);
    }

    /** Remove method with no transaction context change. **/

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

    public String remove_RemoveEx() throws RemoveException {
        ivString += ":remove_RemoveEx";
        return (ivString);
    }

    public String removeUnique(String string) {
        ivString += ":removeUnique:" + string;

        if (string != null) {
            if (string.contains("EJBException"))
                throw new EJBException("Test Exception");
        }

        return (ivString);
    }

    /** Required default constructor **/
    public RemoveAdvBMTBean() {
    }

    public void ejbCreate() throws CreateException {
    }

    public void ejbCreate(String string) throws CreateException {
        ivString += string;
    }
}
