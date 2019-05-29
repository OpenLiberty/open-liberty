<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%-- jsf:pagecode language="java" location="/src/pagecode/storepurchasePages/ShoppingCartPage_e.java" --%><%-- /jsf:pagecode --%><%@page
	language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@taglib uri="http://www.ibm.com/jsf/html_extended" prefix="hx"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="http://myfaces.apache.org/tomahawk" prefix="t"%>

<f:subview id="shoppingcart_view">
	<body>
	<hx:scriptCollector id="scriptCollector1">

		<t:panelTabbedPane styleClass="panelTabbedPane" id="shoppingCarttabbedPane1"width="680"
											activeTabStyleClass="activeTab"
                                            inactiveTabStyleClass="inactiveTab"
                                            disabledTabStyleClass="disabledTab"
                                            activeSubStyleClass="activeSub"
                                            inactiveSubStyleClass="inactiveSub"
                                            tabContentStyleClass="tabContent"
                                            serverSideTabSwitch="false">
			<t:panelTab styleClass="panelTab" id="cartTab" label="Shopping Cart Page">
			
			
		
		<br>
		<h2>Shopping Cart : Product List</h2>
		<br>
		
		<p><strong>Hi <h:outputText id="text1" styleClass="outputText" value="#{cart.memberID}"></h:outputText>. Here is the shopping cart.</strong>
		<br>
		<br>

		<h:form id="cartForm">
			<h:dataTable border="0" cellpadding="2" cellspacing="0"
				columnClasses="columnClass1" headerClass="headerClass"
				footerClass="footerClass" rowClasses="rowClass1, rowClass2"
				id="table1" styleClass="dataTable" value="#{cart.itemAL}" binding="#{cart.dataTable}" var="item">
				<h:column id="column1">
					<f:facet name="header">
						<h:outputText id="text2" styleClass="outputText" value="Name/ID"></h:outputText>
					</f:facet>
					<h:outputText id="text8" styleClass="outputText" value="#{item.id}"></h:outputText>
				</h:column>
				<h:column id="columnEx1">
					<f:facet name="header">
						<h:outputText id="text3" styleClass="outputText"
							value="Price per Unit"></h:outputText>
					</f:facet>
					<h:outputText id="text9" styleClass="outputText" value="#{item.price}">
						<f:convertNumber maxFractionDigits="2" minFractionDigits="2" type="number"/>
					</h:outputText>
				</h:column>
				<h:column id="columnEx2">
					<f:facet name="header">
						<h:outputText id="text4" styleClass="outputText" value="Quantity"></h:outputText>
					</f:facet>
					<h:inputText id="text10" styleClass="inputText" value="#{item.quantityPurchased}"></h:inputText>
				</h:column>
				<h:column id="columnEx3">
					<f:facet name="header">
						<h:outputText id="text5" styleClass="outputText" value="Amount"></h:outputText>
					</f:facet>
					<h:outputText id="text11" styleClass="outputText" value="#{item.price * item.quantityPurchased}">
						<f:convertNumber maxFractionDigits="2" minFractionDigits="2" type="number"/>
					</h:outputText>
				</h:column>
				<h:column id="columnEx4">
					<f:facet name="header">
						<h:outputText id="text6" styleClass="outputText"></h:outputText>
					</f:facet>
					<h:commandButton type="submit" value="Update Cart" id="button1"
						styleClass="commandButton" action="#{cart.updateCart}"></h:commandButton>
				</h:column>
				<h:column id="columnEx5">
					<f:facet name="header">
						<h:outputText id="text7" styleClass="outputText"></h:outputText>
					</f:facet>
					<h:commandButton type="submit" value="Remove Item" id="button2"
						styleClass="commandButton" action="#{cart.removeItem}"></h:commandButton>
				</h:column>
			</h:dataTable>
		</h:form>
		
		<h:form id="shoppingForm">
		<font size="4"><strong>Number of Items in Cart =</strong></font><h:outputText
				id="text12" styleClass="outputText" value="#{cart.numItemsInCart}">
					</h:outputText><br>
		<font size="4"><strong>Total Cart Amount = $</strong></font>
			<h:outputText id="text13" styleClass="outputText"
				value="#{cart.totalCartAmount}">
				<f:convertNumber maxFractionDigits="2" minFractionDigits="2" type="number"/>
			</h:outputText><br><br>
			<h:commandButton type="submit" value="Check Out" id="button3"
				styleClass="commandButton" action="#{cart.checkOut}"></h:commandButton>
			
			<h:commandButton type="submit" value="Continue Shopping" id="button4"
				styleClass="commandButton" action="#{cart.contShop}"></h:commandButton>
		</h:form>

		</t:panelTab>
		</t:panelTabbedPane>
	</hx:scriptCollector>
	</body>
</f:subview>