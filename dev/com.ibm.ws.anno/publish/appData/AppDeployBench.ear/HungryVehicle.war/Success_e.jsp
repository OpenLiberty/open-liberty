<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%-- jsf:pagecode language="java" location="/src/pagecode/Success_e.java" --%><%-- /jsf:pagecode --%><%@page
	language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@taglib uri="http://java.sun.com/jsf/core" prefix="f"%><%@taglib
	uri="http://www.ibm.com/jsf/BrowserFramework" prefix="odc"%><%@taglib
	uri="http://www.ibm.com/jsf/html_extended" prefix="hx"%><%@taglib
	uri="http://java.sun.com/jsf/html" prefix="h"%>

<f:subview id="success_view">
	<body>
	<hx:scriptCollector id="scriptCollector1">

		<h:form styleClass="form" id="form1">
			<odc:tabbedPanel slantInactiveRight="4" width="650"
				styleClass="tabbedPanel" height="625" slantActiveRight="4"
				showBackNextButton="false" id="tabbedPanel1" showTabs="true"
				variableTabLength="false">

				<odc:bfPanel id="bfpanel1" name="Success Page"
					showFinishCancelButton="false">


					<table border='0' align='center' width="650" cellspacing='0'
						cellpadding='0'>
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
										<td>
										<p align="justify">Your Transaction was <b>SUCCESSFUL!!</b>
										. Thank you for your purchase!<br>
										<h:messages></h:messages> <br><br>
										Your order number will be generated and sent to you in the mail<br>
										Please keep this order number for future reference.</p>
										</td>
									</tr>
								</table>
								<br>
								<table border="1" width="600" align="center" cellspacing="1"
									cellpadding="10">
									<tr>
										<td><strong><u>Note: </u></strong><br>
										<br>
										<p align="justify">If you have an Order Status or a Billing
										Question, <br>
										please e-mail at <a href='mailto:contact@gasnet.inc'>contact@gasnet.inc</a>.
										</p>
										<p align="justify">This purchase is a direct transaction
										between you as customer and this site <b>http://www.server.com/gasnet.inc</b>.
										</p>
										<p>However, the charges will be billed to your account as
										<strong> GASNET AVENUES PVT. LTD. </strong></p>
										</td>
									</tr>
								</table>

								<br>
								<br>

								

								<br>
								<br>
								</td>
							</tr>
						</tbody>
					</table>


					<br>

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