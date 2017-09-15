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
import java.util.Date;

import javax.enterprise.concurrent.LastExecution;
import javax.enterprise.concurrent.Trigger;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import com.ibm.websphere.concurrent.persistent.TaskIdAccessor;

public class DemoTrigger implements Serializable, Trigger {
    private static final long serialVersionUID = -7714839307025025692L;

    private final long initialDelay, interval, maxExecutions;
    private long numExecutions;
    private transient boolean skipped;

    public DemoTrigger(long initialDelay, long interval, long maxExecutions) {
        this.initialDelay = initialDelay;
        this.interval = interval;
        this.maxExecutions = maxExecutions;
    }

    @Override
    public Date getNextRunTime(LastExecution lastExecution, Date taskScheduledTime) {
        if (numExecutions >= maxExecutions)
            return null;
        else if (numExecutions == 0)
            return new Date(taskScheduledTime.getTime() + initialDelay);
        else if (interval < 0)
            return null;
        else if (skipped)
            return new Date(new Date().getTime() + interval);
        else
            return new Date(lastExecution.getRunStart().getTime() + interval);
    }

    @Override
    public boolean skipRun(LastExecution lastExecution, Date scheduledRunTime) {
        try {
            DataSource dataSource = (DataSource) new InitialContext().lookup(
                                                                             "java:comp/env/web.PersistentDemoServlet/demoDB");
            Connection con = dataSource.getConnection();
            try {
                Long taskId = TaskIdAccessor.get();
                skipped = con.createStatement().executeUpdate("UPDATE RESULTS SET NUMSKIPS=NUMSKIPS-1 WHERE TASKID=" + taskId + " AND NUMSKIPS>0") > 0;
                if (skipped) {
                    System.out.println(new Date() + ": Task " + taskId + " execution skipped");
                    return true;
                }
            } finally {
                con.close();
            }
        } catch (RuntimeException x) {
            throw x;
        } catch (Exception x) {
            throw new RuntimeException(x);
        }
        ++numExecutions;
        return false;
    }
}
