<!--
    Copyright (c) 2023 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License 2.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-2.0/
    
    SPDX-License-Identifier: EPL-2.0
 -->
 <%@ page import = "static io.openliberty.pages31.fat.misc.Cafe.NAME, static io.openliberty.pages31.fat.misc.Cafe.SPECIALTY, static io.openliberty.pages31.fat.misc.AnInterface.FIELD" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>

<title>jsp:Import Static</title>
</head>
<body>

    <p> Verify static imports are seen via Expression Language and JSP Expressions</p>

    <br/>

    EL Field expression: ${NAME}
    <br/>
    EL Method expression: ${SPECIALTY()}
    <br>
    EL Interface Field expression: ${FIELD} (will NOT be evaluated - see OLGH25135) 
    <br>

    <br/>
    <br/>
    
    JSP Field expression: <%=NAME%>
    <br/>
    JSP Method expression: <%=SPECIALTY()%>
    <br>
    JSP Static Interface Field expression: <%=FIELD%>
   
</body>
</html>
