<!--
    Copyright (c) 2015 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License 2.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-2.0/
    
    SPDX-License-Identifier: EPL-2.0
   
    Contributors:
        IBM Corporation - initial API and implementation
 -->
<!DOCTYPE HTML>
<%@page language="java"
    contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<html>
<head>
<title>testJspFeaturesChanges</title>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
</head>
<body>
JSP version via getVersionInformation: <%out.write(getVersionInformation());%>
JSP version via getSpecificationVersion: <%out.write(JspFactory.getDefaultFactory().getEngineInfo().getSpecificationVersion());%>
</body>
</html>
