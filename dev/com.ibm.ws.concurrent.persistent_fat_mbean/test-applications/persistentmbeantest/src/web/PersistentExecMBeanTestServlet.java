/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.concurrent.persistent.PersistentExecutor;
import com.ibm.websphere.concurrent.persistent.TaskState;
import com.ibm.websphere.concurrent.persistent.TaskStatus;
import com.ibm.websphere.concurrent.persistent.mbean.PersistentExecutorMBean;

@WebServlet("/*")
public class PersistentExecMBeanTestServlet extends HttpServlet {
    private static final long serialVersionUID = 8447513765214641067L;

    /**
     * Message written to servlet to indicate that is has been successfully invoked.
     */
    private static final String SUCCESS_MESSAGE = "COMPLETED SUCCESSFULLY";

    @Resource(name = "java:module/env/myExecutorRef", lookup = "concurrent/myExecutor")
    private PersistentExecutor executor;

    @Resource(name = "java:module/env/secondExecutorRef", lookup = "concurrent/secondExecutor")
    private PersistentExecutor executor2;

    private final String jndi = "concurrent/myExecutor";
    private final String jndi2 = "concurrent/secondExecutor";
    private final String id = "Exec";

    MBeanServer mbs;
    ObjectInstance bean;
    ObjectInstance bean2;

    String[] signaturesIdOrJndi = { "java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String" };
    String[] signaturesFindScheduled = { "long", "java.lang.String", "boolean", "java.lang.Long", "java.lang.Integer" };
    String[] signaturesTransfer = { "java.lang.Long", "long" };

    /**
     * Invokes test name found in "test" parameter passed to servlet.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String test = request.getParameter("test");
        PrintWriter out = response.getWriter();

        try {
            out.println(getClass().getSimpleName() + " is starting " + test + "<br>");
            System.out.println("-----> " + test + " starting");
            getClass().getMethod(test, HttpServletRequest.class, PrintWriter.class).invoke(this, request, out);
            System.out.println("<----- " + test + " successful");
            out.println(test + " " + SUCCESS_MESSAGE);
        } catch (Throwable x) {
            if (x instanceof InvocationTargetException)
                x = x.getCause();
            System.out.println("<----- " + test + " failed:");
            x.printStackTrace(System.out);
            out.println("<pre>ERROR in " + test + ":");
            x.printStackTrace(out);
            out.println("</pre>");
        } finally {
            out.flush();
            out.close();
        }
    }

    /**
     * Initialize MBeanServer and the 2 MBeans
     */
    @Override
    public void init() throws ServletException {

        mbs = ManagementFactory.getPlatformMBeanServer();
        try {
            bean = getPEMBean(mbs, jndi);
            bean2 = getPEMBean(mbs, jndi2);
        } catch (Exception e) {
            throw new ServletException(e);
        }

    }

    /**
     * Cancel any running tasks
     */
    @Override
    public void destroy() {
        executor.cancel(null, null, TaskState.ANY, true);
        executor2.cancel(null, null, TaskState.ANY, true);
    }

    /**
     * Query the mbean server to ensure an MBean with jndiName=concurrent/myExecutor and id=Exec
     * was created with the Persistent Executor
     */
    public void testMBeanCreation(HttpServletRequest request, PrintWriter out) throws Throwable {
        getPEMBean(mbs, jndi);
        getPEMBeanById(mbs, id);
    }

    /**
     * Tests the mbean's findPartitionInfo using the jndi name of the Persistent Executor.
     */
    public void testfindPartitionInfo(HttpServletRequest request, PrintWriter out) throws Throwable {
        Callable<Integer> task = new MBeanCounterCallable();
        executor.schedule(task, 2000, TimeUnit.SECONDS);

        String userDir = request.getParameter("userdir").concat("/");
        String libertyServerName = "com.ibm.ws.concurrent.persistent.fat.mbean";

        ObjectName name = new ObjectName("WebSphere:feature=persistentExecutor,type=PersistentExecutorMBean,name=persistentExecutor[Exec],jndiName=concurrent/myExecutor,id=Exec");
        PersistentExecutorMBean proxy = JMX.newMXBeanProxy(mbs, name, PersistentExecutorMBean.class);
        String[][] data = proxy.findPartitionInfo(null, null, null, id);
        StringBuilder sb = new StringBuilder(" ");
        String[] info = data[0];

        if (!(info[2].toString().equals(userDir) && info[3].toString().equals(libertyServerName) && info[4].toString().equals(id)))
            throw new Exception(sb.append("Partition info did not match: ")
                            .append(info[2].toString()).append("=").append(userDir).append(" ")
                            .append(info[3].toString()).append("=").append(libertyServerName).append(" ")
                            .append(info[4].toString()).append("=").append(jndi).append(" ")
                            .toString());

    }

