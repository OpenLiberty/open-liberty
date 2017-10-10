<%--
    Copyright (c) 2017 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License v1.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-v10.html
   
    Contributors:
        IBM Corporation - initial API and implementation
 --%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">

<%@page import = "java.util.*"
import = "java.io.*"
import = "javax.security.auth.Subject"
import = "javax.security.auth.*"
import = "com.ibm.websphere.security.auth.WSSubject"
import = "com.ibm.websphere.security.cred.WSCredential"
%>

<%

java.security.Principal principal = request.getUserPrincipal();
String principalName = null;
String remoteUser = request.getRemoteUser();
String authType = request.getAuthType();
String requestURL = request.getRequestURL().toString();
if ( principal != null ) {
     principalName = principal.getName();
 }
            
boolean inManagerRole = request.isUserInRole("Manager");
boolean inEmployeeRole = request.isUserInRole("Employee");
boolean inMappedToEmployee = request.isUserInRole("MappedToEmployee");
boolean inMappedToManager = request.isUserInRole("MappedToManager");

String role = request.getParameter("role");
String roleToPrint = null;
if (role == null){
  roleToPrint = "isUserInRole(" + role + "): " + request.isUserInRole(role);
}

String cookiesToPrint = null;
Cookie [] cookies = request.getCookies();
if (cookies != null && cookies.length > 0) {
   for (int i = 0; i < cookies.length; i++) {
      cookiesToPrint = cookiesToPrint + "cookie: " + cookies[i].getName() + " value: " + cookies[i].getValue();
   }
} 

String callerToPrint = null;
String credToPrint = null;
String runAsSubjectToPrint = null;
String exceptionToPrint = null;

try {
// Get the CallerSubject
Subject callerSubject = com.ibm.websphere.security.auth.WSSubject.getCallerSubject();
if (callerSubject != null)
   callerToPrint = callerSubject.toString();

// Get the public credential from the CallerSubject
if (callerSubject != null) {
	WSCredential callerCredential = callerSubject.getPublicCredentials(WSCredential.class).iterator().next();
	if (callerCredential != null) 
		credToPrint = callerCredential.toString();
}

// Get the invocation subject for run-as tests
Subject runAsSubject = com.ibm.websphere.security.auth.WSSubject.getRunAsSubject();
if (runAsSubject != null) 
   runAsSubjectToPrint = runAsSubject.toString();
}
catch (NoClassDefFoundError ne)
// For OSGI App testing (EBA file). We expect this exception for all packages that are not public.
{
exceptionToPrint = "No ClassDefFoundError for SubjectManager: " + ne;
}

%>

<HTML>
<BODY>
    <H1>Welcome to JSP for BasicAuth</H1>
    
    <p><br>JSPName: BasicAuthJSP.jsp
    <p><br>getAuthType: <%= authType %>
    <p><br>getRemoteUser: <%= remoteUser %>
    <p><br>getUserPrincipal: <%= principal %>
    <p><br>getUserPrincipal().getName(): <%= principalName %>
    <p><br>methodCalled: <%= request.getMethod() %>
    <p><br>request URI: <%= request.getRequestURI() %>
    <p><br>isUserInRole(Employee): <%= inEmployeeRole %>
    <p><br>isUserInRole(Manager): <%= inManagerRole %>
    <p><br>isUserInRole(MappedToEmployee): <%= inMappedToEmployee %>
    <p><br>isUserInRole(MappedToManager): <%= inMappedToManager %>
    <p><br><%= roleToPrint %>
    <p><br>Getting cookies: <%= cookiesToPrint %>
    <p><br>getRequestURL: <%= requestURL %>
    <p><br>callerSubject: <%= callerToPrint %>
    <p><br>callerCredential: <%= credToPrint %>
    <p><br>runAsSubject: <%= runAsSubjectToPrint %>
    <br>&nbsp    
</BODY>
  
</HTML>


