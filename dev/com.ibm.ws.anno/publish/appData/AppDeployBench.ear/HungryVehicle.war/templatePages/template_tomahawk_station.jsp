<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%-- jsf:pagecode language="java" location="/src/pagecode/templatePages/Template_tomahawk_station.java" --%><%-- /jsf:pagecode --%><%@page
	language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@taglib uri="http://myfaces.apache.org/tomahawk" prefix="t"%>
<html>
<head>
<link rel="stylesheet"
	href="../theme/stylesheet.css"
	type="text/css">
<link rel="stylesheet"
	href="../theme/gasstation.css"
	type="text/css">	
<title>template_tomahawk_station</title>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<meta name="GENERATOR" content="Rational Application Developer">
</head>
<f:view>
	<body>

	<div id="container">
		<t:panelGrid styleClass="panelGrid" id="grid1">
			<f:facet name="header">
				<t:panelGroup styleClass="panelGroup" id="group1" colspan="2">
					<div id="header_section">
					
					<jsp:include 
							page="/templatePages/Top_Station.jsp" flush="false">
			 		</jsp:include>
					
					</div>
				</t:panelGroup>
		</f:facet>
		
		<t:column>
				<div id="leftcenter_section">
		         
		         </div>
		</t:column>
		<t:column>
				<div id="right_section">
		        	<jsp:include 
							page="/templatePages/Right_Station.jsp" flush="false">
			 		</jsp:include>
		        </div>
		</t:column>
		
		<f:facet name="footer">
			<t:panelGroup styleClass="panelGroup" id="group2" colspan="2">
				<div id="footer_section">
				
					<jsp:include 
							page="/templatePages/Bottom_Station.jsp" flush="false">
			 		</jsp:include>
				
				</div>
			</t:panelGroup>
		</f:facet>
		</t:panelGrid>
		</div>
	</body>
</f:view>
</html>