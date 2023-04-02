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
 <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>  
 <%@ taglib prefix="x" uri="http://java.sun.com/jsp/jstl/xml" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
    <title>Function Transform Tag</title>
</head>
<body>

    <h1>Testing x:transform</h1>

    <!-- As of Feb 17th, 2020 :) -->
    <c:set var="stocks">  
       <stocks>
            <stock>  
                <name>AAPL</name>  
                <price>131</price>    
            </stock>  
            <stock>  
                <name>IBM</name>  
                <price>120</price>   
            </stock>
            <stock>  
                <name>GME</name>  
                <price>46</price>   
            </stock>  
        </stocks> 
    </c:set>

    <c:import url = "transformStyle.xsl" var = "xslt"/>
    <x:transform xml = "${stocks}" xslt = "${xslt}"/>


</body>
</html>

