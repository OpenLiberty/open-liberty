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
<%@ page import="java.io.StringWriter,java.io.PrintWriter,javax.naming.InitialContext,jakarta.enterprise.concurrent.ContextService,java.util.function.Supplier" %>

<% 
String output = "SUCCESS";
try {
	ContextService contextSvc =  InitialContext.doLookup("java:app/concurrent/securityClearedContextSvc");
	Supplier<String> contextualSupplier = contextSvc.contextualSupplier(() -> {
        // Security Context should be cleared for securityClearedContextSvc
        return request.getUserPrincipal() == null ? "null" : request.getUserPrincipal().getName();
    });
    String name = contextualSupplier.get();
    if(!name.equals("null")) {
        output = "Security context should have been cleared. UserPrincipal Name should have been (null) but was (" + name + ")";
    };

} catch (Exception e) { //Return any exceptions thrown by the test as a string for easier debugging
    StringWriter sw = new StringWriter();
    e.printStackTrace(new PrintWriter(sw));
    output = sw.toString();
}
%>
<%=output%>