    /**
     * Verifies that the findTaskIds mbean method correctly finds the taskId
     */
    public void testFindTaskIds(HttpServletRequest request, PrintWriter out) throws Throwable {

        Callable<Integer> task = new MBeanCounterCallable();
        TaskStatus<Integer> ts = executor.schedule(task, 2000, TimeUnit.SECONDS);
        long taskId = ts.getTaskId();

        long partitionId = 1;
        String state = "SCHEDULED";
        boolean inState = true;
        long minId = 0;
        int maxResults = 100;
        Object[] params = { partitionId, state, inState, minId, maxResults };
        Object obj2 = mbs.invoke(bean.getObjectName(), "findTaskIds", params, signaturesFindScheduled);

        Long[] data2 = (Long[]) obj2;

        boolean taskIdFound = false;
        for (Long l : data2)
            if (l.equals(taskId))
                taskIdFound = true;
        if (!taskIdFound)
            throw new Exception("TaskId was not found");
    }

    /**
     * Verifies that the transfer mbean method correctly transfers tasks
     */
    public void testTransfer(HttpServletRequest request, PrintWriter out) throws Throwable {
        Callable<Integer> task = new MBeanCounterCallable();
        executor.schedule(task, 2000, TimeUnit.SECONDS);

        Callable<Integer> task2 = new MBeanCounterCallable();
        executor2.schedule(task2, 2000, TimeUnit.SECONDS);

        long maxTaskId = 1000;
        long partitionId = 1;

        ObjectName name = new ObjectName("WebSphere:feature=persistentExecutor,type=PersistentExecutorMBean,name=persistentExecutor[default-0],jndiName=concurrent/secondExecutor");
        PersistentExecutorMBean proxy = JMX.newMXBeanProxy(mbs, name, PersistentExecutorMBean.class);
        int tasksTransfered = proxy.transfer(maxTaskId, partitionId);
        if (tasksTransfered < 1)
            throw new Exception("No tasks were transfered");
    }

    /**
     * Test to verify the MBean method remove partition info correctly removes partitions
     */
    public void testRemovePartitionInfo(HttpServletRequest request, PrintWriter out) throws Throwable {
        Callable<Integer> task = new MBeanCounterCallable();
        executor.schedule(task, 2000, TimeUnit.SECONDS);

        Object[] params = { null, null, null, id };
        Object obj = mbs.invoke(bean.getObjectName(), "findPartitionInfo", params, signaturesIdOrJndi);
        String[][] data = (String[][]) obj;
        if (data.length == 0)
            throw new Exception("Partiton Info should exist");
        Object obj2 = mbs.invoke(bean.getObjectName(), "removePartitionInfo", params, signaturesIdOrJndi);
        if ((Integer) obj2 != 1)
            throw new Exception("One entry should have been removed");
        Object obj3 = mbs.invoke(bean.getObjectName(), "findPartitionInfo", params, signaturesIdOrJndi);
        String[][] data3 = (String[][]) obj3;
        if (data3.length != 0)
            throw new Exception("Partiton Info should have been removed");

    }

    /**
     * Verify that an MBean with the given jndiName exists.
     */
    public void findMBeanByJndiName(HttpServletRequest request, PrintWriter out) throws Throwable {
        getPEMBean(mbs, request.getParameter("jndi"));
    }

    /**
     * Verify that an MBean with the given jndiName does not exist.
     */
    public void missMBeanByJndiName(HttpServletRequest request, PrintWriter out) throws Throwable {
        try {
            getPEMBean(mbs, request.getParameter("jndi"));
        } catch (Exception e) {
            return;
        }
        throw new Exception("PersistentExecutorMBean with jndiName=" + request.getParameter("jndi") + " was found.");
    }

