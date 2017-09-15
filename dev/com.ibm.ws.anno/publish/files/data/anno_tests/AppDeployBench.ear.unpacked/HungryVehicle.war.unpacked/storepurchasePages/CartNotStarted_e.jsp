<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%-- jsf:pagecode language="java" location="/src/pagecode/storepurchasePages/CartNotStarted_e.java" --%><%-- /jsf:pagecode --%><%@page
	language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@taglib uri="http://www.ibm.com/jsf/html_extended" prefix="hx"%>
<%@taglib uri="http://myfaces.apache.org/tomahawk" prefix="t"%>

<f:subview id="cartNotStarted">
	<body>
	<hx:scriptCollector id="scriptCollector1">

		<t:panelTabbedPane styleClass="panelTabbedPane" id="tabbedPane1" width="650"
											activeTabStyleClass="activeTab"
                                            inactiveTabStyleClass="inactiveTab"
                                            disabledTabStyleClass="disabledTab"
                                            activeSubStyleClass="activeSub"
                                            inactiveSubStyleClass="inactiveSub"
                                            tabContentStyleClass="tabContent"
                                            serverSideTabSwitch="false">

			<t:panelTab styleClass="panelTab" id="cartTab"
				label="NoCart Display">


					<h2>Shopping Cart : Product List</h2>
					<br>
					<br>
			
					<p><strong>Hi <c:out value="${sessionScope.MemberID}"></c:out>.
					There is no shopping cart available to display.</strong></p>
					<br>
					<br>
					<font size="4"><strong>Number of Items in Cart = <h:outputText
						styleClass="outputText" id="text1" value="#{cart.numItemsInCart}">
					</h:outputText></strong></font>
					<br>
					<h:form styleClass="form" id="form1">
						<h:commandButton type="submit" value="Continue Shopping" label="Continue Shopping"
							styleClass="commandButton" id="button1" action="#{cart.contShop}"></h:commandButton>
					</h:form>
			</t:panelTab>


		</t:panelTabbedPane>

	</hx:scriptCollector>
	</body>
</f:subview>