<!--
    Copyright (c) 2023, 2024 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License 2.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-2.0/
    
    SPDX-License-Identifier: EPL-2.0
 -->
 <html>
    <title>errorPage</title>
    <body>
        <%@ page isErrorPage="true"  %>
        <% out.println("This is a test error page"); %>
        <% out.println("Exception encountered is: " + exception.getMessage()); %>
        <br/>
        <!-- Verifying Pages 4.0 queryString:  -->
        queryString: ${pageContext.errorData.queryString}

         <!-- Verifying Pages 4.0 method:  -->
         errorMethod: ${pageContext.errorData.method}
    </body>
</html>
