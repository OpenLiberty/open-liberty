<!--
    Copyright (c) 2025 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License 2.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-2.0/
    
    SPDX-License-Identifier: EPL-2.0
 -->
<%@ page contentType="text/html;charset=UTF-8" %>
<!-- 16384 -->
<%
    StringBuilder sb = new StringBuilder("");
    //32768 bytes is enough for the jsp/webcontainer buffer to commit the response
	for(int i=0; i<32768; i++){
		sb.append("0");
	}
%>
<p>Hello <%= sb.toString() %>!</p>
<% System.out.println("DEBUG: END OF FOOTER"); %>
