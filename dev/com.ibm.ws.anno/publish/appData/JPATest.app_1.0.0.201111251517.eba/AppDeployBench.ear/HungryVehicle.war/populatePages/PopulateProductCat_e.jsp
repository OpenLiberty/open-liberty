<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%-- jsf:pagecode language="java" location="/src/pagecode/populatePages/PopulateProductCat_e.java" --%><%-- /jsf:pagecode --%><%@page
	language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@taglib uri="http://myfaces.apache.org/tomahawk" prefix="t"%>
<%@taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@taglib uri="http://www.ibm.com/jsf/html_extended" prefix="hx"%>

<f:subview id="populateCat">
	<body>
	<t:panelTabbedPane styleClass="panelTabbedPane" id="tabbedPane1">
		<t:panelTab styleClass="panelTab" id="group1" label="Populate Product Categories">
		
			<h2>Populated product categories, click on Populate.</h2>
				<h:form id="PopulateProductCatForm">
								<HR>
								<BR>
								<BR>
								<BR>
								<BR>
								<BR>
								<TABLE border="0">
									<TBODY>
										<TR align="left">
											<TD align="left" width="257"><FONT size="4">
											Start product Number: </FONT></TD>
											<TD width="60"><h:inputText id="param0"
												value="#{populate.startProductNumber}" size="10"
												required="true" /></TD>
											<TD>Starting number of products to populate</TD>
										</TR>
										<TR align="left">
											<TD align="left" width="257"><FONT size="4">Number
											of Products: </FONT></TD>
											<TD width="60"><h:inputText id="param1"
												value="#{populate.numberOfProducts}" size="10" required="true" />
											</TD>
											<TD>Insert number of products</TD>
										</TR>
										<tr>
											<td></td>
										</tr>
									</TBODY>
								</TABLE>
								<h:messages></h:messages>
								<BR>
								<BR>
								<BR>
								<h:commandButton id="Populate" value="Submit Populate"
									action="#{populate.populate_products}" />
								<BR>
								<BR>
								<BR>
								<HR>
				<br>
				<br>
				<h:messages></h:messages>
							</h:form>
		
		</t:panelTab>
	</t:panelTabbedPane>
	</body>
</f:subview>