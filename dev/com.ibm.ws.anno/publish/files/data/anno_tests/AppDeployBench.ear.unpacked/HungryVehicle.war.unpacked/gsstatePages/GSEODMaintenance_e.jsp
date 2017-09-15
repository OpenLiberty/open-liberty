<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%-- jsf:pagecode language="java" location="/src/pagecode/gsstatePages/GSEODMaintenance_e.java" --%><%-- /jsf:pagecode --%><%@page
	language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@taglib
	uri="http://java.sun.com/jsf/html" prefix="h"%>
	
<f:subview id="eod_maintenance">
	<body>
	<h:form styleClass="form" id="form1">
						<HR>
							
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


								<table border="1" width="600" align="center" cellspacing="1"
									cellpadding="10">
									<tr>
										<td>
										<p align="justify"><FONT SIZE="6"><BR>
							GasStation in EOD Maintenance</FONT> <br>
										Thank you for your interest in purchasing our products!<br>
										Please bear with us. GasStation will be open soon. <br>
										<br>
										<h:messages></h:messages> <br>
										<br>
										</p>
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

								<br>
								<br>
								</td>
							</tr>
						</tbody>
					</table>
							
							<HR>
							</h:form>
	</body>
</f:subview>
