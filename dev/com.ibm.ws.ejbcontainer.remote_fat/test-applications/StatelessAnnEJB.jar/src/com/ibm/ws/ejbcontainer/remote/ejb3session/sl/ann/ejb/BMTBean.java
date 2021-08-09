/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.ejb;

import static javax.ejb.TransactionManagementType.BEAN;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJBException;
import javax.ejb.Remote;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.transaction.UserTransaction;

/**
 * Bean implementation class for Enterprise Bean: BMTBean
 **/
@Stateless(name = "BMTBean")
// d405023
@Remote(BMTRemote.class)
@TransactionManagement(BEAN)
public class BMTBean {
    @Resource
    private SessionContext ivContext;
    private UserTransaction ivUserTran;

    @PostConstruct
    private void initUserTransaction() {
        ivUserTran = ivContext.getUserTransaction();
    }

    /** Begins a global transaction. This is used to verify we are in a BMT **/
    public void bmtMethod() {
        try {
            ivUserTran.begin();
        } catch (Exception ex) {
            throw new EJBException("UserTran Error executing ivUserTran.begin(): ", ex);
        }

        try {
            ivUserTran.commit();
        } catch (Exception ex) {
            throw new EJBException("UserTran Error executing ivUserTran.commit(): ", ex);
        }

        return;
    }

    /** Required default constructor **/
    public BMTBean() {}
}