<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%-- jsf:pagecode language="java" location="/src/pagecode/storepurchasePages/OrderPage_e.java" --%><%-- /jsf:pagecode --%><%@page
	language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@taglib uri="http://www.ibm.com/jsf/html_extended" prefix="hx"%>
<%@taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="http://www.ibm.com/jsf/BrowserFramework" prefix="odc"%>

<f:subview id="order_view">
	<body>

	<hx:scriptCollector id="scriptCollector1">
	<h:form styleClass="form" id="order_outerform1">
			<odc:tabbedPanel slantInactiveRight="4" width="660"
				styleClass="tabbedPanel" height="600" slantActiveRight="4"
				showBackNextButton="true" id="ordertabbedPanel1" showTabs="true"
				variableTabLength="false">
				<odc:bfPanel id="orderTab" name="OrderDisplay"
					showFinishCancelButton="false">

				<table border='0' align='center' width="650" cellspacing='0'cellpadding='0'>
				 <tbody>
				   <tr>
				     <td>
						<table border='0' align='center' width="600" cellspacing='0'
							cellpadding='0'>
							<tr>
								<td align="center">
								<center>
								<h1>gasnet.inc</h1>
								</center>
								</td>
							</tr>
						</table>

						<br>
						<br>
						<table border="0" width="600" align="center" cellspacing="1"
									cellpadding="10">
									<tr>
										<td align="center" valign="middle">Please proceed back to
										Purchase <h:commandButton action="#{purchase.killHttpSession}"
											value="Finish and Logout" id="endSession"></h:commandButton></td>
									</tr>

						</table>
						<table border="1" width="600" align="center" cellspacing="1"
							cellpadding="10">
							<tr>
								<td >

					
										<h2>Order Success !! Order will be shipped soon.</h2>

										<p align="justify">Your Transaction was <b>SUCCESSFUL!!</b>
										. Thank you for your purchase!<br>
										<h:messages></h:messages> <br><br>
										Your order number is generated and will also be sent to you in the mail, if a valid email address has been provided.<br>
										Please keep this order number for future reference.</p>
						
								</td>
							</tr>
						</table>
						<br>
						<table border="1" width="600" align="center" cellspacing="1" cellpadding="10">
							<tr>
								<td>				
					
										<h2>Here is the Sale summary:</h2>
					
										<table cellspacing="2" cellpadding="2">
										<tbody>
											<tr>
												<td>Member No : <c:out value="${sessionScope.MemberID}"></c:out><br>
												</td>
											</tr>
											<tr>
												<td>Sale ID : <h:outputText
														styleClass="outputText" id="text8" value="#{cart.saleNum}"></h:outputText>
												<br>
												</td>
											</tr>
											<tr>
												<td>Total Cart Amount : <h:outputText id="text2" styleClass="outputText"
																			value="#{cart.totalCartAmount}">

															<f:convertNumber maxFractionDigits="2" minFractionDigits="2" type="number" />
																	</h:outputText>
												</td>
											</tr>

											<tr>
												<td>
													Purchased Products List :
												</td>
											</tr>
										</tbody>
										</table>
					


										<h:dataTable border="0" cellpadding="2" cellspacing="0"
												columnClasses="columnClass1" headerClass="headerClass"
												footerClass="footerClass" rowClasses="rowClass1, rowClass2"
												id="table1" styleClass="dataTable" value="#{cart.orderAL}"
												var="order">
											<h:column id="column1">
													<f:facet name="header">
														<h:outputText id="text1" styleClass="outputText" value="Name/ID"></h:outputText>
													</f:facet>
													<h:outputText id="text4" styleClass="outputText"
																	value="#{order.type_id}"></h:outputText>
											</h:column>
											<h:column id="column2">
													<f:facet name="header">
														<h:outputText id="text7" styleClass="outputText"
																	value="Quantity"></h:outputText>
													</f:facet>
														<h:outputText id="text5" styleClass="outputText"
																	value="#{order.quant_purchased}"></h:outputText>
											</h:column>
											<h:column id="column3">
													<f:facet name="header">
														<h:outputText id="text3" styleClass="outputText" value="Amount"></h:outputText>
													</f:facet>
														<h:outputText id="text6" styleClass="outputText"
																	value="#{order.itemAmount}">
																	<f:convertNumber maxFractionDigits="2" minFractionDigits="2" type="number" />
														</h:outputText>
											</h:column>
										</h:dataTable>
									<br>
									<br>
								</td>
							</tr>
						</table>

				 </td>
				</tr>
				</tbody>
				</table>
				
					

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