<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%-- jsf:pagecode language="java" location="/src/pagecode/populatePages/PopulateStationMember_e.java" --%><%-- /jsf:pagecode --%><%@page
	language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@taglib uri="http://myfaces.apache.org/tomahawk" prefix="t"%>
<%@taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@taglib uri="http://www.ibm.com/jsf/html_extended" prefix="hx"%>

<f:subview id="populateMember">
	<body>
	<t:panelTabbedPane styleClass="panelTabbedPane" id="tabbedPane1">
		<t:panelTab styleClass="panelTab" id="group1" label="Populate Station Members">
			<h:form id="PopulateStationMemberForm">
									<HR>
									<BR>
									<BR>
									<b>Note:</b>Populate using this page, if you have chosen
									"not" to send list from Network while Registering Station.
									<BR>
									<BR>
									<BR>
									<h2>Populated for Members, click on Populate.</h2>
									<BR>
									<BR>
									<BR>
									<TABLE border="0">
										<TBODY>
											<TR align="left">
												<TD align="left" width="257"><FONT size="4">Start
												Member Number: </FONT></TD>
												<TD width="60"><h:inputText id="param0"
													value="#{populate.startMemberNumber}" size="10"
													required="true" /></TD>
												<TD>Starting number of members to populate</TD>
											</TR>
											<TR align="left">
												<TD align="left" width="257"><FONT size="4">Number
												of Members: </FONT></TD>
												<TD width="60"><h:inputText id="param1"
													value="#{populate.numberOfMembers}" size="10"
													required="true" /></TD>
												<TD>Insert number of members</TD>
											</TR>
										</TBODY>
									</TABLE>
									<BR>
									<BR>
									<BR>
	
									<h:commandButton id="PopulateMemb" value="Submit Populate"
										action="#{populate.populate_stationMembers}" />
	
									<BR>
									<BR>
									<BR>
									<HR>
				</h:form>
		</t:panelTab>
	</t:panelTabbedPane>
	</body>
</f:subview>