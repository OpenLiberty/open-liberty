<!--
    Copyright (c) 2015 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License v1.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-v10.html
   
    Contributors:
        IBM Corporation - initial API and implementation
 -->
<%@taglib prefix="test" tagdir="/WEB-INF/tags" %>
<%@attribute name="number" required="true" type="java.lang.Double" %>
<%@tag body-content="empty" %>

Testing Coercion of a Value X to Type Y.
<br/>
Test if X is null and Y is not a primitive type and also not a String, return null (Expected:true): ${number == null}


