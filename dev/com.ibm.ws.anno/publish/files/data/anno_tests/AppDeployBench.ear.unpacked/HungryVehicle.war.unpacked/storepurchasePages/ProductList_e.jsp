<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%-- jsf:pagecode language="java" location="/src/pagecode/storepurchasePages/ProductList_e.java" --%><%-- /jsf:pagecode --%><%@page
	language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@taglib uri="http://www.ibm.com/jsf/html_extended" prefix="hx"%>
<%@taglib uri="http://www.ibm.com/jsf/BrowserFramework" prefix="odc"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>


<f:subview id="productList">
	<body>

<hx:scriptCollector id="scriptCollector1">
	<h:form id="productCategoryForm">
			<odc:tabbedPanel slantInactiveRight="4" width="650"
				styleClass="tabbedPanel" height="600" slantActiveRight="4"
				showBackNextButton="false" id="productListtabbedPanel1"
				showTabs="true" variableTabLength="false">

				<odc:bfPanel id="productListTab" name="Product List Display"
					showFinishCancelButton="false">

				<table border="0" align="center" width="650" cellspacing='0'cellpadding='0'>
				 <tbody>
				   <tr>
				     <td>
						<table border='0' align='center' width="600" cellspacing='0'
							cellpadding='0'>
							<tr>
								<td align="center">
								<center>
								<h2>Product Categories</h2>
								</center>
								</td>
							</tr>
						</table>

						<br>
						
					<table border="1" width="600" align="center" cellspacing="0"
							cellpadding="10">
						<tr>
						<td >
					
					

					<p> <b>Welcome <h:outputText styleClass="outputText" id="text1"
						value="#{sessionScope.MemberID}"></h:outputText></b></p>
						<p> <b>Please choose the product.
					User can choose only one product at one time</b></p>
					<br>
					<b>
					
					<h:dataTable value="#{productList.productCategoryAL}"
						binding="#{productTypeList.dataTable}" var="productCat" rows="20" id="prodListTable">
						<f:facet name="footer">
							<hx:panelBox styleClass="panelBox" id="box1">
								<hx:pagerWeb styleClass="pagerWeb" id="web1" />
							</hx:panelBox>
						</f:facet>

						<h:column>
							<f:facet name="header">
								<h:outputText value="ID" />
							</f:facet>
							<h:outputText value="#{productCat.productID }" />
						</h:column>
						<h:column>
							<f:facet name="header">
								<h:outputText value="Name" />
							</f:facet>
							<h:outputText value="#{productCat.productName }" />
						</h:column>
						<h:column>
							<f:facet name="header">
								<h:outputText value="Command" />
							</f:facet>
							<h:commandButton id="button" value="Show product list for #{productCat.productName}"
								action="#{productTypeList.getProductTypeList}" />

						</h:column>
					</h:dataTable>
					</b>
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