<!--
  Copyright (c) 2021 IBM Corporation and others.
  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 
  Contributors:
      IBM Corporation - initial API and implementation
 -->
 <!--Use this JSP with filter to wrap the response-->

<%
    System.out.println("############ indexWrapped.jsp, about to dispatch FORWARD");
    request.getRequestDispatcher("/displayPage.jsp").forward(request,response);
    System.out.println("############ indexWrapped.jsp, returned from dispatch FORWARD");
    out.println("FAIL_CLOSE_RESPONSE_OUTPUT_AFTER_FORWARD  This text is after dispatch forward.  It should not be in the response.");
%>
