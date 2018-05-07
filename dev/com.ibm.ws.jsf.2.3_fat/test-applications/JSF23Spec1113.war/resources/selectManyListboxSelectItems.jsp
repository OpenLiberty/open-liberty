<!--
    Copyright (c) 2017, 2018 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License v1.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-v10.html
   
    Contributors:
        IBM Corporation - initial API and implementation
 -->
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>

<html>
    <head>
        <title>JSP SelectManyListbox SelectItems</title>
    </head>

    <body>
    	<H2>JSP SelectManyListbox: selectItems</H2>
    	<br/>
        <f:view>
            <h:selectManyListbox id="listbox1" onselect="jsFunction">
                <f:selectItem id="item1" itemLabel="Hello" itemValue="true" />
                <f:selectItem id="item2" itemLabel="World" itemValue="false" />
            </h:selectManyListbox> 
        </f:view>
    </body>
</html>
