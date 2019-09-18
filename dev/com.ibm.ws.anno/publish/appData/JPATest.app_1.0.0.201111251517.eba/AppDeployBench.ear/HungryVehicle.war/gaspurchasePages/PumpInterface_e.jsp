<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%-- jsf:pagecode language="java" location="/src/pagecode/gaspurchasePages/PumpInterface_e.java" --%><%-- /jsf:pagecode --%><%@page
	language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@taglib uri="http://myfaces.apache.org/tomahawk" prefix="t"%>
<%@taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@taglib uri="http://www.ibm.com/jsf/html_extended" prefix="hx"%>

<f:subview id="pump_view">
	<body>
	<hx:scriptCollector id="scriptCollector1">

		<t:panelTabbedPane styleClass="panelTabbedPane" id="pumpTabs" width="650"
											activeTabStyleClass="activeTab"
                                            inactiveTabStyleClass="inactiveTab"
                                            disabledTabStyleClass="disabledTab"
                                            activeSubStyleClass="activeSub"
                                            inactiveSubStyleClass="inactiveSub"
                                            tabContentStyleClass="tabContent"
                                            serverSideTabSwitch="false">

			<t:panelTab styleClass="panelTab" id="pumpInterface" label="Pump Interface">

				<h:form id="memberGasPurchase">
					<h2>Please enter GasPurchase information:</h2>
					<br>

					<br>
					<br>
					<h:messages />

					<table border="0">
						<tbody>
							<tr>
								<td width="287"><b><font size="4">Member ID: </font></b></td>
								<td width="279"><h:inputText id="MemberID0"
									value="#{purchase.memberID}" /> <h:message for="MemberID0"
									showSummary="false" showDetail="true" /></td>
							</tr>
							<tr>
								<td width="287"><b><font size="4">CreditCard Number:</font></b></td>
								<td width="279"><h:inputText id="CCNumber0"
									value="#{purchase.ccnumber}"></h:inputText> <h:message
									for="CCNumber0" showSummary="false" showDetail="true" /></td>
							</tr>
							<tr>
								<td width="287"><b><font size="4">Pump Number:</font></b></td>
								<td width="279"><h:selectOneMenu id="pumpNum"
									required="true" value="#{purchase.pumpNum}"
									valueChangeListener="#{purchase.pumpValueChange}"
									validator="#{purchase.validatePumpNum}">
									<f:selectItem value="#{purchase.pumpNum1}" />
									<f:selectItem value="#{purchase.pumpNum2}" />
									<f:selectItem value="#{purchase.pumpNum3}" />
									<f:selectItem value="#{purchase.pumpNum4}" />
									<f:selectItem value="#{purchase.pumpNum5}" />
								</h:selectOneMenu> <h:message for="pumpNum" showSummary="false" showDetail="true" />
								</td>
							</tr>
							<tr>
								<td width="287"><b><font size="4">Grade Number:</font></b></td>
								<td width="279"><h:selectOneMenu id="gradeNum"
									required="true" value="#{purchase.gradeNumber}"
									valueChangeListener="#{purchase.gradeValueChange}">
									<f:selectItems value="#{applicationScope.gradesAL}" />

								</h:selectOneMenu> <h:message for="gradeNum" showSummary="false" showDetail="true" />
								</td>
							</tr>
							<tr>
								<td width="287"><b><font size="4">Quantity
								Purchased:</font></b></td>
								<td width="279"><h:selectOneMenu id="gasQuant"
									required="true" value="#{purchase.gasQuant}"
									valueChangeListener="#{purchase.gasquantValueChange}"
									binding="#{purchase.quantSelectOne}">
									<f:selectItem binding="#{purchase.gasQuant1}" />
									<f:selectItem binding="#{purchase.gasQuant2}" />
									<f:selectItem binding="#{purchase.gasQuant5}" />
									<f:selectItem binding="#{purchase.gasQuant10}" />
									<f:selectItem binding="#{purchase.gasQuant20}" />
									<f:selectItem binding="#{purchase.gasQuant30}" />
									<f:selectItem binding="#{purchase.gasQuant200}" />
								</h:selectOneMenu> 
								
								<h:message for="gasQuant" showSummary="false" showDetail="true" />
								</td>
							</tr>
						</tbody>
					</table>
					<br>
					<br>
					<h:commandButton id="SubmitPurchase" value="Submit Purchase"
						action="#{purchase.purchaseGas}" />
					<br>
					<br>

					<hr>
					<br>
					<br>
					<ul>
						<li><font size="4">Choose GradeNumber to purchase</font></li>
						<li><font size="4">Choose Quantity to purchase</font></li>
						<li><font size="4">Customer can enter new CCNo, if
						want to change the CCNo</font></li>
					</ul>
					
				</h:form>
				<hr>

			</t:panelTab>

		</t:panelTabbedPane>
	</hx:scriptCollector>
	</body>
</f:subview>