<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%-- jsf:pagecode language="java" location="/src/pagecode/populatePages/PopulateSelection_e.java" --%><%-- /jsf:pagecode --%><%@page
	language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@taglib uri="http://myfaces.apache.org/tomahawk" prefix="t"%>
<%@taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@taglib uri="http://www.ibm.com/jsf/html_extended" prefix="hx"%>

<f:subview id="populateSelection">
	<body>
	<p><br></p>
	<p></p>
	<hx:scriptCollector id="scriptCollector1">
					
		<t:panelTabbedPane styleClass="panelTabbedPane" id="populateSelecttabbedPane1" width="650"
											activeTabStyleClass="activeTab"
                                            inactiveTabStyleClass="inactiveTab"
                                            disabledTabStyleClass="disabledTab"
                                            activeSubStyleClass="activeSub"
                                            inactiveSubStyleClass="inactiveSub"
                                            tabContentStyleClass="tabContent"
                                            serverSideTabSwitch="false">
			<hr>
			<br>
			<t:outputText styleClass="outputText" id="text1">
						<p>Here are the options which user should choose to populate various initial values.
						Please click them in order below</p>
						<ol>
							<li>
								InitValues
							</li>
							<li>
								Populate Grades
							</li>
							<li>
								Populate Members
							</li>
							<li>
								Populate Product Categories
							</li>
						</ol>
					</t:outputText>
					<b>Look for the results at the bottom after each Population.</b>
					<br>
			<t:panelTab styleClass="panelTab" id="selectPopulateTab" label="Select Populate Options">
			
					
				
				<h:form id="populateSelectionForm">

					<br>
					<br>
					<h:selectOneListbox id="popSel" required="true"
						value="#{populate.populateSelection}"
						valueChangeListener="#{populate.populateSelectionValueChange}" size="5">
						<f:selectItems value="#{populate.populateOptionsAL}" />
					</h:selectOneListbox>
					<h:message for="popSel" showSummary="false" showDetail="true" />

					<br>
					<br>
					<h:commandButton id="PopulateSelection" value="Select Option"
						action="#{populate.selectPopulate}" />
		
					<h:messages></h:messages>

					<br>
					<h:dataTable value="#{populate.initVarsTableDataAL}"
						binding="#{populate.initVarDataTable}" var="initVars">
						<h:column>
							<f:facet name="header">
								<h:outputText value="Init Var Name" />
							</f:facet>
							<h:outputText value="#{initVars.initVarName}" />
						</h:column>
						<h:column>
							<f:facet name="header">
								<h:outputText value="Init Var Value" />
							</f:facet>
							<h:outputText value="#{initVars.initVarValue}" />
						</h:column>
					</h:dataTable>
		
				</h:form>
			</t:panelTab>

		</t:panelTabbedPane>
	</hx:scriptCollector>
	</body>
</f:subview>