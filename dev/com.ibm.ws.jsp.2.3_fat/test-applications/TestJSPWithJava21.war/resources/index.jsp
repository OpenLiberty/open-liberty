<!--
    Copyright (c) 2023 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License 2.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-2.0/
    
    SPDX-License-Identifier: EPL-2.0
 -->
 <!DOCTYPE HTML>
 <html>
 
   <head>
     <title>Java 21</title>
     <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
   </head>
 
   <body>
       <!-- Test Java 21's Collection#getFirst method -->
     <% 
       java.util.ArrayList<String> list = new java.util.ArrayList<String>();
       list.add("getFirst success!");
       out.println(list.getFirst());
     %>
 
       <!-- Test instanceOf Java 21's Pattern Matching for Switch  -->
     <%
       Object test = "hello";
       switch(test){
         case Integer i -> out.println("switch fail!");
         case String s ->  out.println("switch success!");
         default  -> out.println("switch fail!");
       }
     %>
   </body>
</html>
