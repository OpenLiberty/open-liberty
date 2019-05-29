<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%-- jsf:pagecode language="java" location="/src/pagecode/aboutusPages/FuelType_Display_e.java" --%><%-- /jsf:pagecode --%><%@page
	language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@taglib uri="http://myfaces.apache.org/tomahawk" prefix="t"%>

<%@taglib uri="http://www.ibm.com/jsf/html_extended" prefix="hx"%>
<%@ page import="com.sun.rowset.CachedRowSetImpl" %>
<f:subview id="fuelType">
	<body>
<hx:scriptCollector id="scriptCollector1">
<t:panelTabbedPane styleClass="panelTabbedPane" id="ftTabbedPane" width="650"
					activeTabStyleClass="activeTab"
                                            inactiveTabStyleClass="inactiveTab"
                                            disabledTabStyleClass="disabledTab"
                                            activeSubStyleClass="activeSub"
                                            inactiveSubStyleClass="inactiveSub"
                                            tabContentStyleClass="tabContent"
                                            serverSideTabSwitch="false">

			
					<t:panelTab styleClass="panelTab" id="fuelTypeTab" label="Fuel Type List">
						<h:form id="FuelTypeForm1">
							<h2>List of Fuel Types Stations provide:</h2>
<br>
							<table border="1" width="400px" align="center" >
								<tbody>
									<tr bgcolor="#EDF6FC">
										<th>
											FTYPE_NO
										</th>
										<th>
											FTYPE_NAME
										</th>
									</tr>
	<%
	CachedRowSetImpl cacherowset =  (CachedRowSetImpl)request.getAttribute("resultdata");

	while (cacherowset.next()) {
        // System.out.println("FTYPE_NO: " + cacherowset.getInt(1) + ",  FTYPE_NAME: " + cacherowset.getString(2));
            
	%>
			<tr bgcolor="#EDF6FC" align="center">
				<td><%=cacherowset.getInt(1) %></td>
				<td><%=cacherowset.getString(2) %></td>
			</tr>

		
<%
	}
 %>
 		</tbody>
	</table>
</h:form>
					</t:panelTab>
			
				</t:panelTabbedPane>
				</hx:scriptCollector>

	</body>
</f:subview>