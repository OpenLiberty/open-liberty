<!--
    Copyright (c) 2021 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License v1.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-v10.html
   
    Contributors:
        IBM Corporation - initial API and implementation
 -->
 <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>  
 <%@ taglib prefix="x" uri="http://java.sun.com/jsp/jstl/xml" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
    <title>Function Tags</title>
</head>
<body>

    <p>Testing x:parsee, x:out, x:if</p>

    <!-- As of Feb 17th, 2020 :) -->
    <c:set var="stocks">  
       <list>
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
        </list> 
    </c:set>

    <x:parse xml="${stocks}" var="results"/>  

    <x:if select="$results/list/stock[2]/price > 100">  
        <p><x:out select="$results/list/stock[2]/name"/> is trading above 100<p> 
    </x:if>  

</body>
</html>

