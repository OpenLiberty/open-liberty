<!--
    Copyright (c) 2024 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License 2.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-2.0/
    
    SPDX-License-Identifier: EPL-2.0
 -->
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ taglib prefix="test2" uri="io.test.one.tld"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
        <title>Global TLD: Test 2</title>
    </head>
    <body>
        <!--
            Expected to work 
            We are using the old constructor, so the TLD file'S URI should be honored
            "addtoTldPathList(new TldPathConfig("WEB-INF/tld/test2.tld", "/WEB-INF/tld/test1.tld", null));"
        -->
        <test2:Sample>io.test.one.tld</test2:Sample>
    </body>
</html>
