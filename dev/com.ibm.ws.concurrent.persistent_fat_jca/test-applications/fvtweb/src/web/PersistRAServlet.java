/*******************************************************************************
 * Copyright (c) 2012, 2019 IBM Corporation and others.
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
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.enterprise.concurrent.Trigger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.concurrent.persistent.PersistentExecutor;
import com.ibm.websphere.concurrent.persistent.TaskStatus;

public class PersistRAServlet extends HttpServlet {
    private static final long serialVersionUID = 7709282314904580334L;

    /**
     * Interval for polling task status (in milliseconds).
     */
    private static final long POLL_INTERVAL = 200;

    /**
     * Message written to servlet to indicate that is has been successfully invoked.
     */
    public static final String SUCCESS_MESSAGE = "COMPLETED SUCCESSFULLY";

    /**
     * Maximum number of nanoseconds to wait for a task to finish.
     */
    private static final long TIMEOUT_NS = TimeUnit.SECONDS.toNanos(10);

    @Resource(name = "concurrent/myScheduler")
    private PersistentExecutor scheduler;

    @Resource(name = "eis/serializableTask")
    private Callable<Object> raSerializableTask;

    @Resource(name = "eis/serializableTrigger")
    private Trigger raSerializableTrigger;

    @Resource(name = "eis/task")
    private Callable<Object> raTask;

    @Resource(name = "eis/trigger")
    private Trigger raTrigger;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String test = request.getParameter("test");
        PrintWriter out = response.getWriter();
        out.println("Starting " + test + "<br>");
        System.out.println("-----> " + test + " starting");
        try {
            getClass().getMethod(test, HttpServletRequest.class, HttpServletResponse.class).invoke(this, request, response);
            System.out.println("<----- " + test + " successful");
            out.println(test + " COMPLETED SUCCESSFULLY");
        } catch (Throwable x) {
            if (x instanceof InvocationTargetException)
                x = x.getCause();
            System.out.println("<----- " + test + " failed:");
            x.printStackTrace(System.out);
            out.println("<pre>ERROR in " + test + ":");
            x.printStackTrace(out);
            out.println("</pre>");
        }
    }

    /**
     * Verify that a task scheduled by a resource adapter upon start completes successfully.
     */
    public void testResourceAdapterSchedulesTaskWhenStarted(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        @SuppressWarnings("unchecked")
        Future<TaskStatus<?>> statusFuture = (Future<TaskStatus<?>>) Class.forName("fat.persistra.resourceadapter.PSETestResourceAdapter")
                        .getMethod("getTaskStatus").invoke(null);
        TaskStatus<?> status = statusFuture.get();

        Object result = status.get();
        if (result == null || !result.getClass().getSimpleName().equals("RAResult"))
            throw new Exception("Unexpected result: " + result);

        status = scheduler.getStatus(status.getTaskId());
        if (status != null)
            throw new Exception("Application isolation should prevent web application from seeing task scheduled by the resource adapter. Instead: " + status);
    }

    /**
     * From a servlet, schedule a serializable task that came from a resource adapter using a
     * serializable trigger that came from a resource adapter,
     * where the task returns a result that came from the resource adapter.
     */
    public void testServletSchedulesSerializableTaskFromResourceAdapter(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        TaskStatus<?> status = scheduler.schedule(raSerializableTask, raSerializableTrigger);

        for (long start = System.nanoTime(); !status.hasResult() && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
            status = scheduler.getStatus(status.getTaskId());

        Object result = status.get();
        if (result == null || !result.getClass().getSimpleName().equals("RAResult"))
            throw new Exception("Unexpected result: " + result);
    }

    /**
     * From a servlet, schedule a non-serializable task that came from a resource adapter using a
     * non-serializable trigger that came from a resource adapter,
     * where the task returns a result that came from the resource adapter.
     */
    public void testServletSchedulesTaskFromResourceAdapter(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        TaskStatus<?> status = scheduler.schedule(raTask, raTrigger);

        for (long start = System.nanoTime(); !status.hasResult() && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
            status = scheduler.getStatus(status.getTaskId());

        Object result = status.get();
        if (result == null || !result.getClass().getSimpleName().equals("RAResult"))
            throw new Exception("Unexpected result: " + result);
    }
}
