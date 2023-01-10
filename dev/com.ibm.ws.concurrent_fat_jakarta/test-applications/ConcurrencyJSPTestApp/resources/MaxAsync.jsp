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
<%@ page import="java.io.PrintWriter,java.io.StringWriter,java.util.concurrent.Callable,java.util.concurrent.Exchanger,java.util.concurrent.Future,java.util.concurrent.TimeoutException,java.util.concurrent.TimeUnit,javax.naming.InitialContext,jakarta.enterprise.concurrent.ManagedScheduledExecutorService" %>

<% 
String testResult = null;
long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(2);
try {
    Exchanger<String> status = new Exchanger<String>();
    ManagedScheduledExecutorService executor = InitialContext.doLookup("java:module/concurrent/scheduledExecutor1");
    Callable<String> checkMaxAsyncAndContext = () -> {
        String s = status.exchange("Task is running", TIMEOUT_NS, TimeUnit.NANOSECONDS);
        if (!"Ready to complete task".equals(s))
            return "Must not run both tasks concurrently when maxAsync is 1: " + s;

        // Security context must be cleared for securityClearedContextSvc
        if (request.getUserPrincipal() != null)
            return "Security context should have been cleared on first task. UserPrincipal Name should have been null but was " +
                   request.getUserPrincipal().getName();

        // Application context must be propagated, allowing lookup from java:module to succeed,
        InitialContext.doLookup("java:module/concurrent/scheduledExecutor1");

        return null;
    };

    Future<String> future1 = executor.submit(checkMaxAsyncAndContext);
    Future<String> future2 = executor.submit(checkMaxAsyncAndContext);

    // These will only be possible if 2 copies of the task exchange with eachother,
    // which means they are running at the same time in violation of max-async 1

    try {
        testResult = future1.get(1, TimeUnit.SECONDS);
    } catch (TimeoutException x) {
        // expected
    }

    if (testResult == null)
        try {
            testResult = future2.get(1, TimeUnit.SECONDS);
        } catch (TimeoutException x) {
            // expected
        }

    // Allow the tasks to complete,
    status.exchange("Ready to complete task", TIMEOUT_NS, TimeUnit.NANOSECONDS);
    status.exchange("Ready to complete task", TIMEOUT_NS, TimeUnit.NANOSECONDS);

    String failure1 = future1.get(TIMEOUT_NS, TimeUnit.SECONDS);
    if (testResult == null && failure1 != null)
        testResult = failure1;

    String failure2 = future2.get(TIMEOUT_NS, TimeUnit.SECONDS);
    if (testResult == null && failure2 != null)
        testResult = failure2;

    if (testResult == null) // no failures to report
        testResult = "SUCCESS";
} catch (Throwable e) { // convert to string for easier debugging
    if (testResult == null) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        testResult = sw.toString();
    }
}
%>
<%=testResult%>
