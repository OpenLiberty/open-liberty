<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%-- jsf:pagecode language="java" location="/src/pagecode/gaspurchasePages/InputMemberData_e.java" --%><%-- /jsf:pagecode --%><%@page
	language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@taglib uri="http://www.ibm.com/jsf/html_extended" prefix="hx"%>
<%@taglib uri="http://www.ibm.com/jsf/BrowserFramework" prefix="odc"%>

<f:subview id="inputNewMemberData_view">
	<body>
	<hx:scriptCollector id="scriptCollector1">

		<h:form id="newMemberForm">
			<odc:tabbedPanel slantInactiveRight="4" width="600"
				styleClass="tabbedPanel" height="600" slantActiveRight="4"
				showBackNextButton="true" id="newMemberDatatabbedPanel1" showTabs="true"
				variableTabLength="false">

				<odc:bfPanel id="newMemberDataTab" name="InputNewMemberData"
					showFinishCancelButton="false">


					
						<TABLE border="0">
							<TBODY>
								<TR>
									<TD	height="30">
									</TD>
								</TR>
								<TR>
									<TD width="262"><B>MemberFirstName </B></TD>
									<TD align="left" width="373"><h:inputText id="FirstName"
										value="#{newmember.firstName}" size="20" required="true">
										<f:validateLength minimum="2"></f:validateLength>
									</h:inputText> <h:message for="FirstName" showSummary="false"
										showDetail="true" /></TD>
								</TR>
								<TR>
									<TD width="262"><B>MemberLastName</B></TD>
									<TD align="left" width="373"><h:inputText id="LastName"
										required="true" value="#{newmember.lastName}"
										size="20">
									<f:validateLength minimum="2"></f:validateLength>
								</h:inputText> <h:message for="LastName" showSummary="false"
										showDetail="true" /></TD>
								</TR>
								<TR>
									<TD width="262"></TD>
									<TD align="left" width="373"></TD>
									<TD></TD>
								</TR>
								<TR>
									<TD width="262"><B>CreditCard Number</B></TD>
									<TD align="left" width="373"><h:selectOneMenu id="CCNO"
										required="true" value="#{newmember.ccno}"
										valueChangeListener="#{newmember.ccnoValueChange}"
										binding="#{newmember.ccnoSelectOne}">
										<f:selectItem itemValue="5234567890423456"
											itemLabel="5234567890423456" />
										<f:selectItem itemValue="4234567890123456"
											itemLabel="4234567890123456" />
										<f:selectItem itemValue="6234567890123456"
											itemLabel="6234567890123456" />
										<f:selectItem itemValue="5234567890123456"
											itemLabel="5234567890123456" />
									</h:selectOneMenu></TD>
								</TR>
								<TR>
									<TD width="262"></TD>
									<TD align="left" width="373"></TD>
								</TR>
								<TR>
									<TD width="262"><B>LocationJoined</B></TD>
									<TD align="left" width="373"><h:selectOneMenu
										id="locations" required="true" value="#{newmember.locations}"
										valueChangeListener="#{newmember.locationsValueChange}">
										<f:selectItem itemValue="RTP" itemLabel="RTP" />
										<f:selectItem itemValue="RAL" itemLabel="Raleigh" />
										<f:selectItem itemValue="DHM" itemLabel="Durham" />
										<f:selectItem itemValue="CRY" itemLabel="Cary" />
									</h:selectOneMenu></TD>
								</TR>
								<TR>
									<TD height="128" width="262"><B>DateJoined</B></TD>
									<TD height="128" align="left" width="373">
									<hx:inputMiniCalendar styleClass="inputMiniCalendar"
										id="miniCalendar1" valueChangeListener="#{newmember.calDateValueChange}"
										value="#{newmember.calDate}" required="true">
										
										<hx:convertDateTime type="date" pattern="MM/dd/yyyy" />
										
									</hx:inputMiniCalendar><h:message for="miniCalendar1"></h:message>
									
									</TD>

								</TR>
								<TR>
									<TD height="81" width="262"><B>Purchase Options</B></TD>
									<TD height="81" width="373"><h:selectOneRadio
										id="PurchaseOption" required="true" value="GasPurchase"
										valueChangeListener="#{newmember.purchaseValueChange}">
										<f:selectItem itemValue="GasPurchase" itemLabel="GasPurchase" />
										<f:selectItem itemValue="StationStore"
											itemLabel="StationStore" />
									</h:selectOneRadio></TD>
								</TR>
								<TR>
									<TD width="262" height="103">
									<h:commandButton
										id="submitNew" value="Submit New Member"
										action="#{newmember.register}" /></TD>
									<TD><INPUT type="reset" value="Reset value"></TD>
								</TR>
							</TBODY>
						</TABLE>

					


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
