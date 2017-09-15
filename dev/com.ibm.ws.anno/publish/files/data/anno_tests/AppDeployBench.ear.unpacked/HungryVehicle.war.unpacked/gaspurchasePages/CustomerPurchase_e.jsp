<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%-- jsf:pagecode language="java" location="/src/pagecode/gaspurchasePages/CustomerPurchase_e.java" --%><%-- /jsf:pagecode --%><%@page
	language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@taglib uri="http://myfaces.apache.org/tomahawk" prefix="t"%>
<%@taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@taglib uri="http://www.ibm.com/jsf/html_extended" prefix="hx"%>

<f:subview id="custPurchase_view">
	<body>
	<hx:scriptCollector id="scriptCollector1">

		<t:panelTabbedPane styleClass="panelTabbedPane" id="custPurchaseTabbedPane" width="650"
											activeTabStyleClass="activeTab"
                                            inactiveTabStyleClass="inactiveTab"
                                            disabledTabStyleClass="disabledTab"
                                            activeSubStyleClass="activeSub"
                                            inactiveSubStyleClass="inactiveSub"
                                            tabContentStyleClass="tabContent"
                                            serverSideTabSwitch="false">

			<t:panelTab styleClass="panelTab" id="memberPurchaseTab" label="Network Member">
									<h:form id="existingCustomerForm">

												<h2>Returning Customer, Please Login</h2>
													<hr>
												<table border="0">
													<tbody>
														<tr height="20"></tr>
														<tr>
															<td width="323"><b>MemberID:</b></td>
															<td width="182"><h:selectOneMenu id="MemberID0"
																binding="#{login.memberIDSelectOne}" value="#{login.memberID}"
																valueChangeListener="#{login.memberValueChange}">
																<f:selectItem itemValue="Member1" itemLabel="Member1" />
																<f:selectItem itemValue="Member2" itemLabel="Member2" />
																<f:selectItem itemValue="Member3" itemLabel="Member3" />
																<f:selectItem itemValue="Member4" itemLabel="Member4" />
															</h:selectOneMenu></td>
														</tr>
														<tr>
															<td height="81" width="323"><b>Purchase Options:</b></td>
															<td height="81" width="182"><h:selectOneRadio
																id="PurchaseOption" required="true" value="#{login.purchaseOption}"
																valueChangeListener="#{login.purchaseValueChange}">
																<f:selectItem itemValue="GasPurchase"
																	itemLabel="GasPurchase" />
																<f:selectItem itemValue="StationStore"
																	itemLabel="StationStore" />
															</h:selectOneRadio></td>
														</tr>
														<tr>
															<td height="60" width="323">
																<h:commandButton
																id="submit" value="Submit Member"
																action="#{login.login}" /> 
																<h:commandButton id="reset"
																value="Reset values" action="#{login.resetValues}" />
															</td>
														</tr>

													</tbody>
												</table>

												<ul>
													<li>Please enter memberID in this Format = MemberX<br>
													</li>
												</ul>
											</h:form>
											
			</t:panelTab>
			<t:panelTab styleClass="panelTab" id="newMemberPurchaseTab" label=" New Member">
			
									<h:form id="NewMemberForm">

					<table border='0' width="600" cellspacing='0'
						cellpadding='0'>

						<tbody>
							<tr height="20"></tr>
							<tr height="20"></tr>
							<tr>
								<td width="587">
								<h2>New Customer, would you like to become member.</h2>
								</td>
							</tr>
							<tr height="20"></tr>
							<tr height="20"></tr>
						</tbody>
					</table>
					<table border='0' width="600" cellspacing='0'
						cellpadding='0'>
						<tbody>
							<tr>
								<td width="587">Please enter your credentials in next page,
								Please click on Submit New</td>
							</tr>
							<tr>
							<td>
							<br>
							</td>
							</tr>
							<tr>
								<td width="587"><h:commandButton id="submitNew"
									value="Submit New" action="NewMember" /> <br>
								<br>
								</td>
							</tr>

						</tbody>
					</table>
				</h:form>
					
			
			</t:panelTab>
			<t:panelTab styleClass="panelTab" id="nonMemberPurchaseTab" label="Non Member">
			
				<h:form id="nonmemberForm">
						<table border="0">
							<tbody>
							<tr height="20"></tr>
							<tr height="20"></tr>
								<tr>
									<td width="523">
										<h2>Do not want to become Member</h2>
									 	User can Purchase Gas , but will not be able to do Store purchases.
									</td>
								</tr>
								
								<tr>
									<td width="523">
									User can earn points while purchasing Gas, become Member
									</td>

								</tr>
								<tr>
							<td>
							<br>
							</td>
							</tr>
								<tr>
									<td width="523" height="22">
									<h:inputHidden id="MemberID1"
										value="Not Member"></h:inputHidden> 
									<h:inputHidden
										id="CCNumber1" value="5234567890423456"></h:inputHidden> 
									<h:commandButton
										id="submitNon" value="NonMember Purchase" action="#{login.login}"/> <br>
									<br>
									</td>
								</tr>
							</tbody>
						</table>
					</h:form>
			</t:panelTab>
			
		</t:panelTabbedPane>
	</hx:scriptCollector>
	</body>
</f:subview>