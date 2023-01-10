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
<%@ page import="java.io.StringWriter,java.io.PrintWriter,javax.naming.InitialContext,java.util.concurrent.CompletableFuture,jakarta.enterprise.concurrent.ManagedExecutorService,jakarta.enterprise.concurrent.ContextService,java.util.function.Supplier" %>

<% 
String output = "SUCCESS";
try {
	ContextService contextSvc =  InitialContext.doLookup("java:app/concurrent/securityUnchangedContextSvc");
	Supplier<String> contextualSupplier = contextSvc.contextualSupplier(() -> {
        // Security Context should be availible for calls on the same thread
        return request.getUserPrincipal() == null ? "null" : request.getUserPrincipal().getName();
    });
    String name = contextualSupplier.get();
    if(!name.equals("concurrency")) {
        output = "Security Context should have been propagated. UserPrincipal Name should have been (concurrency) was (" + name + ")";
    };
    
    ManagedExecutorService executor = InitialContext.doLookup("java:app/concurrent/executor2");
    CompletableFuture<String> future = executor.supplyAsync(() -> {
        // Security Context should not be available for calls on a new thread
        return request.getUserPrincipal() == null ? "null" : request.getUserPrincipal().getName();
    });
    name = future.join();
    if(!name.equals("null")) {
        output = "Security context should not have been propogated.  UserPrincipal Name should have been (null) but was (" + name + ")";
    };

} catch (Exception e) { //Return any exceptions thrown by the test as a string for easier debugging
    StringWriter sw = new StringWriter();
    e.printStackTrace(new PrintWriter(sw));
    output = sw.toString();
}
%>
<%=output%>