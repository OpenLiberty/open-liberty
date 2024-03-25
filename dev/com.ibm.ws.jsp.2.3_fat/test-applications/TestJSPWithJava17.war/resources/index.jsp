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
    <title>Java 17</title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
  </head>

  <body>
      <!-- Test Java 17's Text Block -->
    <% 
      String block = """
        If this compiles, then the 
        test passes. " success-text-block" woo!
        """;
      out.print(block);
    %>

      <!-- Test instanceOf Java 17's pattern matching  -->
    <%
      Object o = " success-pattern-matching ";
      if (o instanceof String s) {
        out.print(s.strip()); // no need to cast to String 
      }
    %>
  </body>
</html>
