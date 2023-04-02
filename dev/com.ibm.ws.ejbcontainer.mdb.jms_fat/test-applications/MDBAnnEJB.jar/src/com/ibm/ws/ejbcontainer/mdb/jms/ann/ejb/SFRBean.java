/*******************************************************************************
 * Copyright (c) 2003, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.mdb.jms.ann.ejb;

import static javax.ejb.TransactionAttributeType.NOT_SUPPORTED;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.annotation.Resource.AuthenticationType;
import javax.ejb.EJBException;
import javax.ejb.Remote;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.sql.DataSource;

@Stateful(name = "SFRBean")
@Remote(SF.class)
public class SFRBean {
    private final static String CLASSNAME = SFRBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    @SuppressWarnings("unused")
    private static final long serialVersionUID = 6126860989973139868L;
    final static String BeanName = "SFRBean";

    public int intValue;

    // 454065
    @Resource(name = "jdbc/MDBDS", type = DataSource.class, description = "test data source",
              shareable = true, authenticationType = AuthenticationType.CONTAINER)
    private DataSource ds;

    /**
     * Returns the intValue.
     *
     * @return int
     */
    public int getIntValue() {
        printMsg(BeanName, "----->getIntValue = " + intValue);
        return intValue;
    }

    /**
     * Sets the intValue.
     *
     * @param intValue The intValue to set
     */
    public void setIntValue(int intValue) {
        printMsg(BeanName, "----->setIntValue = " + intValue);
        this.intValue = intValue;
    }

    /**
     * Insert the method's description here.
     */
    public void printMsg(String beanName, String msg) {
        svLogger.info("       " + beanName + " : " + msg);
    }

    /**
     * method1
     */
    @TransactionAttribute(NOT_SUPPORTED)
    public String method1(String arg1) {
        printMsg(BeanName, "-----> method1 arg = " + arg1);
        return arg1;
    }

    // d454065, test datasource injection - ds should not be null
    public DataSource getDataSource() {
        printMsg(BeanName, "-----> getDataSource ds = " + ds);
        return ds;
    }

    // d454065, test datasource injection - make sure ds is injected properly
    public String getStringValue() {
        String primaryKey = "pkey_remote";
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        String stringValue = null;

        try {
            con = ds.getConnection();
            ps = con.prepareStatement("select pkey, intValue, stringvalue from EJBFAT.BMP where pkey = ?");
            ps.setString(1, primaryKey);
            result = ps.executeQuery();
            if (result.next()) {
                intValue = result.getInt("intValue");
                stringValue = result.getString("stringValue");
                svLogger.info("stringValue from mdba.SFRBean: " + stringValue);
                return stringValue;
            } else {
                svLogger.info("nothing found in DB with primary key == pkey_remote");
                throw new EJBException();
            }
        } catch (SQLException se) {
            svLogger.info("SQLException in SFRBean.getStringValue:");
            throw new EJBException(se);
        } finally {
            try {
                if (result != null)
                    result.close();
                if (ps != null)
                    ps.close();
                if (con != null)
                    con.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }
}