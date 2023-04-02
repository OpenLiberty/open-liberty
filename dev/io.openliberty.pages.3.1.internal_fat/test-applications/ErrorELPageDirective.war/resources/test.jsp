<!--
    Copyright (c) 2022 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License 2.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-2.0/
    
    SPDX-License-Identifier: EPL-2.0

    Contributors:
        IBM Corporation - initial API and implementation
 -->
<%@ page errorOnELNotFound="true" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
  <title>Unidentified EL Expression Exception</title>
</head>
  <body>
    <%-- test is undefined and pages should throw an exception when errorOnELNotFound=true --%>
    <div> Exception should be thrown: ${test} </div>
  </body>
</html>
