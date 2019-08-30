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
package com.ibm.test.servlet;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;

import javax.enterprise.concurrent.ManagedTask;
import javax.enterprise.concurrent.ManagedTaskListener;
import javax.naming.InitialContext;
import javax.sql.DataSource;

/**
 * Task that increments an entry in a database.
 * The task creates the entry if not already present.
 */
public class DBIncrementTask implements Callable<Integer>, ManagedTask, Runnable, Serializable {

    private static final long serialVersionUID = -6813765273368292860L;

    final Map<String, String> execProps = new TreeMap<String, String>();
    private final String key;
    private int totalUpdates;

    public DBIncrementTask(String key) {
        this.execProps.put(ManagedTask.IDENTITY_NAME, getClass().getSimpleName() + '-' + key);
        this.key = key;
    }

    @Override
    public Integer call() throws Exception {
        DataSource ds = (DataSource) new InitialContext().lookup("java:module/env/jdbc/testDBRef");
        Connection con = ds.getConnection();
        try {
            PreparedStatement pstmt = con.prepareStatement("UPDATE MYTABLE SET MYVALUE=MYVALUE+1 WHERE MYKEY=?");
            pstmt.setString(1, key);
            int updateCount = pstmt.executeUpdate();
            pstmt.close();
            if (updateCount == 0) {
                pstmt = con.prepareStatement("INSERT INTO MYTABLE VALUES(?,1)");
                pstmt.setString(1, key);
                updateCount = pstmt.executeUpdate();
            }
            totalUpdates += updateCount;
            return totalUpdates;
        } finally {
            con.close();
        }
    }

    @Override
    public Map<String, String> getExecutionProperties() {
        return execProps;
    }

    @Override
    public ManagedTaskListener getManagedTaskListener() {
        return null;
    }

    @Override
    public void run() {
        try {
            call();
        } catch (RuntimeException x) {
            throw x;
        } catch (Exception x) {
            throw new RuntimeException(x);
        }
    }

    @Override
    public String toString() {
        return super.toString() + '[' + key + ']';
    }
}