    /**
     * Verify that an ObjectName is properly constructed with or without both Id and JNDI
     */
    public void testObjectName(HttpServletRequest request, PrintWriter out) throws Throwable {
        //Both
        if (!getPEMBean(mbs, jndi).getObjectName().toString().equals("WebSphere:feature=persistentExecutor,type=PersistentExecutorMBean,name=persistentExecutor[Exec],jndiName=concurrent/myExecutor,id=Exec"))
            throw new Exception("MBean objectname was not correct. Expected:"
                                + "WebSphere:feature=persistentExecutor,type=PersistentExecutorMBean,name=persistentExecutor[Exec],jndiName=concurrent/myExecutor,id=Exec"
                                + " Actual:" + getPEMBean(mbs, jndi).getObjectName());
        //Just jndi
        if (!getPEMBean(mbs, jndi2).getObjectName().toString().equals("WebSphere:feature=persistentExecutor,type=PersistentExecutorMBean,name=persistentExecutor[default-0],jndiName=concurrent/secondExecutor"))
            throw new Exception("MBean objectname was not correct. Expected:"
                                + "WebSphere:feature=persistentExecutor,type=PersistentExecutorMBean,name=persistentExecutor[default-0],jndiName=concurrent/secondExecutor"
                                + " Actual:" + getPEMBean(mbs, jndi2).getObjectName());
        //Just id
        if (!getPEMBeanById(mbs, "myExec").getObjectName().toString().equals("WebSphere:feature=persistentExecutor,type=PersistentExecutorMBean,name=persistentExecutor[myExec],id=myExec"))
            throw new Exception("MBean objectname was not correct. Expected:"
                                + "WebSphere:feature=persistentExecutor,type=PersistentExecutorMBean,name=persistentExecutor[myExec],id=myExec"
                                + " Actual:" + getPEMBeanById(mbs, "myExec").getObjectName());
        //Neither
        if (!getPEMBeanByName(mbs, "ejbContainer/timerService[default-0]/persistentExecutor[default-0]").getObjectName().toString().equals("WebSphere:feature=persistentExecutor,type=PersistentExecutorMBean,name=ejbContainer/timerService[default-0]/persistentExecutor[default-0]"))
            throw new Exception("MBean objectname was not correct. Expected:"
                                + "WebSphere:feature=persistentExecutor,type=PersistentExecutorMBean,name=ejbContainer/timerService[default-0]/persistentExecutor[default-0]"
                                + " Actual:" + getPEMBeanByName(mbs, "ejbContainer/timerService[default-0]/persistentExecutor[default-0]").getObjectName());

    }

    /**
     * Obtain the Persistent Executor MBean with specified jndiName
     */
    private ObjectInstance getPEMBean(MBeanServer mbs, String jndiName) throws Exception {
        ObjectName obn = new ObjectName("WebSphere:type=PersistentExecutorMBean,jndiName=" + jndiName + ",*");
        Set<ObjectInstance> s = mbs.queryMBeans(obn, null);
        if (s.size() != 1) {
            System.out.println("ERROR: Found incorrect number of MBeans (" + s.size() + ")");
            for (ObjectInstance i : s)
                System.out.println("  Found MBean: " + i.getObjectName());
            throw new Exception("Expected to find exactly 1 MBean, instead found " + s.size());
        }
        return s.iterator().next();
    }

    /**
     * Obtain the Persistent Executor MBean with specified id
     */
    private ObjectInstance getPEMBeanById(MBeanServer mbs, String id) throws Exception {
        ObjectName obn = new ObjectName("WebSphere:type=PersistentExecutorMBean,id=" + id + ",*");
        Set<ObjectInstance> s = mbs.queryMBeans(obn, null);
        if (s.size() != 1) {
            System.out.println("ERROR: Found incorrect number of MBeans (" + s.size() + ")");
            for (ObjectInstance i : s)
                System.out.println("  Found MBean: " + i.getObjectName());
            throw new Exception("Expected to find exactly 1 MBean, instead found " + s.size());
        }
        return s.iterator().next();
    }

    /**
     * Obtain the Persistent Executor MBean with specified name
     */
    private ObjectInstance getPEMBeanByName(MBeanServer mbs, String name) throws Exception {
        ObjectName obn = new ObjectName("WebSphere:type=PersistentExecutorMBean,name=" + name + ",*");
        Set<ObjectInstance> s = mbs.queryMBeans(obn, null);
        if (s.size() != 1) {
            System.out.println("ERROR: Found incorrect number of MBeans (" + s.size() + ")");
            for (ObjectInstance i : s)
                System.out.println("  Found MBean: " + i.getObjectName());
            throw new Exception("Expected to find exactly 1 MBean, instead found " + s.size());
        }
        return s.iterator().next();
    }

}
