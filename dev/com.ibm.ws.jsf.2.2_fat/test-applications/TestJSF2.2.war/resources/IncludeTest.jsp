<!--
 * Copyright (c)  2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 -->
<%@ page contentType="text/html" pageEncoding="UTF-8" %>
<%@ taglib prefix="f" uri="http://java.sun.com/jsf/core" %>
<%@ taglib prefix="h" uri="http://java.sun.com/jsf/html" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
    "http://www.w3.org/TR/html4/loose.dtd">

<%-- This file is an entry point for JavaServer Faces application. --%>lz
<f:view>
    <html>
        <head>
            <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
            <title>Include Testcase</title>
        </head>
        <body>
            <h:form id="form1">          
                <f:subview id="subview1">
                    <jsp:include page="Include.jsp"/>
                </f:subview>
                <f:subview id="subview2">
                    <jsp:include page="Include.jsp"/>
                </f:subview>
            </h:form>
        </body>
    </html>
</f:view>
