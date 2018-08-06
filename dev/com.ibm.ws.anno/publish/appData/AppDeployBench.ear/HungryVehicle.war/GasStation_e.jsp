<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%-- jsf:pagecode language="java" location="/src/pagecode/GasStation_e.java" --%><%-- /jsf:pagecode --%><%@page
	language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@taglib uri="http://myfaces.apache.org/tomahawk" prefix="t"%>

<%@taglib uri="http://www.ibm.com/jsf/html_extended" prefix="hx"%>


<f:subview id="gasStation_view">
	<body>
	<table border='0' align='center' width="690" cellspacing='0'cellpadding='0'>
		<tbody>
			<tr>
				<td height="20"></td>
			</tr>
			<tr>
			 <td>
				<table border='1' width="680" cellspacing='0'
					cellpadding='0'>
					<tr bgcolor="#EDF6FC">
						<th align="center" >
						<br>
						<h2>Welcome to GasStation application site</h2>

						</th>
					</tr>
				</table>
			</td>
			</tr>
			<tr>
			 <td>

				
				<br>

				
				<hx:scriptCollector id="scriptCollector1">

				<t:panelTabbedPane styleClass="panelTabbedPane" id="stTabbedPane" width="650"
											activeTabStyleClass="activeTab"
                                            inactiveTabStyleClass="inactiveTab"
                                            disabledTabStyleClass="disabledTab"
                                            activeSubStyleClass="activeSub"
                                            inactiveSubStyleClass="inactiveSub"
                                            tabContentStyleClass="tabContent"
                                            serverSideTabSwitch="false">

			
					<t:panelTab styleClass="panelTab" id="StationTab2" label="High Level Execution Steps">
						<h:form id="GasStationForm1">
							
							
							<table border="0" width="650" align="center" cellspacing="0"cellpadding="10">
								
								<tr>
									<td>
										<h2>High Level Execution Steps</h2>
									
								
							
										<c:forEach items="#{asu.homeSteps}" var="current" varStatus="status">
											<p>
											<h:outputText styleClass="outputText" value="#{current}" />
											</p>
										</c:forEach>
                                                                                
									</td>
								</tr>
							</table>
							<br>

						
						</h:form>
					</t:panelTab>
					<t:panelTab styleClass="panelTab" id="StationTab1" label="Station Services Provided">
						<h:form id="GasStationForm2">

							<table border='0' align='center' width="650" cellspacing='0'cellpadding='0'>
								<tbody>
									<tr>
										<td>
										
										
												<table>
												<tr>
													<td>
													<h2>
														Services Provided<br>
													</h2>
													</td>
												</tr>
												
												<tr bgcolor="#EDF6FC">
														
													<th> Top Class Fuel Products | </th>
													<th> Store Products | </th>
													<th> Car Wash | </th>
									
													
												</tr>
												<tr>
													<td height="40"></td>
												</tr>
												<tr>
													<td height="40"></td>
												</tr>
												<tr>
													<td height="40"></td>
												</tr>

												</table>
												
										</td>
									</tr>
								</tbody>
							</table>
						</h:form>
					</t:panelTab>
			
				</t:panelTabbedPane>
				</hx:scriptCollector>


				<br>

			</td>					
			</tr>

		</tbody>
	</table>


	</body>
</f:subview>
