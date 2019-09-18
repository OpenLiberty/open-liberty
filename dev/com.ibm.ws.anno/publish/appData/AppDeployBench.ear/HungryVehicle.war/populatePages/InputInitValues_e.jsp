<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%-- jsf:pagecode language="java" location="/src/pagecode/populatePages/InputInitValues_e.java" --%><%-- /jsf:pagecode --%><%@page
	language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@taglib uri="http://myfaces.apache.org/tomahawk" prefix="t"%>
<%@taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@taglib uri="http://www.ibm.com/jsf/html_extended" prefix="hx"%>

<f:subview id="inputInitValues_view">
	<body>
	<t:panelTabbedPane styleClass="panelTabbedPane" id="initValuetabbedPane1">
		<t:panelTab styleClass="panelTab" id="initValuetab" label="Input Initial Values">
		
		<h:form id="StoreInitParameters">
								
								<hr>
								<h2>Fill in the required information :<font size="3"
									color="#0000CD"><br>

								</font> <br>
								<br>
								</h2>
								<div align="left"><strong><u>Choose
								GasStation you need to fill gas from.</u></strong> <br>
								<br>
								<table border="0">
									<tbody>
										<tr>
											<td width="214"><font size="4">GasStation Number:
											</font></td>
											<td width="99"><h:selectOneMenu id="GasStationNumber"
												required="true" value="#{populate.gsNumber}"
												valueChangeListener="#{populate.gsNumberValueChange}">
												<f:selectItem itemValue="1" itemLabel="GS-1@NC" />
												<f:selectItem itemValue="2" itemLabel="GS-2@NC" />
												<f:selectItem itemValue="3" itemLabel="GS-3@NC" />
												<f:selectItem itemValue="4" itemLabel="GS-4@NC" />
											</h:selectOneMenu> <h:message for="NetworkStorage" showSummary="false"
												showDetail="true" /> <h:message for="GasStationNumber"
												showSummary="false" showDetail="true" /></td>
											<td width="299">choose GasStation Number</td>
										</tr>
									</tbody>
								</table>
								</div>
								<br>
								<hr>
								<p><br>
								<strong><u>Fill in requested properties, to start
								the test.</u></strong><br>
								<br>
								</p>
								<p></p>
								<div align="left">
								<table border="0">
									<tbody>
										<tr>
											<td align="left" width="215"><font size="4">Grade
											Quantity: </font></td>
											<td width="60"><h:inputText id="param1"
												value="#{populate.gradeQuant}" size="5" required="true"
												valueChangeListener="#{populate.gradeQuantValueChange}" />
											<h:message for="param1" showSummary="false" showDetail="true" />
											</td>
											<td>Quantity for each Grade</td>
										</tr>
										<tr>
											<td align="left" width="215"><font size="4">Store
											ProductType Quantity: </font></td>
											<td width="60"><h:inputText id="param4"
												value="#{populate.productQuant}" size="5" required="true"
												valueChangeListener="#{populate.productQuantValueChange}" />
											<h:message for="param4" showSummary="false" showDetail="true" />
											</td>
											<td>Quantity for each Product Type</td>
										</tr>
										<tr align="left">
											<td align="left" width="215"><font size="4">EOD
											Number: </font></td>
											<td width="60"><h:inputText id="param2"
												value="#{populate.eodNumber}" size="5" required="true"
												valueChangeListener="#{populate.eodNumberValueChange}" /> <h:message
												for="param2" showSummary="false" showDetail="true" /></td>
											<td>number of total Grade message transactions in a day</td>
										</tr>
										<tr>
											<td align="left" width="215"><font size="4">TranEnd
											Number: </font></td>
											<td width="60"><h:inputText id="param3"
												value="#{populate.tranEndNumber}" size="5" required="true"
												valueChangeListener="#{populate.tranEndNumberValueChange}" />
											<h:message for="param3" showSummary="false" showDetail="true" />
											</td>
											<td>number of total day message transactions in the whole test</td>
										</tr>

									</tbody>
								</table>
								<br>
								</div>
								<br>
								<hr>
								<br>
								<strong><u>Choose Network storage for GasStation
								Account.</u></strong>
								<br>
								<br>
								<div align="left">

								<table border="0">
									<tbody>
										<tr align="left">
											<td align="left" width="215"><font size="4">CICS
											: </font></td>
											<td width="90"><h:selectOneMenu id="NetworkStorage"
												required="true" value="#{populate.cicsDecision}"
												valueChangeListener="#{populate.cicsDecisionValueChange}">
												<f:selectItem itemValue="TRUE" itemLabel="YES" />
												<f:selectItem itemValue="FALSE" itemLabel="NO" />
											</h:selectOneMenu> <h:message for="NetworkStorage" showSummary="false"
												showDetail="true" /></td>
											<td width="320">Yes will choose CICS, No will choose a
											RDBMS</td>
										</tr>
									</tbody>
								</table>
								<br>
								<br>
								<br>
								<h:messages></h:messages> <h:commandButton id="Properties"
									value="Submit Properties" action="#{populate.storeInitValues}" /></div>
								<hr>
								<ul>
									<li>GasStationNumber: Number of GasStation registered to
									GasNetwork</li>
									<li>&nbsp;GradeQuantity :GasStation should process the
									number of transactions for the Grade before purchasing more Gas
									for the Grade from Network.</li>
									<li>&nbsp;Store ProductType Quantity :GasStation should
									process the number of transactions for the product.</li>
									<li>&nbsp;EODNumber: This will define number of
									GradeQuantity iterations, and is a logical EOD</li>
									<li>&nbsp;TranEndNumber: This will define certain number
									of EODNumber iterations , GasStation should close and stop
									selling</li>
								</ul>
								<hr>
								<p><font size="2">@File:InputInitialValues.jsp</font></p>
								</h:form>
		</t:panelTab>
	</t:panelTabbedPane>
	</body>
</f:subview>