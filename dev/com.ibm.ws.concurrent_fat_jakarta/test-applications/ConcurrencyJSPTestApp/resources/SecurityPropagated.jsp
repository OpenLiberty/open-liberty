<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.io.StringWriter,java.io.PrintWriter,javax.naming.InitialContext,java.util.concurrent.CompletableFuture,jakarta.enterprise.concurrent.ManagedExecutorService" %>

<% 
String output = "SUCCESS";
try {
    ManagedExecutorService executor = InitialContext.doLookup("concurrent/executor1");
    CompletableFuture<String> future = executor.supplyAsync(() -> {
        // Security Context should be propogated for the default ContextService
        return request.getUserPrincipal() == null ? "null" : request.getUserPrincipal().getName();
    });
    String name = future.join();
    if(!name.equals("concurrency")) {
        output = "UserPrincipal Name should have been (concurrency) was (" + name + ")";
    };

} catch (Exception e) { //Return any exceptions throw by the test as a string for easier debugging
    StringWriter sw = new StringWriter();
    e.printStackTrace(new PrintWriter(sw));
    output = sw.toString();
}
%>
<%=output%>
