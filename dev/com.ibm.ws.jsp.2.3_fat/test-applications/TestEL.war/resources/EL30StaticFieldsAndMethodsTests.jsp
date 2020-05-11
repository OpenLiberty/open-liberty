<!--
    Copyright (c) 2015 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License v1.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-v10.html
   
    Contributors:
        IBM Corporation - initial API and implementation
 -->
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>TestLambda</title>
</head>
 
<%
pageContext.getELContext().getImportHandler().importPackage("com.ibm.ws.jsp23.fat.testel.beans");
%>
	<body>
	<em><b>EL 3.0 Static Field/Method Reference Tests</b></em>
	<br><br>
	Format:
	<br>
	EL expression | expected =? result	
	<br><br>
	
	Boolean.TRUE | true =? ${Boolean.TRUE}
	<br>
        // Invoke a constructor with an argument (spec 1.22)	
	<br>

	Boolean(true) | true =? ${Boolean(true)}
	<br>
        // Invoke a constructor with an argument (spec 1.22)	
	<br>
	Integer('1000') | 1000 =? ${Integer('1000')}
	<br>
	
	=== Evaluate custom beans.* classes ===
	<br>
        // Reference a static field on custom enum
	<br>
	EL30StaticFieldsAndMethodsEnum.TEST_ONE | TEST_ONE =? ${EL30StaticFieldsAndMethodsEnum.TEST_ONE}
	<br>
        // Reference a static field on a custom class
	<br>
	EL30StaticFieldsAndMethodsBean.staticReference | static reference =? ${EL30StaticFieldsAndMethodsBean.staticReference}
	<br>
        // Call a zero parameter custom method
	<br>
	EL30StaticFieldsAndMethodsBean.staticMethod() | static method =? ${EL30StaticFieldsAndMethodsBean.staticMethod()}
	<br>
        // Call a one parameter custom method
	<br>
	EL30StaticFieldsAndMethodsBean.staticMethodParam("static method param") | static method param =? ${EL30StaticFieldsAndMethodsBean.staticMethodParam("static method param")}
	<br>
	</body>
</html> 
