/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.managedbeans.fat.mb.ejb;

import java.sql.Connection;
import java.util.logging.Logger;

import javax.annotation.ManagedBean;
import javax.annotation.Resource;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;

/**
 * Simple Managed Bean that has been named and has simple injection.
 **/
@ManagedBean("InjectionManagedBean")
public class InjectionManagedBean {
    private static final String CLASS_NAME = InjectionManagedBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);
    private static int svNextID = 1;

    private final int ivID;
    private String ivValue = null;

    @Resource
    UserTransaction ivUserTran;

    @Resource(name = "jdbc/TestDS")
    DataSource testDS;

    @Resource(name = "TestDSfromEJB")
    DataSource testDSfromEJB;

    public InjectionManagedBean() {
        // Use a unique id so it is easy to tell which instance is in use.
        synchronized (this) {
            svLogger.info("-- ejb.InjectionManagedBean.<init>:" + svNextID);
            ivID = svNextID++;
        }
    }

    /**
     * Returns the unique identifier of this instance.
     */
    public int getIdentifier() {
        svLogger.info("-- getIdentifier : " + this);
        return ivID;
    }

    /**
     * Returns the value.. to verify object is 'stateful'
     */
    public String getValue() {
        svLogger.info("-- getValue : " + this);
        return ivValue;
    }

    /**
     * Sets the value.. to verify object is 'stateful'
     */
    public void setValue(String value) {
        svLogger.info("-- setValue : " + ivValue + "->" + value + " : " + this);
        ivValue = value;
    }

    /**
     * Returns the injected UserTransaction.
     */
    public UserTransaction getUserTransaction() {
        return ivUserTran;
    }

    /**
     * Returns the injected DataSource.
     */
    public DataSource getDataSource() throws Exception {
        Connection conn = testDS.getConnection();
        conn.close();

        return testDS;
    }

    /**
     * Returns the injected DataSource defined by the EJB.
     */
    public DataSource getEJBdefinedDataSource() throws Exception {
        Connection conn = testDSfromEJB.getConnection();
        conn.close();

        return testDSfromEJB;
    }

    @Override
    public String toString() {
        return "ejb.InjectionManagedBean(ID=" + ivID + "," + ivValue + ")";
    }

}
