<!--
    Copyright (c) 2023 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License 2.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-2.0/
    
    SPDX-License-Identifier: EPL-2.0
 -->
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>

    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
        <title>Test Java 11 Compilation</title>
    </head>

    <body>
        Testing Java 11's String.strip function:
        <% out.print(" success-strip ".strip()); %> 
        <br/>

        Testing Java 11's String.lines function: 
        <% out.println(" Hello World\n Good Bye\n success-lines".lines()); %>
        <br />

        Java 10's Map.copyOf static method
        <% 
            java.util.Map<String,String> map = new java.util.HashMap<String,String>();
            map.put("key","success-copyof");
            out.println(java.util.Map.copyOf(map)); // {key=value}
        %>
    </body>
</html>
