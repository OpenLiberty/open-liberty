<%-- 
  Copyright (c) 2021 IBM Corporation and others.
  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 
  Contributors:
      IBM Corporation - initial API and implementation
--%>
<%@ page  import="java.io.PrintWriter" %>
<%@ page  import="java.io.OutputStreamWriter" %>
<%
	PrintWriter localOut;
    String requestGetContextPath = request.getContextPath();
    String contextGetContextPath = getServletContext().getContextPath();
    StringBuffer testMessage = new StringBuffer("Results: ");
	try {
		localOut = response.getWriter();
	} catch (IllegalStateException e) {
		localOut = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), response.getCharacterEncoding()));
	}

    testMessage.append("Test request.getContextPath()=");
    if (requestGetContextPath != null && requestGetContextPath.endsWith("/")){
        testMessage.append("[FAIL]");
        System.out.println("Test request.getContextPath() FAIL");
    }else
         testMessage.append("[PASS]");

    testMessage.append(".  Test servletContext.getContextPath()=");
    if (contextGetContextPath != null && contextGetContextPath.endsWith("/")){
        testMessage.append("[FAIL]");
        System.out.println("Test servletContext.getContextPath() FAIL");
    }else
         testMessage.append("[PASS]");
    
    localOut.println(testMessage.toString());
%>

