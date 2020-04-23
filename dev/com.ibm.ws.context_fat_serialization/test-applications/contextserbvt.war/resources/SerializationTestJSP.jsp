<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<!--
    Copyright (c) 2013,2017 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License v1.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-v10.html
   
    Contributors:
        IBM Corporation - initial API and implementation
 -->
<%@ page import="java.io.BufferedOutputStream"%>
<%@ page import="java.io.FileOutputStream"%>
<%@ page import="java.io.ObjectOutputStream"%>
<%@ page import="java.util.concurrent.Executor"%>
<%@ page import="javax.enterprise.concurrent.ContextService"%>
<%@ page import="javax.naming.InitialContext"%>
<%@ page import="test.context.serialization.app.CurrentThreadExecutor"%>
<html>
 <head>
  <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
  <title>Context Service Serialization Test JSP</title>
 </head>
 <body>
   <%
    String outputMessage;
    String test = request.getParameter("test");
    System.out.println("-----> " + test + " starting");
    try {
        if ("testSerializeJEEMetadataContext".equals(test)) {
            ContextService jeeMetadataContextSvc = (ContextService) new InitialContext().lookup("concurrent/jeeMetadataContextSvc");
            Executor contextualExecutor = jeeMetadataContextSvc.createContextualProxy(new CurrentThreadExecutor(), Executor.class);
            ObjectOutputStream outfile = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream("jeeMetadataContext-JSP-vNext.ser")));
            try {
                outfile.writeObject(contextualExecutor);
            } finally {
                outfile.close();
            }
        } else
        	throw new Exception("unknown test name: " + test);

        System.out.println("<----- " + test + " successful");
        outputMessage = test + " COMPLETED SUCCESSFULLY";
    } catch (Throwable x) {
        System.out.println("<----- " + test + " failed:");
        x.printStackTrace(System.out);
        outputMessage = "<pre>ERROR in " + test + ":" + x + "</pre>";
    }
  %>
  <%=outputMessage%>
 </body>
</html>