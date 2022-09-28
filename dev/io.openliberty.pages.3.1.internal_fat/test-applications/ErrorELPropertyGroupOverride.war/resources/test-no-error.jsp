<!--
    Copyright (c) 2022 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License v1.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-v10.html

    Contributors:
        IBM Corporation - initial API and implementation
 -->
 <%@ page errorOnELNotFound="false" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
    <title>Unidentified EL Expression Exception</title>
</head>
<body>
    <%-- test is undefined and pages should throw an exception when error-On-EL-Not--Found jsp property group is true --%>
    <div> Exception should not be thrown: ${test} here </div>
</body>
</html>
