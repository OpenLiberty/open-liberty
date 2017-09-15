/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.Callable;

import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.sql.DataSource;

/**
 * Task that finds the base shipping charge for a state.
 */
public class BaseShippingChargeFinder implements Callable<Float> {
    private final String stateName;

    BaseShippingChargeFinder(String stateName) {
        this.stateName = stateName;
    }

    @Override
    public Float call() throws Exception {
        DataSource dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/testdbRef");
        Connection con = dataSource.getConnection();
        try {
            PreparedStatement pstmt = con.prepareStatement("select baseAmount from BaseShippingAmounts where stateName=?");
            pstmt.setString(1, stateName);
            ResultSet result = pstmt.executeQuery();
            if (result.next())
                return result.getFloat(1);
            else
                throw new ServletException("We do not currently provide shipping to " + stateName);
        } finally {
            con.close();
        }
    }
}
