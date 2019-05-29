<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%-- jsf:pagecode language="java" location="/src/pagecode/JCAViolationDetection.java" --%><%-- /jsf:pagecode --%><%@page
	language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<html>
<head>
<link rel="stylesheet"
	href="theme/stylesheet.css"
	type="text/css">
<link rel="stylesheet"
	href="theme/gasstation.css"
	type="text/css">
<title>JCAViolationDetection</title>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<meta name="GENERATOR" content="Rational Application Developer">
</head>
<f:view>
	<body>
<div id="container">

		<h:panelGrid styleClass="panelGrid" id="grid1" columns="2">
		
			<f:facet name="header">
				<h:panelGroup styleClass="panelGroup" id="group1">
					<div id="header_section">
					
					<jsp:include 
							page="/templatePages/Top_Station.jsp" flush="false">
			 		</jsp:include>
					
					</div>
				</h:panelGroup>
			</f:facet>
	
			<h:column>		         
		         <div id="leftcenter_section">
		         	<jsp:include 
							page="JCAViolationDetection_e.jsp" flush="false">
			 		</jsp:include>
		         </div>
			</h:column>
				
			<h:column>
		        <div id="right_section">
		        	<jsp:include 
							page="/templatePages/Right_Station.jsp" flush="false">
			 		</jsp:include>
		        </div>
			</h:column>
	
	
			<f:facet name="footer">
				<h:panelGroup styleClass="panelGroup" id="group2">
				<div id="footer_section">
				
					<jsp:include 
							page="/templatePages/Bottom_Station.jsp" flush="false">
			 		</jsp:include>
				
				</div>

			</h:panelGroup>
			</f:facet>

		</h:panelGrid>

	
		</div>
	</body>
</f:view>
</html>