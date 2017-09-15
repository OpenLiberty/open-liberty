<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%-- jsf:pagecode language="java" location="/src/pagecode/FailureLog_e.java" --%><%-- /jsf:pagecode --%><%@page
	language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@taglib uri="http://www.ibm.com/jsf/html_extended" prefix="hx"%>

<f:subview id="failureLog">
	<body>
		<hx:scriptCollector id="scriptCollector1">
	
			<h:form styleClass="form" id="form1">
			<table border='0' width="650" align='center' width="650" cellspacing='0'
						cellpadding='0'>
				<tbody>
					<tr>
						<td>
						
						<table border='1' align='center' width="600" cellspacing='0' cellpadding='0'>
						<tr>
						<td align="justify">
						
						 This will fetch if there are any errors occured during these transactions.
						
						<ul>
							<li>PopulateInitValues</li>
							<li> GasPurchaseFailure</li>
							<li> GradeMaintFailure</li>
							<li> NewMemberFailure</li>
							<li> GasEODMaintFailure</li>
							<li>StoreEODMaintFailure</li>
							<li>StorePurchaseFailure. </li>
							
						</ul>
					 	</td>
						</tr>
						
						</table>
						<br>
						<br>
						<table border='1' align='center' width="600" cellspacing='0' cellpadding='0'>
					
						<tr>
						<td>
							<h2>To view errors, please click below</h2>
								<h:commandButton action="#{failure.displayFailures}" type="submit" value="View Errors" label="Submit"
										styleClass="commandButton" id="button1">
			
								</h:commandButton>
			
						<br>
						<br>
								<h:messages></h:messages>
						<h:dataTable value="#{failure.failureTableAL}"
						binding="#{failure.failureDataTable}" var="logger">
						<h:column>
							<f:facet name="header">
								<h:outputText value="Error ID" />
							</f:facet>
							<h:outputText value="#{logger.id}" />
						</h:column>
						<h:column>
							<f:facet name="header">
								<h:outputText value="Time Of Exception" />
							</f:facet>
							<h:outputText value="#{logger.timeofException}" />
						</h:column>
						<h:column>
							<f:facet name="header">
								<h:outputText value="Exception Message" />
							</f:facet>
							<h:outputText value="#{logger.exception}" />
						</h:column>
					</h:dataTable>
					
					</td>
					</tr>
					</table>

		</tbody>
		</table >
				<br><br>
			</h:form>
		</hx:scriptCollector>
	</body>
</f:subview>
