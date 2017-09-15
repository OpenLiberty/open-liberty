<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%-- jsf:pagecode language="java" location="/src/pagecode/aboutusPages/AboutUs_e.java" --%><%-- /jsf:pagecode --%><%@page
	language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@taglib uri="http://java.sun.com/jsf/core" prefix="f"%><%@taglib
	uri="http://www.ibm.com/jsf/BrowserFramework" prefix="odc"%><%@taglib
	uri="http://www.ibm.com/jsf/html_extended" prefix="hx"%><%@taglib
	uri="http://java.sun.com/jsf/html" prefix="h"%>
	

<f:subview id="aboutUs_view">
	<body>
	 
	<p></p>
	<p></p>
	<hx:scriptCollector id="scriptCollector1">

		<h:form styleClass="form" id="aboutUs_outerform1">
			<odc:tabbedPanel slantInactiveRight="4" width="650"
				styleClass="tabbedPanel" height="400" slantActiveRight="4"
				showBackNextButton="true" id="aboutUstabbedPanel1" showTabs="true"
				variableTabLength="true">
				<odc:bfPanel id="aboutUsTab1" name="Fuel Types List"
					showFinishCancelButton="false">
					
					
						<br>
<br>
					To find what fuel types services our Network of Stations provide <br>
, please click the button below
						<br>
						<br>
						
						<h:commandButton type="submit" value="Fuel Type List" styleClass="commandButton" action="PROCEED" id="button1"/>
						<br>
						<br>
						<br>
						Note: This page uses servlet and servlet-include and JDBC calls to
						database. <br>
						This functionality will test LI 2446 : Sharable Transaction Containment
						for V7.0.
					
				 
				</odc:bfPanel>
				<odc:bfPanel id="aboutUsTab2" name="Our Advertisements"
					showFinishCancelButton="false">
					Listen to our advertisements
					<hx:playerGenericPlayer styleClass="playerGenericPlayer"
						id="genericPlayer1">

					</hx:playerGenericPlayer>
					<br>
					<br>Coming Soon ... 
				</odc:bfPanel>

				<odc:bfPanel id="aboutUsTab3" name="Members Progress Bar"
					showFinishCancelButton="false">
				We are addding members , here is the where we stand,
					<hx:progressBar auto="true" timeInterval="1000" proportion="5"
						styleClass="progressBar" id="memberBar"
						style='font-family: "Trebuchet MS"; font-size: 12pt'></hx:progressBar>
					<br>Coming Soon ... 
				</odc:bfPanel>



				<f:facet name="back">
					<hx:commandExButton type="submit" value="&lt; Back"
						id="tabbedPanel1_back" style="display:none"></hx:commandExButton>
				</f:facet>
				<f:facet name="next">
					<hx:commandExButton type="submit" value="Next &gt;"
						id="tabbedPanel1_next" style="display:none"></hx:commandExButton>
				</f:facet>
				<f:facet name="finish">
					<hx:commandExButton type="submit" value="Finish"
						id="tabbedPanel1_finish" style="display:none"></hx:commandExButton>
				</f:facet>
				<f:facet name="cancel">
					<hx:commandExButton type="submit" value="Cancel"
						id="tabbedPanel1_cancel" style="display:none"></hx:commandExButton>
				</f:facet>
			</odc:tabbedPanel>
		</h:form>
		
	</hx:scriptCollector>
	
	</body>
</f:subview>
