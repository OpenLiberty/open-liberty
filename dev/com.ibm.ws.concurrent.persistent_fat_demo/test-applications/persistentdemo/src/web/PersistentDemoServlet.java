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

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedTask;
import javax.enterprise.concurrent.SkippedException;
import javax.enterprise.concurrent.Trigger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import com.ibm.websphere.concurrent.persistent.PersistentExecutor;
import com.ibm.websphere.concurrent.persistent.TaskState;
import com.ibm.websphere.concurrent.persistent.TaskStatus;

@WebServlet("/*")
public class PersistentDemoServlet extends HttpServlet {
    private static final long serialVersionUID = -1747377967711217272L;

    @Resource(lookup = "jdbc/DemoDB")
    private DataSource demoDB;

    @Resource(lookup = "concurrent/myScheduler")
    private PersistentExecutor persistentExecutor;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PrintWriter out = response.getWriter();
        String url = request.getRequestURL().toString() + '?' + request.getQueryString();
        System.out.println("-----> " + url);

        try {
            out.println("<h3>Persistent Executor Demo</h3>");

            // schedule new task
            String newTaskName = request.getParameter("newTaskName");
            if (newTaskName != null) {
                TimeUnit units = TimeUnit.valueOf(request.getParameter("units"));
                String value = request.getParameter("initialDelay");
                Long initialDelay = value == null || value.length() == 0 ? 0 : units.toMillis(Long.parseLong(value));
                value = request.getParameter("interval");
                Long interval = value == null || value.length() == 0 ? -1 : units.toMillis(Long.parseLong(value));
                value = request.getParameter("numExecutions");
                Long numExecutions = value == null || value.length() == 0 ? Long.MAX_VALUE : Long.parseLong(value);

                DemoTask task = new DemoTask(newTaskName);
                if (Boolean.TRUE.toString().equals(request.getParameter("suspendTran")))
                    task.getExecutionProperties().put(ManagedTask.TRANSACTION, ManagedTask.SUSPEND);
                Trigger trigger = new DemoTrigger(initialDelay, interval, numExecutions);
                TaskStatus<String> status = persistentExecutor.schedule(task, trigger);
                out.println("Successfully scheduled task " + status.getTaskId() + "<br>");
            }

            // remove by name task and state
            String namePattern = request.getParameter("removeTaskPattern");
            if (namePattern != null) {
                String state = request.getParameter("state");
                TaskState taskState = state == null ? null : TaskState.valueOf(state);
                boolean inState = !Boolean.FALSE.toString().equals(request.getParameter("inState"));
                int numRemoved = persistentExecutor.remove(namePattern, '\\', taskState, inState);
                out.println("Successfully removed " + numRemoved + " tasks<br>");
            }

            // cancel/fail/skip/remove
            for (Entry<String, String[]> entry : request.getParameterMap().entrySet())
                for (String value : entry.getValue())
                    if (value.equals("Cancel")) {
                        long taskId = Long.parseLong(entry.getKey());
                        TaskStatus<String> taskStatus = persistentExecutor.getStatus(taskId);
                        if (taskStatus == null)
                            out.println("Unable to find task " + taskId + "<br>");
                        else {
                            if (taskStatus.cancel(true))
                                out.println("Successfully canceled task " + taskId + "<br>");
                            else
                                out.println("Unable to cancel task " + taskId + "<br>");
                        }
                    } else if (value.equals("Fail")) {
                        long taskId = Long.parseLong(entry.getKey());
                        Connection con = demoDB.getConnection();
                        try {
                            Statement stmt = con.createStatement();
                            try {
                                stmt.executeUpdate("INSERT INTO RESULTS VALUES (" + taskId + ",0,'Intentionally failed')");
                            } catch (SQLIntegrityConstraintViolationException x) {
                                stmt.executeUpdate("UPDATE RESULTS SET FAILMSG='Intentionally failed' WHERE TASKID=" + taskId);
                            }
                        } finally {
                            con.close();
                        }
                    } else if (value.equals("Skip")) {
                        long taskId = Long.parseLong(entry.getKey());
                        Connection con = demoDB.getConnection();
                        try {
                            Statement stmt = con.createStatement();
                            try {
                                stmt.executeUpdate("INSERT INTO RESULTS VALUES (" + taskId + ",1,'')");
                            } catch (SQLIntegrityConstraintViolationException x) {
                                stmt.executeUpdate("UPDATE RESULTS SET NUMSKIPS=NUMSKIPS+1 WHERE TASKID=" + taskId);
                            }
                        } finally {
                            con.close();
                        }
                    } else if (value.equals("Remove")) {
                        long taskId = Long.parseLong(entry.getKey());
                        if (persistentExecutor.remove(taskId))
                            out.println("Successfully removed task " + taskId + "<br>");
                        else
                            out.println("Unable to find/remove task " + taskId + "<br>");
                    }

            out.println("<form>");
            out.println("<input type=submit value=Schedule> task");
            out.println(" <input type=text name=newTaskName value='My Task Name' size=15> with");
            out.println(" initial delay <input type=text name=initialDelay value=0 size=3> and");
            out.println(" interval <input type=text name=interval size=3>");
            out.println("<select name=units>");
            out.println(" <option value=SECONDS>Seconds</option>");
            out.println(" <option value=MINUTES>Minutes</option>");
            out.println(" <option value=HOURS>Hours</option>");
            out.println(" <option value=DAYS>Days</option>");
            out.println("</select>");
            out.println("to run <input type=text name=numExecutions size=3> time(s).");
            out.println("<br><input type=checkbox name=suspendTran value=true>Suspend executor's transaction");
            out.println("</form><br>");

            out.println("<form>");
            out.println("<input type=submit value=Remove> tasks ");
            out.println("<select name=inState>");
            out.println(" <option value=true>in</option>");
            out.println(" <option value=false>not in</option>");
            out.println("</select>");
            out.println("<select name=state>");
            out.println(" <option value=SCHEDULED>Scheduled</option>");
            out.println(" <option value=UNATTEMPTED>Unattempted</option>");
            out.println(" <option value=SKIPPED>Skipped</option>");
            out.println(" <option value=SKIPRUN_FAILED>Failed skipRun</option>");
            out.println(" <option value=FAILURE_LIMIT_REACHED>Retry limit reached</option>");
            out.println(" <option value=SUSPENDED>Suspended</option>");
            out.println(" <option value=CANCELED>Canceled</option>");
            out.println(" <option value=ENDED>Ended</option>");
            out.println(" <option value=ANY>Any</option>");
            out.println("</select>");
            out.println(" state with names like <input type=text name=removeTaskPattern value='%' size=15>");
            out.println("</form><br>");

            out.println("<form><input type=submit value=Refresh onclick='javascript:window.location.href=\"" + request.getRequestURL().toString() + "\"'></form><br>");

            int count = 0;
            for (TaskStatus<?> status : persistentExecutor.findTaskStatus("%", '\\', TaskState.ANY, true, null, null)) {
                if (++count == 1) {
                    out.println("<form>");
                    out.println("<table border=1 cellspacing=0 cellpadding=4>");
                    out.println("<tr><th>Id</th><th>Name</th><th>State</th><th width=140>Next Execution</th><th>Most Recent Result</th><th>Cancel</th><th>Fail</th><th>Skip</th><th>Remove</th></tr>");
                }
                // TaskStatus[1]@a556a556 My Task Name SCHEDULED,UNATTEMPTED 2014/06/03-8:48:00.000-CDT[millis]
                String[] parts = status.toString().split(" ");
                String name = "";
                for (int i = 1; i < parts.length - 2; i++)
                    name += (i == 1 ? "" : " ") + parts[i];
                String state = parts[parts.length - 2];
                String nextExecTime = parts[parts.length - 1];
                nextExecTime = nextExecTime.substring(0, nextExecTime.indexOf('.'));
                String result = "";
                if (status.hasResult())
                    try {
                        result = (String) status.getResult();
                    } catch (SkippedException x) {
                        result = "[SKIPPED" + (x.getCause() == null ? "" : (": " + x.getCause())) + "]";
                    } catch (ExecutionException x) {
                        result = x.getCause().toString();
                    } catch (IllegalStateException x) {
                    } catch (Throwable x) {
                        result = x.toString();
                    }
                long taskId = status.getTaskId();
                String cancel = state.startsWith("DONE") ? "" : ("<input type=submit name=" + taskId + " value=Cancel>");
                String fail = state.startsWith("DONE") ? "" : ("<input type=submit name=" + taskId + " value=Fail>");
                String skip = state.startsWith("DONE") ? "" : ("<input type=submit name=" + taskId + " value=Skip>");
                String remove = "<input type=submit name=" + taskId + " value=Remove>";

                out.println("<tr><td>" + taskId + "</td><td>" + name + "</td><td>" + state + "</td><td>" + nextExecTime + "</td><td>"
                            + result + "</td><td>" + cancel + "</td><td>" + fail + "</td><td>" + skip + "</td><td>" + remove + "</td></tr>");
            }
            if (count > 0)
                out.println("</table></form>");

            System.out.println("<----- " + url);
            out.println("<!--COMPLETED SUCCESSFULLY-->");
        } catch (Throwable x) {
            if (x instanceof InvocationTargetException)
                x = x.getCause();
            System.out.println("<----- " + url + " failed:");
            x.printStackTrace(System.out);
            out.println("<pre>ERROR during test:");
            x.printStackTrace(out);
            out.println("</pre>");
        } finally {
            out.flush();
            out.close();
        }
    }

    /**
     * Initialize the scheduler tables and a table used by the application
     */
    @Override
    public void init() throws ServletException {
        try {
            Connection con = demoDB.getConnection();
            try {
                Statement stmt = con.createStatement();
                try {
                    stmt.executeUpdate("DELETE FROM RESULTS"); // delete any entries from previous test run
                } catch (SQLException x) {
                    stmt.executeUpdate("CREATE TABLE RESULTS (TASKID BIGINT NOT NULL PRIMARY KEY, NUMSKIPS INT NOT NULL, FAILMSG VARCHAR(80))");
                }
            } finally {
                con.close();
            }
        } catch (SQLException x) {
            throw new ServletException(x);
        }
    }
}
