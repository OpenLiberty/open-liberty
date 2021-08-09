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
package web;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;

import javax.enterprise.concurrent.ManagedTask;
import javax.enterprise.concurrent.ManagedTaskListener;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import com.ibm.websphere.concurrent.persistent.AutoPurge;
import com.ibm.websphere.concurrent.persistent.TaskIdAccessor;

public class DemoTask implements Callable<String>, ManagedTask, Serializable {
    private static final long serialVersionUID = -6614795300373340299L;

    private final Map<String, String> execProps = new TreeMap<String, String>();
    long numExecutions;

    public DemoTask(String name) {
        if (name != null)
            execProps.put(IDENTITY_NAME, name);
        execProps.put(AutoPurge.PROPERTY_NAME, AutoPurge.NEVER.toString());
    }

    @Override
    public String call() throws DemoTaskException, HeuristicMixedException, HeuristicRollbackException, IllegalStateException, NamingException, NotSupportedException, RollbackException, SecurityException, SQLException, SystemException {
        numExecutions++;
        long taskId = TaskIdAccessor.get();
        System.out.println(new Date() + ": Task " + taskId + " attempting execution " + numExecutions);

        UserTransaction newTran = ManagedTask.SUSPEND.equals(execProps.get(ManagedTask.TRANSACTION)) ? (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction") : null;
        if (newTran != null)
            newTran.begin();
        try {
            DataSource dataSource = (DataSource) new InitialContext().lookup(
                                                                             "java:comp/env/web.PersistentDemoServlet/demoDB");
            Connection con = dataSource.getConnection();
            try {
                Statement stmt = con.createStatement();
                ResultSet results = stmt.executeQuery("SELECT FAILMSG FROM RESULTS WHERE TASKID=" + taskId);
                if (results.next()) {
                    String failureMessage = results.getString(1);
                    if (failureMessage != null && failureMessage.length() > 0)
                        throw new DemoTaskException(failureMessage);
                }
            } finally {
                con.close();
            }
        } finally {
            if (newTran != null)
                newTran.commit();
        }
        return "RESULT-" + numExecutions;
    }

    @Override
    public Map<String, String> getExecutionProperties() {
        return execProps;
    }

    @Override
    public ManagedTaskListener getManagedTaskListener() {
        return null;
    }
}
