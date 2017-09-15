<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<%-- jsf:pagecode language="java" location="/src/pagecode/JCAViolationDetection_e.java" --%><%-- /jsf:pagecode --%><%@page
	language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<f:subview id="jcaViolation">
	<body>

	<form action="<%=request.getContextPath()%>/PMVServlet" method="post">
	
		<table border="1" width="680px" cellspacing='0'cellpadding='0'>
		<tr>
	        <td colspan="2" align="center">&nbsp; <font size="8">Not running this in V7.0</font><br>
	        
	       </td>
	  	</tr>
		<tr>
	        <td colspan="2" align="center">&nbsp; <font size="4">Connection Info</font><br>
	        
	       </td>
	  	</tr>
	  	<tr>
		    <td>&nbsp; Connection Type: </td>
		    <td><select name="ConnectionType" size="1">
			    <option value="JMSQueueConnection">JMSQueueConnection</option>
				<option value="JMSTopicConnection">JMSTopicConnection</option>
				<option value="DatabaseConnection">DatabaseConnection</option>
			</select></td>
		    
	  	</tr>
		<tr height="20">
	
		</tr>
	 	<%-- 
	  	<tr>    
		    <td>&nbsp; Resource JNDI/Reference name : </td>
			<td><input type="text" name="JNDIName"></td>
		   
	  	</tr>
		--%>
	
	  	
	  	<tr height="20">
	  		<td colspan="2">
			<br>
			<br>
			</td>
		</tr>
		</table>
		<table border="1" width="680px" cellspacing='0'cellpadding='0'>
	 		<tr>
  		
        <td colspan="2" align="center">
			<font size="4">JCA Programming Model Violation mode</font><br>
        	<B>More than one mode can be selected to test at a time!</B><br>
       	</td>
  		</tr>
   		<tr>
        	<td>			MultiThreadedConnectionUsage : </td>
        	<td><p>
        <input name="mode" type="checkbox" value="MultipleServletThreads"> MultipleServletThreads<p>
        <input name="mode" type="checkbox" value="MultipleEJBThreads"> MultipleEJBThreads</td>
        
   </tr>
	<tr height="20">
	
	</tr>
	   <tr>
	        <td>
				CrossComponentConnectionUsage :</td>
	        <td><p>
	        <input name="mode" type="checkbox" value="MultipleServletEJBComponents"> MultipleServletEJBComponents<p>
	        <input name="mode" type="checkbox" value="MultipleEJBComponents"> MultipleEJBComponents</td>
	        
	  </tr>
	
	</table>
		<br>
		<br>
	
	
		<input name="" type="submit" value="submit">
			
	</form>
	</body>

</f:subview>