<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%-- jsf:pagecode language="java" location="/src/pagecode/storepurchasePages/ProductTypeList_e.java" --%><%-- /jsf:pagecode --%><%@page
	language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@taglib uri="http://java.sun.com/jsf/core" prefix="f"%><%@taglib
	uri="http://myfaces.apache.org/tomahawk" prefix="t"%><%@taglib
	uri="http://www.ibm.com/jsf/html_extended" prefix="hx"%>
	<%@taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
	<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<f:subview id="productTypeList_view">
	<body>
	<p><br>
	</p>
	<hx:scriptCollector id="scriptCollector1">

		<t:panelTabbedPane styleClass="panelTabbedPane" id="TyepListtabbedPane1" width="650"
											activeTabStyleClass="activeTab"
                                            inactiveTabStyleClass="inactiveTab"
                                            disabledTabStyleClass="disabledTab"
                                            activeSubStyleClass="activeSub"
                                            inactiveSubStyleClass="inactiveSub"
                                            tabContentStyleClass="tabContent"
                                            serverSideTabSwitch="false">
                                            
			<t:panelTab styleClass="panelTab" id="TypelistTab" label="ProductType List Display">
			
			
		
		<hr>
		<h2>ProductType List for selected Product</h2>
		<br>
		
		<p><strong>Welcome <c:out value="${sessionScope.MemberID}"></c:out>. 
		Here are our product types. Please choose from these.</strong></p>
		<BR><br>
			<h:form id="productCategoryForm">
		      <h:dataTable value="#{productTypeList.productTypeAL}" 
		      				binding="#{product.typeDataTable}" var="productType" id="prodTypeTable">
		        <h:column>
		            <f:facet name="header">
		              <h:outputText value="ID"/>
		            </f:facet>
		            <h:outputText value="#{productType.productTypeID }" />    
		        </h:column>
		        <h:column>
		            <f:facet name="header">
		              <h:outputText value="Name"/>
		            </f:facet>
		            <h:outputText value="#{productType.productTypeName }" />
		        </h:column>
		        <h:column>
		            <f:facet name="header">
		              <h:outputText value="Price"/>
		            </f:facet>
		            <h:outputText value="#{productType.productTypePrice }">
		            	<f:convertNumber maxFractionDigits="2" minFractionDigits="2" type="number"/>
		            </h:outputText>
		        </h:column>
		        <h:column>
		            <f:facet name="header">
		              <h:outputText value="Command"/>
		            </f:facet>
				<h:commandButton id="button" value="Get Product Info for Product#{productType.productTypeID}" action="#{product.getProduct}" />
				
				</h:column>
		      </h:dataTable>
		    </h:form> 
			
			</t:panelTab>

		</t:panelTabbedPane>

	</hx:scriptCollector>
	</body>
</f:subview>