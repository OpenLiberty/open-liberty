<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<%-- jsf:pagecode language="java" location="/src/pagecode/SiteMap_e.java" --%><%-- /jsf:pagecode --%><%@page
	language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@taglib uri="http://java.sun.com/jsf/core" prefix="f"%><%@taglib
	uri="http://www.ibm.com/jsf/BrowserFramework" prefix="odc"%><%@taglib
	uri="http://www.ibm.com/jsf/html_extended" prefix="hx"%><%@taglib
	uri="http://java.sun.com/jsf/html" prefix="h"%>



<f:subview id="sitemap">
	<body>
	<hx:scriptCollector id="scriptCollector1">

<br>
		<h:form styleClass="form" id="form1">
			<odc:tabbedPanel slantInactiveRight="4" width="710"
				styleClass="tabbedPanel" height="700" slantActiveRight="4"
				showBackNextButton="false" id="tabbedPanel1" showTabs="true"
				variableTabLength="false">
				<odc:bfPanel id="bfpanel1" name="Site Map"
					showFinishCancelButton="false">


					<table border='0' align='center' width="650" cellspacing='0'
						cellpadding='10'>
						<tbody>
							<tr>
								<td><br>
								<br>


								<table border="0" width="600" align="center" cellspacing="0"
									cellpadding="0">
									<tr>
										<td>
										<p align=justify>
											<b>SiteMap:</b> List of site pages : <br>
										Each of the pages has an included page as {page_name}_e.jsp in the application.</p>
										</td>
									</tr>
								</table>
								<br>





								<br>

								

								<table border="1" width="600" align="center" cellspacing="1"
									cellpadding="10">
									<tr>
										<td><b>Home:</b>
										<ul>
											<li><a href="<%=request.getContextPath()%>/faces/GasStation.jsp">GasStation.jsp</a></li>
										</ul>
<hr>
										<b>Populate:</b>

										<ul>
											<li><a href="<%=request.getContextPath()%>/faces/populatePages/PopulateSelection.jsp">PopulateSelection.jsp</a>
											<ul>
												<li>InputInitValues.jsp</li>
												<li>PopulateStationGrade.jsp</li>
												<li>PopulateStationMember.jsp</li>
												<li>PopulateProductCat.jsp</li>
											</ul>
											</li>
										</ul>
<hr>

										<b>CustomerPurchase:</b>
										<ul>
											<li>
											<b>Member : </b>
											<a href="<%=request.getContextPath()%>/faces/gaspurchasePages/CustomerPurchase.jsp">CustomerPurchase.jsp</a>
											
											<ul>
												<li>GasPurchase
												<ul>
													<li>PumpInterface.jsp</li>
													<li>Success.jsp</li>
												</ul>
												</li>

											</ul>
											<ul>
												<li>StorePurchase
												<ul>
													<li>ProductList.jsp</li>
													<li>ProductTypeList.jsp</li>
													<li>Product.jsp</li>
													<li>ShoppingCartPage.jsp</li>
													<li>Order.jsp</li>
												</ul>
												</li>
											</ul>
											</li>
										</ul>
										
										<ul>
											<li>
												<b>NewMember : </b>
												<a href="<%=request.getContextPath()%>/faces/gaspurchasePages/CustomerPurchase.jsp">CustomerPurchase.jsp</a>
											
												<ul>
													<li>InputMemberData.jsp
													<ul>
														<li>GasPurchase
															<ul>
																<li>PumpInterface.jsp</li>
																<li>Success.jsp</li>
															</ul>
														</li>												
													</ul>
													
													<ul>
														<li>StorePurchase
															<ul>
																<li>ProductList.jsp</li>
																<li>ProductTypeList.jsp</li>
																<li>Product.jsp</li>
																<li>ShoppingCartPage.jsp</li>
																<li>Order.jsp</li>
														
															</ul>
														</li>
													</ul>
													</li>
												</ul>
											</li>
										</ul>
										
										
										<ul>
											<li>
												<b>NonMember : </b>
												<a href="<%=request.getContextPath()%>/faces/gaspurchasePages/CustomerPurchase.jsp">CustomerPurchase.jsp</a>
											
												<ul>
													<li>GasPurchase
														<ul>
															<li>PumpInterface.jsp</li>
															<li>Success.jsp</li>
														</ul>
													</li>

												</ul>
											</li>
										</ul>
<hr>
										<b>AboutUs:</b>
										<ul>
											<li><a href="<%=request.getContextPath()%>/faces/aboutusPages/AboutUs.jsp">AboutUs.jsp</a></li>
										</ul>
<hr>
										<b>StationLocator:</b>
										<ul>
											<li><a href="<%=request.getContextPath()%>/faces/stlocatorPages/StationLocator.jsp">StationLocator.jsp</a></li>
										</ul>
<hr>

										<b>JCAViolation:</b>
										<ul>
											<li><a href="<%=request.getContextPath()%>/faces/JCAViolationDetection.jsp">JCAViolationDetection.jsp</a></li>
										</ul>
<hr>
										<b>Other Pages:</b>
										<ul>
											<li>ErrorPage.jsp</li>
											<li>GSMaintenance.jsp</li>
											<li>GSEODMaintenance.jsp</li>
											<li>GSClose.jsp</li>
											<li>InvalidMember.jsp</li>
											<li>CartNotStarted.jsp</li>
										</ul>

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