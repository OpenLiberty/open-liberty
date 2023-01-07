/*******************************************************************************
 * Copyright (c) 2011,2022 IBM Corporation and others.
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
package web;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.UserTransaction;

import javax.naming.InitialContext;

import componenttest.app.FATServlet;
import web.mdb.BVTMessageDrivenBean;

public class JCABVTServlet extends FATServlet {
    private static final long serialVersionUID = 7709282314904580334L;

    // Using this to try to speed up the bucket
    private static final ExecutorService daemon = Executors.newSingleThreadExecutor();

    // Interval (in milliseconds) up to which tests should wait for a single task to run
    private static final long TIMEOUT = 5000;

    @Resource
    private UserTransaction tran;

    /**
     * Verify that an activationSpec can be found in the service registry.
     */
    public void testActivationSpec(HttpServletRequest req, HttpServletResponse res) throws Throwable {
        @SuppressWarnings("unchecked")
        Queue<String> queue = (Queue<String>) new InitialContext().lookup("java:comp/env/eis/queue2");
        queue.add("item1");
        String message = BVTMessageDrivenBean.messages.poll();
        if (!"onMessage: [item1]".equals(message))
            throw new Exception("Incorrect or missing onMessage for item1: " + message + "; " + BVTMessageDrivenBean.messages);
        queue.add("item2");
        message = BVTMessageDrivenBean.messages.poll();
        if (!"onMessage: [item2]".equals(message))
            throw new Exception("Incorrect or missing onMessage for item2: " + message + "; " + BVTMessageDrivenBean.messages);
    }

    /**
     * Look up administered objects.
     */
    public void testAdminObjects(HttpServletRequest req, HttpServletResponse res) throws Throwable {

        @SuppressWarnings("unchecked")
        Queue<String> queue = (Queue<String>) new InitialContext().lookup("java:comp/env/eis/queue1");

        queue.clear();
        queue.add("something");

        byte[] bytes;
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        ObjectOutputStream objectOut = new ObjectOutputStream(byteOut);
        try {
            objectOut.writeObject(queue);
            bytes = byteOut.toByteArray();
        } finally {
            objectOut.close();
        }

        ObjectInputStream in = new SerialObjectInputStream(new ByteArrayInputStream(bytes), queue.getClass().getClassLoader());
        try {
            @SuppressWarnings("unchecked")
            Queue<String> deserQueue = (Queue<String>) in.readObject();
            queue = deserQueue;
        } finally {
            in.close();
        }

        String value = queue.peek();
        if (!"something".equals(value))
            throw new Exception("Incorrect value in queue after deserializing it: " + value + ". Queue is: " + queue);

        @SuppressWarnings("unchecked")
        Queue<String> q1 = (Queue<String>) new InitialContext().lookup("java:comp/env/eis/queue1");
        String result = q1.remove();
        if (!"something".equals(result))
            throw new Exception("Incorrect value in queue: " + result);
    }

    /**
     * Lookup the JCA objects like AOs and CFs from jndi using direct lookup and
     * do a smoke test on them by invoking basic operations.
     */
    public void testDirectLookups(HttpServletRequest req, HttpServletResponse res) throws Throwable {
        Object cf = new InitialContext().lookup("eis/cf2");
        Object cf1 = new InitialContext().lookup("eis/cf1");
        Object queue1 = new InitialContext().lookup("eis/queue1");
        Object queue2 = new InitialContext().lookup("eis/queue2");
        Object recordFactory = cf.getClass().getMethod("getRecordFactory").invoke(cf);
        @SuppressWarnings("unchecked")
        List<Object> record = (List<Object>) recordFactory.getClass().getMethod("createIndexedRecord", String.class).invoke(recordFactory, "myRecord");

        Object con = cf.getClass().getMethod("getConnection").invoke(cf);
        try {
            Object connectionMetaData = con.getClass().getMethod("getMetaData").invoke(con);
            String userName = (String) connectionMetaData.getClass().getMethod("getUserName").invoke(connectionMetaData);
            if (!"RAR2USER".equals(userName))
                throw new Exception("Incorrect user for eis/cf2: " + userName);

            Object interaction = con.getClass().getMethod("createInteraction").invoke(con);

            Class<?> InteractionSpec = con.getClass().getClassLoader().loadClass("jakarta.resource.cci.InteractionSpec");
            Class<?> Record = con.getClass().getClassLoader().loadClass("jakarta.resource.cci.Record");
            Method Interaction_execute = interaction.getClass().getMethod("execute", InteractionSpec, Record);

            // Clear the table contents of any previous usage
            record.add(0, "delete from cf2tbl");
            Interaction_execute.invoke(interaction, null, record);

            // Insert some data
            record.clear();
            record.add(0, "insert into cf2tbl values (?, ?)");
            record.add(1, 10);
            record.add(2, "ten");
            Interaction_execute.invoke(interaction, null, record);

            // Insert data in a local transaction, and commit it
            Object localTran = con.getClass().getMethod("getLocalTransaction").invoke(con);
            localTran.getClass().getMethod("begin").invoke(localTran);
            try {
                record.set(1, 20);
                record.set(2, "twenty");
                Interaction_execute.invoke(interaction, null, record);
            } finally {
                localTran.getClass().getMethod("commit").invoke(localTran);
            }
            @SuppressWarnings("unchecked")
            Queue<String> queue = (Queue<String>) new InitialContext().lookup("eis/queue1");

            queue.clear();
            queue.add("something");
            @SuppressWarnings("unchecked")
            Queue<String> q1 = (Queue<String>) new InitialContext().lookup("eis/queue1");
            String result = q1.remove();
            if (!"something".equals(result))
                throw new Exception("Incorrect value in queue: " + result);

        } finally {
            try {
                con.getClass().getMethod("close").invoke(con);
            } catch (InvocationTargetException x) {
                if (!"jakarta.resource.spi.IllegalStateException".equals(x.getCause().getClass().getName()))
                    throw x.getCause();
            }
        }

    }

    /**
     * Look up a connection factory and use it to perform interactions.
     */
    public void testConnectionFactory(HttpServletRequest req, HttpServletResponse res) throws Throwable {

        Object cf = new InitialContext().lookup("java:comp/env/eis/cf2");
        Object recordFactory = cf.getClass().getMethod("getRecordFactory").invoke(cf);
        @SuppressWarnings("unchecked")
        List<Object> record = (List<Object>) recordFactory.getClass().getMethod("createIndexedRecord", String.class).invoke(recordFactory, "myRecord");

        Object con = cf.getClass().getMethod("getConnection").invoke(cf);
        try {
            Object connectionMetaData = con.getClass().getMethod("getMetaData").invoke(con);
            String userName = (String) connectionMetaData.getClass().getMethod("getUserName").invoke(connectionMetaData);
            if (!"RAR2USER".equals(userName))
                throw new Exception("Incorrect user for eis/cf2: " + userName);

            Object interaction = con.getClass().getMethod("createInteraction").invoke(con);

            Class<?> InteractionSpec = con.getClass().getClassLoader().loadClass("jakarta.resource.cci.InteractionSpec");
            Class<?> Record = con.getClass().getClassLoader().loadClass("jakarta.resource.cci.Record");
            Method Interaction_execute = interaction.getClass().getMethod("execute", InteractionSpec, Record);

            // Clear the table contents of any previous usage
            record.add(0, "delete from cf2tbl");
            Interaction_execute.invoke(interaction, null, record);

            // Insert some data
            record.clear();
            record.add(0, "insert into cf2tbl values (?, ?)");
            record.add(1, 10);
            record.add(2, "ten");
            Interaction_execute.invoke(interaction, null, record);

            // Insert data in a local transaction, and commit it
            Object localTran = con.getClass().getMethod("getLocalTransaction").invoke(con);
            localTran.getClass().getMethod("begin").invoke(localTran);
            try {
                record.set(1, 20);
                record.set(2, "twenty");
                Interaction_execute.invoke(interaction, null, record);
            } finally {
                localTran.getClass().getMethod("commit").invoke(localTran);
            }

            // Insert data in a local transaction, and roll it back
            localTran.getClass().getMethod("begin").invoke(localTran);
            try {
                record.set(1, 30);
                record.set(2, "thirty");
                Interaction_execute.invoke(interaction, null, record);
            } finally {
                localTran.getClass().getMethod("rollback").invoke(localTran);
            }

            // Insert data in a global transaction, and commit it.
            tran.begin();
            try {
                record.set(1, 40);
                record.set(2, "forty");
                Interaction_execute.invoke(interaction, null, record);
            } finally {
                tran.commit();
            }

            // Insert data in a global transaction, and roll it back.
            tran.begin();
            try {
                record.set(1, 50);
                record.set(2, "fifty");
                Interaction_execute.invoke(interaction, null, record);
            } finally {
                tran.rollback();
            }

            // Query for the data
            record.clear();
            record.add(0, "select col1, col2 from cf2tbl where col1 < ? order by col1");
            record.add(1, 100);
            ResultSet result = (ResultSet) Interaction_execute.invoke(interaction, null, record);

            // Scroll the result set
            if (!result.next())
                throw new Exception("Missing first entry (10)");
            int col1 = result.getInt(1);
            String col2 = result.getString(2);
            if (col1 != 10 || !"ten".equals(col2))
                throw new Exception("Expecting (10,ten) not (" + col1 + ',' + col2 + ')');

            if (!result.next())
                throw new Exception("Missing second entry (20)");
            col1 = result.getInt(1);
            col2 = result.getString(2);
            if (col1 != 20 || !"twenty".equals(col2))
                throw new Exception("Expecting (20,twenty) not (" + col1 + ',' + col2 + ')');

            // Entry with 30 should be rolled back.

            if (!result.next())
                throw new Exception("Missing third entry (40)");
            col1 = result.getInt(1);
            col2 = result.getString(2);
            if (col1 != 40 || !"forty".equals(col2))
                throw new Exception("Expecting (40,forty) not (" + col1 + ',' + col2 + ')');

            // Entry with 50 should be rolled back
            if (result.next())
                throw new Exception("Unexpected entry: " + result.getInt(1) + ',' + result.getString(2));
        } finally {
            try {
                con.getClass().getMethod("close").invoke(con);
            } catch (InvocationTargetException x) {
                if (!"jakarta.resource.spi.IllegalStateException".equals(x.getCause().getClass().getName()))
                    throw x.getCause();
            }
        }
    }

    /**
     * Look up a connection factory using a resource ref with container managed authentication.
     */
    public void testContainerManagedAuth(HttpServletRequest req, HttpServletResponse res) throws Throwable {

        // Container managed auth where bindings specify an authentication-alias
        Object cf = new InitialContext().lookup("java:comp/env/eis/cf1-container-auth-ref");
        Object con = cf.getClass().getMethod("getConnection").invoke(cf);
        try {
            Object connectionMetaData = con.getClass().getMethod("getMetaData").invoke(con);
            String userName = (String) connectionMetaData.getClass().getMethod("getUserName").invoke(connectionMetaData);
            if (!"RAR2USER".equals(userName))
                throw new Exception("User name from container-managed auth alias not honored. Instead: " + userName);
        } finally {
            try {
                con.getClass().getMethod("close").invoke(con);
            } catch (InvocationTargetException x) {
                if (!"jakarta.resource.spi.IllegalStateException".equals(x.getCause().getClass().getName()))
                    throw x.getCause();
            }
        }

        // Container managed auth using default container auth alias (containerAuthDataRef)
        cf = new InitialContext().lookup("java:comp/env/eis/cf1-default-container-auth-ref");
        con = cf.getClass().getMethod("getConnection").invoke(cf);
        try {
            Object connectionMetaData = con.getClass().getMethod("getMetaData").invoke(con);
            String userName = (String) connectionMetaData.getClass().getMethod("getUserName").invoke(connectionMetaData);
            if (!"CNTRUSER".equals(userName))
                throw new Exception("User name from containerAuthDataRef not honored. Instead: " + userName);
        } finally {
            try {
                con.getClass().getMethod("close").invoke(con);
            } catch (InvocationTargetException x) {
                if (!"jakarta.resource.spi.IllegalStateException".equals(x.getCause().getClass().getName()))
                    throw x.getCause();
            }
        }
    }

    /**
     * Test sharable connections.
     */
    public void testSharing(HttpServletRequest req, HttpServletResponse res) throws Throwable {

        Object cf = new InitialContext().lookup("java:comp/env/eis/cf1");
        Object recordFactory = cf.getClass().getMethod("getRecordFactory").invoke(cf);
        @SuppressWarnings("unchecked")
        List<Object> record = (List<Object>) recordFactory.getClass().getMethod("createIndexedRecord", String.class).invoke(recordFactory, "myRecord");
        Method Interaction_execute;

        Object con = cf.getClass().getMethod("getConnection").invoke(cf);
        try {
            Object connectionMetaData = con.getClass().getMethod("getMetaData").invoke(con);
            String userName = (String) connectionMetaData.getClass().getMethod("getUserName").invoke(connectionMetaData);
            if (!"CF1USER".equals(userName))
                throw new Exception("Incorrect user for eis/cf1: " + userName);

            Object interaction = con.getClass().getMethod("createInteraction").invoke(con);

            Class<?> InteractionSpec = con.getClass().getClassLoader().loadClass("jakarta.resource.cci.InteractionSpec");
            Class<?> Record = con.getClass().getClassLoader().loadClass("jakarta.resource.cci.Record");
            Interaction_execute = interaction.getClass().getMethod("execute", InteractionSpec, Record);

            // Clear the table contents of any previous usage
            record.add(0, "delete from cf1tbl");
            Interaction_execute.invoke(interaction, null, record);
            interaction.getClass().getMethod("close").invoke(interaction);

            // Use two connection handles in a global transaction.
            tran.begin();
            try {
                Object con1 = cf.getClass().getMethod("getConnection").invoke(cf);
                Object interaction1 = con.getClass().getMethod("createInteraction").invoke(con1);
                record.clear();
                record.add(0, "insert into cf1tbl values (?, ?)");
                record.add(1, 1);
                record.add(2, "one");
                Interaction_execute.invoke(interaction1, null, record);

                Object con2 = cf.getClass().getMethod("getConnection").invoke(cf);
                Object interaction2 = con2.getClass().getMethod("createInteraction").invoke(con2);
                record.set(1, 2);
                record.set(2, "two");
                Interaction_execute.invoke(interaction2, null, record);
                con2.getClass().getMethod("close").invoke(con2);
            } finally {
                tran.commit();
            }

            // Sharable connection should be closed now, after tran.commit
            try {
                con.getClass().getMethod("close").invoke(con);
                throw new Exception("Expecting IllegalStateException for already closed connection.");
            } catch (InvocationTargetException x) {
                if (!"jakarta.resource.spi.IllegalStateException".equals(x.getCause().getClass().getName()))
                    throw x.getCause();
            }

            // Query for the data
            con = cf.getClass().getMethod("getConnection").invoke(cf);
            interaction = con.getClass().getMethod("createInteraction").invoke(con);
            record.clear();
            record.add(0, "select col1, col2 from cf1tbl where col1 < ? order by col1");
            record.add(1, 100);
            ResultSet result = (ResultSet) Interaction_execute.invoke(interaction, null, record);

            if (!result.next())
                throw new Exception("Missing first entry (1)");
            int col1 = result.getInt(1);
            String col2 = result.getString(2);
            if (col1 != 1 || !"one".equals(col2))
                throw new Exception("Expecting (1,one) not (" + col1 + ',' + col2 + ')');

            if (!result.next())
                throw new Exception("Missing second entry (2)");
            col1 = result.getInt(1);
            col2 = result.getString(2);
            if (col1 != 2 || !"two".equals(col2))
                throw new Exception("Expecting (2,two) not (" + col1 + ',' + col2 + ')');

            if (result.next())
                throw new Exception("Unexpected entry: " + result.getInt(1) + ',' + result.getString(2));
        } finally {
            try {
                con.getClass().getMethod("close").invoke(con);
            } catch (InvocationTargetException x) {
                if (!"jakarta.resource.spi.IllegalStateException".equals(x.getCause().getClass().getName()))
                    throw x.getCause();
            }
        }
    }

    /**
     * Test the JCA timer.
     * Schedule a repeating timer task that cancels itself on the third run.
     * Schedule a one-shot timer task and cancel the timer before it starts.
     */
    public void testTimer(HttpServletRequest req, HttpServletResponse res) throws Throwable {
        Object resourceAdapterAssociation = new InitialContext().lookup("java:comp/env/eis/queue1");
        Object adapter = resourceAdapterAssociation.getClass().getMethod("getResourceAdapter").invoke(resourceAdapterAssociation);
        adapter.getClass().getMethod("testTimer").invoke(adapter);
    }

    /**
     * Tests thread context propagation for JCA work manager.
     */
    public void testWorkContext(HttpServletRequest req, HttpServletResponse res) throws Throwable {
        Object resourceAdapterAssociation = new InitialContext().lookup("java:comp/env/eis/queue1");
        Object adapter = resourceAdapterAssociation.getClass().getMethod("getResourceAdapter").invoke(resourceAdapterAssociation);
        // Run this from the application thread, so that the context of the application is available
        adapter.getClass().getMethod("testWorkContext").invoke(adapter);
    }

    /**
     * Tests work context inflow.
     */
    public void testWorkContextInflow(HttpServletRequest req, HttpServletResponse res) throws Throwable {

        Object resourceAdapterAssociation = new InitialContext().lookup("java:comp/env/eis/queue1");
        final Object adapter = resourceAdapterAssociation.getClass().getMethod("getResourceAdapter").invoke(resourceAdapterAssociation);

        try {
            // Run this from a native thread so that the context of the application is not available
            daemon.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    adapter.getClass().getMethod("testWorkContextInflow").invoke(adapter);
                    return null;
                }
            }).get(TIMEOUT * 10, TimeUnit.MILLISECONDS);
        } catch (ExecutionException x) {
            throw x.getCause();
        }
    }

    /**
     * Cause an in-doubt transaction and verify that it gets recovered.
     */
    public void testXARecovery(HttpServletRequest req, HttpServletResponse res) throws Throwable {
        PrintWriter out = res.getWriter();

        Object[] cons = new Object[2];
        try {
            // Use unsharable connections, so that they all get their own XA resources
            Object cf = new InitialContext().lookup("java:comp/env/eis/cf2");
            Object resourceAdapter = null;
            Field f = null, raField = null;
            try {
                Class cfClass = cf.getClass();
                f = cfClass.getDeclaredField("mcf");
                f.setAccessible(true);
                Object mcf = f.get(cf);
                Class mcfClass = mcf.getClass();
                raField = mcfClass.getDeclaredField("adapter");
                raField.setAccessible(true);
                resourceAdapter = raField.get(mcf);
            } finally {
                f.setAccessible(false);
                raField.setAccessible(false);
            }

            // Only allow 1 commit or rollback to succeed
            Class adapterClass = cf.getClass().getClassLoader().loadClass("test.jca.adapter.BVTResourceAdapter");
            adapterClass.getMethod("setXAResourceSuccessLimit", int.class).invoke(resourceAdapter, 1);

            cons[0] = cf.getClass().getMethod("getConnection").invoke(cf);

            Object[] interactions = new Object[2];
            interactions[0] = cons[0].getClass().getMethod("createInteraction").invoke(cons[0]);

            Class<?> InteractionSpec = cons[0].getClass().getClassLoader().loadClass("jakarta.resource.cci.InteractionSpec");
            Class<?> Record = cons[0].getClass().getClassLoader().loadClass("jakarta.resource.cci.Record");
            Method Interaction_execute = interactions[0].getClass().getMethod("execute", InteractionSpec, Record);

            // Clear the table contents of any previous usage
            Object recordFactory = cf.getClass().getMethod("getRecordFactory").invoke(cf);
            @SuppressWarnings("unchecked")
            List<Object> record = (List<Object>) recordFactory.getClass().getMethod("createIndexedRecord", String.class).invoke(recordFactory, "theRecord");
            record.add(0, "delete from cf2tbl");
            Interaction_execute.invoke(interactions[0], null, record);

            tran.begin();
            try {
                cons[1] = cf.getClass().getMethod("getConnection").invoke(cf);
                interactions[1] = cons[1].getClass().getMethod("createInteraction").invoke(cons[1]);

                // Insert some data
                record.clear();
                record.add(0, "insert into cf2tbl values (?, ?)");
                record.add(1, 160);
                record.add(2, "0xA0");
                Interaction_execute.invoke(interactions[0], null, record);

                record.set(1, 161);
                record.set(2, "0xA1");
                Interaction_execute.invoke(interactions[1], null, record);

                out.println("Intentionally causing in-doubt transaction");

                try {
                    tran.commit();
                    throw new Exception("Commit should not have succeeded because the test infrastructure is supposed to cause an in-doubt transaction.");
                } catch (HeuristicMixedException x) {
                    out.println("Caught expected HeuristicMixedException: " + x.getMessage());
                }
            } catch (Throwable x) {
                adapterClass.getMethod("setXAResourceSuccessLimit", int.class).invoke(resourceAdapter, Integer.MAX_VALUE);
                try {
                    tran.rollback();
                } catch (Throwable t) {
                }
                throw x;
            } finally {
                adapterClass.getMethod("setXAResourceSuccessLimit", int.class).invoke(resourceAdapter, Integer.MAX_VALUE);
            }

            // At this point, the transaction is in-doubt.
            // We won't be able to access the data until the transaction manager recovers
            // the transaction and resolves it.

            out.println("attempting to access data (only possible after recovery)");
            record.clear();
            record.add(0, "select col1, col2 from cf2tbl where col1 < ? order by col1");
            record.add(1, 200);
            ResultSet result = (ResultSet) Interaction_execute.invoke(interactions[0], null, record);

            // Scroll the result set
            if (!result.next())
                throw new Exception("Missing first entry (160)");
            int col1 = result.getInt(1);
            String col2 = result.getString(2);
            if (col1 != 160 || !"0xA0".equals(col2))
                throw new Exception("Expecting (160,0xA0) not (" + col1 + ',' + col2 + ')');

            if (!result.next())
                throw new Exception("Missing second entry (161)");
            col1 = result.getInt(1);
            col2 = result.getString(2);
            if (col1 != 161 || !"0xA1".equals(col2))
                throw new Exception("Expecting (161,0xA1) not (" + col1 + ',' + col2 + ')');

            // There should not be any more entries
            if (result.next())
                throw new Exception("Unexpected entry: " + result.getInt(1) + ',' + result.getString(2));

            out.println("successfully accessed the data");
        } finally {
            for (Object con : cons)
                if (con != null)
                    try {
                        con.getClass().getMethod("close").invoke(con);
                    } catch (InvocationTargetException x) {
                        if (!"jakarta.resource.spi.IllegalStateException".equals(x.getCause().getClass().getName()))
                            throw x.getCause();
                    }
        }
    }
}