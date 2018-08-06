<!-- 
   Simple error page for reporting application errors.
   This error page is called when a servlet throws an Exception, or by calling
   response.sendError().  Error pages can use the request-scoped bean named
   "ErrorReport" to get more information about the error.
--->

<jsp:useBean id="ErrorReport" scope="request" class="com.ibm.websphere.servlet.error.ServletErrorReport"/>
<!DOCTYPE HTML PUBLIC "-//W3C/DTD HTML 4.0 Transitional//EN">
<html>
<head><title>Error <%=ErrorReport.getErrorCode()%></title></head>
<body>


<H1>SecFVTServlet1 Error <%= ErrorReport.getErrorCode() %> </H1>
<% if(ErrorReport.getErrorCode() >= 500 && ErrorReport.getErrorCode() != response.SC_SERVICE_UNAVAILABLE) { 
%>
<H4>An error has occured while processing request: <%= HttpUtils.getRequestURL(request) %></H4>
<B>Message: </B><%= ErrorReport.getMessage() %><BR>
<B>StackTrace: </B><%= ErrorReport.getStackTrace() %> 

<%}else if(ErrorReport.getErrorCode() == response.SC_NOT_FOUND){
%>
Document Not Found
<%}%>

</html>
