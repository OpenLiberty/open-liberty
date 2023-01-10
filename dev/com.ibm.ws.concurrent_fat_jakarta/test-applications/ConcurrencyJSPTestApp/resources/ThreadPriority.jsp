<%--
    Copyright (c) 2022 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License 2.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-2.0/
    
    SPDX-License-Identifier: EPL-2.0

    Contributors:
        IBM Corporation - initial API and implementation
 --%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.io.PrintWriter,java.io.StringWriter,java.util.concurrent.CompletableFuture,java.util.concurrent.TimeoutException,java.util.concurrent.TimeUnit,javax.naming.InitialContext,jakarta.enterprise.concurrent.ManagedThreadFactory,javax.naming.NamingException" %>

<% 
String testResult = null;
long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(2);
try {
    CompletableFuture<String> future = new CompletableFuture<String>();
    ManagedThreadFactory threadFactory = InitialContext.doLookup("java:comp/concurrent/threadFactory7");
    Thread thread = threadFactory.newThread(() -> {
        // Application context must be cleared
        try {
            Object unexpected = InitialContext.doLookup("java:comp/concurrent/threadFactory7");
            future.complete("Application context should have been cleared. Looked up " + unexpected);
        } catch (NamingException x) {
            // expected
        } catch (Throwable x) {
            future.completeExceptionally(x);
        }

        try {
            // Thread priority must match managed-thread-factory config
            int priority = Thread.currentThread().getPriority();

            // Security context must be propagated
            String name = request.getUserPrincipal() == null ? null : request.getUserPrincipal().getName();

            future.complete(priority + "," + name);
        } catch (Throwable x) {
            future.completeExceptionally(x);
        }
    });

    int threadPriority = thread.getPriority();
    if (threadPriority != 7)
        testResult = "ManagedThreadFactory created thread with priority " + threadPriority;

    thread.start();

    String result = future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
    if (testResult == null)
        if ("7,concurrency".equals(result))
            testResult = "SUCCESS";
        else
            testResult = "Unexpected result: " + result;
} catch (Throwable e) { // convert to string for easier debugging
    if (testResult == null) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        testResult = sw.toString();
    }
}
%>
<%=testResult%>
