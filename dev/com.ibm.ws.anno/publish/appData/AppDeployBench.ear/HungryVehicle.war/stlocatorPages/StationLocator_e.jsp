<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%-- jsf:pagecode language="java" location="/src/pagecode/stlocatorPages/StationLocator_e.java" --%><%-- /jsf:pagecode --%><%@page
	language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@taglib uri="http://www.ibm.com/jsf/html_extended" prefix="hx"%>
<%@taglib uri="http://www.ibm.com/jsf/BrowserFramework" prefix="odc"%>

<f:subview id="locator_view">
	<body>
	<hx:scriptCollector id="scriptCollector1">
		<h:form styleClass="form" id="form1">

			<odc:tabbedPanel slantInactiveRight="4" width="650"
				styleClass="tabbedPanel" height="300" slantActiveRight="4"
				showBackNextButton="false" id="tabbedPanel1" showTabs="true"
				variableTabLength="false">
				<odc:bfPanel id="bfpanel1" name="Station Locator"
					showFinishCancelButton="false">

					<br>

					<br>
There are various Stations across many states in US which are registered with GasNetwork Inc. <br><br><br>
					<table cellpadding="0" cellspacing="0" border="0">
						<tr>
						<td ><font face="arial" color="333399" size="1"><b>Enter your location to find the stores in your area:</b></font></td>

					</tr>
					<tr>
						<td>
					Please choose the state

					<h:selectOneMenu styleClass="selectOneMenu" id="menu1">
						<f:selectItem itemLabel="States" itemValue="US" />
					</h:selectOneMenu>
					</td>
					</tr>
					</table>

					<br>
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