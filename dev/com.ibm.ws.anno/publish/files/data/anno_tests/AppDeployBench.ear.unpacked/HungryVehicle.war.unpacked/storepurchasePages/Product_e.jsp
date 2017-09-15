<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%-- jsf:pagecode language="java" location="/src/pagecode/storepurchasePages/Product_e.java" --%><%-- /jsf:pagecode --%><%@page
	language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@taglib uri="http://www.ibm.com/jsf/html_extended" prefix="hx"%>
<%@taglib uri="http://www.ibm.com/jsf/BrowserFramework" prefix="odc"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<f:subview id="product">
	<body>
<hx:scriptCollector id="scriptCollector1">
	<h:form id="productForm">
		<odc:tabbedPanel slantInactiveRight="4" width="650"
				styleClass="tabbedPanel" height="600" slantActiveRight="4"
				showBackNextButton="false" id="productListtabbedPanel1"
				showTabs="true" variableTabLength="false">

				<odc:bfPanel id="productTab" name="Product Display"
					showFinishCancelButton="false">



				<!-- This page will show the details of a particular product.
		It will allow the user to Purchase a number of that product.
		When purchased, the page will then show the Shopping Cart page.
		If declined, the page will return to the previous page. (should
		be able to use javascript to do the showPage(-1) or whatever that is.
		
		When posted, the Quantity text field has the name of "Quantity"
		 -->

				<!-- 
		|  Name/ID  			|  Price  				|
		|  Quantity-Purchase    |  Quantity-Available	|
		|  Description  								| 100%width
		-->
				<hr>
				<h2>Product Page</h2>
				<p><strong>Welcome 
				<c:out value="${sessionScope.MemberID}"></c:out>. Here are the product
				details.</strong></p>
				<br>
				<br>

				

					<table width="100%" cellspacing="0" cellpadding="5" border="0">
						<tr>
							<td class="right halfwidth">
							<center><font class="small" color="000080"> <u>Name
							of Product/ID</u> </font></center>
							</td>
							<td>
							<center><font class="small" color="000080"> <u>Price</u>
							</font></center>
							</td>
						</tr>
						<tr>
							<td class="right halfwidth" height="50px">
							<center>
							<h:outputText value="#{product.type_id}"
								id="type_idOut" /> <h:message for="type_idOut"
								showSummary="false" showDetail="true" />
							</center>
							</td>
							<td height="50px">
							<center>
							<h:outputText value="#{product.price}"
								id="priceOut">
								<f:convertNumber maxFractionDigits="2" minFractionDigits="2"
									type="number" />
							</h:outputText>
							</center>
							</td>
						</tr>
						<tr>
							<td class="right top halfwidth" height="40px">
							<center><font class="small" color="000080"> <u>Quantity
							Desired:</u> </font>
							<h:inputText id="Quantity" styleClass="inputText"
								value="#{product.quantity}"></h:inputText>
							</center>
							</td>
							<td class="top">
							<center><font class="small" color="000080"> <u>Units
							Available</u> </font></center>
							</td>
						</tr>
						<tr>
							<td class="right halfwidth" height="40px">

							<center><h:commandButton type="submit"
								value="Add to Cart" id="button1" styleClass="commandButton"
								action="#{cart.addItem}"></h:commandButton></center>
							</td>
							<td class="halfwidth">
							<center><h:outputText id="quantav"
								value="#{product.quantAv}" /></center>
							</td>
						</tr>
						<tr>
							<td class="top" colspan="2">
							<center><font class="small" color="000080"> <u>Image</u>
							</font></center>
							</td>
						</tr>
						<tr>
							<td class="bottom" colspan="2">
							<center>
							
							
							<hx:graphicImageEx id="imageEx1"
								value="#{product.imageArray}" styleClass="graphicImageEx"
								mimeType="image/gif">
							</hx:graphicImageEx>
							
							</center>
							</td>
						</tr>
						<tr>
							<td class="top" colspan="2">
							<center><font class="small" color="000080"> <u>Description</u>
							</font></center>
							</td>
						</tr>
						<tr>
							<td class="bottom" colspan="2">
							<center><h:outputText id="typedesc"
								value="#{product.type_desc}" /></center>
							</td>
						</tr>
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