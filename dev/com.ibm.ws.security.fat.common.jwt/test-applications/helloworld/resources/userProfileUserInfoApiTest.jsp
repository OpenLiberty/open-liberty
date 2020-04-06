<%@ page language="java" contentType="text/html; charset=UTF-8"

    pageEncoding="UTF-8"

    import="java.util.List"
    import="java.util.Map"
    import="java.util.ArrayList"
    import="java.util.Collection"
    import="com.ibm.websphere.security.auth.WSSubject"
    import="javax.security.auth.Subject"
    import="java.util.Set"
    import="java.security.AccessController"
    import="java.security.PrivilegedAction"

%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<title>User Login Test Page</title>
</head>
<body style="margin:10px; padding:10px">
<h2>User Login Test Servlet</h2>

<br>
<br>Description: this is a tiny sample servlet that prints the authenticed user principal.
<br>and invokes social's UserProfile.getUserInfo api.
<br>It's safe to package in environments where social isn't installed, because it won't 
<br>get compiled unless it's invoked. 
<br>
<br>

<%
    Object o = request.getUserPrincipal();
    if (o == null) {
        %>user principal is null<br><%
    } else {
        %>And the current user is:<h2><pre><%=o.toString()%></pre></h2><%
    }
%>
<br/>
<%
    String userInfo = null;
    try {
        Class.forName("com.ibm.websphere.security.social.UserProfile");

        Set s = AccessController.doPrivileged(new PrivilegedAction<Set>() {
            public Set run() {
                Subject subj;
                try {
                    subj = WSSubject.getRunAsSubject();
                } catch (Exception ex) {
                    System.out.println("*** userprofileuserinfoapitest.jsp caught unexpected Exception: " + ex);
                    ex.printStackTrace();
                    return null;
                }
                return subj.getPrivateCredentials(com.ibm.websphere.security.social.UserProfile.class);
            }
        });
        if (s != null && !s.isEmpty()) {
            com.ibm.websphere.security.social.UserProfile u = (com.ibm.websphere.security.social.UserProfile)s.iterator().next();
            userInfo = u.getUserInfo();
        }

    } catch (ClassNotFoundException cnex) {
        %>UserProfile class not found<br><%
        System.out.println("Caught ClassNotFoundException: " + cnex);
        cnex.printStackTrace();
    }

    if (userInfo == null) {
        %>user info is null<%
    } else {
        %>user info is: <h2><pre><%=userInfo%></pre></h2><%
    }

%>

</body>
</html>