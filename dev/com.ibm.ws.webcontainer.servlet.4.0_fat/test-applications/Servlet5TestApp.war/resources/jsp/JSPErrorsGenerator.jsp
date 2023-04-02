<!--
  Copyright (c) 2021 IBM Corporation and others.
  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License 2.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-2.0/
 
  Contributors:
      IBM Corporation - initial API and implementation
 -->
<%@ page  import="java.io.IOException" %>

<%
	System.out.println("JSPErrorsGenerator test. START");
	String test = request.getParameter("test");
	System.out.println("JSPErrorsGenerator testing  : [" + test + "]");

	if (test != null && test.toLowerCase().startsWith("tellerrorpagetogeneraterecursiveerror")) {
		System.out.println("JSPErrorsGenerator sends 500 with message to have error page generating recurive error");
		response.sendError(500, "tellErrorPageToGenerateRecursiveError");
	} else {
		System.out.println("Test not recognized : " + test);
		response.sendError(500, "Unknown test request : " + test);
	}

	System.out.println("JSPErrorsGenerator test. END");
%>
