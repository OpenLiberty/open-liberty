<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"
%>
<!--
    Copyright (c) 2021 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License v1.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-v10.html

    Contributors:
        IBM Corporation - initial API and implementation
-->

<%!
//  only guards against xss in HTML element content 
public String xssguard(String name){
    if (name == null || name.isEmpty()) return name;
    String buf =  name.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
       .replace("\"","&quot;").replace("'","&apos;").replace("/","&#x2F;");  
    return buf;
  }
%>

<%
    String errTitle = xssguard(request.getParameter("title"));
    if( errTitle  == null || errTitle.isEmpty() ){
        errTitle = "HTTP Error Message";
    }
    String errMessage = xssguard(request.getParameter("message"));
    if( errMessage  == null || errMessage.isEmpty() ){
        errMessage = "HTTP Error 403 - Forbidden"; // "The verification on the SAML Response failed";
    }
    String errUserAction = xssguard(request.getParameter("userAction"));
    if( errUserAction  == null || errUserAction.isEmpty() ){
        errUserAction = "Please contact the administrator for further information";
    }
    String errFormAction = xssguard(request.getParameter("action"));
    if( errFormAction  == null || errFormAction.isEmpty() ){
        errFormAction = "";
    }
    String errMethod = xssguard(request.getParameter("method"));
    if( errMethod  == null || errMethod.isEmpty() ){
        errMethod = "get";
    }
%>

<html>
   <head>
      <meta name="GENERATOR" content="Software Development Platform"/>
      <meta http-equiv="Pragma" content="no-cache"/>
      <title>
          <% out.println(errTitle); %>
      </title>
   </head>
   <body>
      <table align="center" ellpadding="5" cellspacing="5" width="90%" >
         <tbody> 
           <tr> 
             <td>  
                 <h1 style="color:#ff8800"">
                    <% out.println(errTitle); %> 
                 </h1>
             </td>
           </tr>
         </tbody>
      </table>
      <hr width="100%"/>
      <table align="center" cellpadding="5" cellspacing="0" width="90%">
            <tbody>
               <tr>
                  <td style="background-color:#ffffff;color:#990000" nowrap="nowrap" width="100%">
                     <b>
                     <% out.println(errMessage); %>
                     </b>
                  </td>
               </tr>
               <tr>
                  <td style="background-color:#ffffff;color:#000000" nowrap="nowrap" width="100%">
                     <% out.println(errUserAction); %>
                  </td>
               </tr>
               <tr>
                  <td style="background-color:#ffffff;color:#000000" nowrap="nowrap" width="100%">
                 
                  </td>
               </tr>
            </tbody>
         </table>
      </center>
   </body>
</html>

