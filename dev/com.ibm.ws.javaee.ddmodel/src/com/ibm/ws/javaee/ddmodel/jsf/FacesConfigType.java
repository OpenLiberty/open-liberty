/*******************************************************************************
 * Copyright (c) 2011,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.jsf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.javaee.dd.jsf.FacesConfig;
import com.ibm.ws.javaee.dd.jsf.FacesConfigAbsoluteOrdering;
import com.ibm.ws.javaee.dd.jsf.FacesConfigApplication;
import com.ibm.ws.javaee.dd.jsf.FacesConfigApplicationResourceBundle;
import com.ibm.ws.javaee.dd.jsf.FacesConfigApplicationResourceLibraryContracts;
import com.ibm.ws.javaee.dd.jsf.FacesConfigApplicationResourceLibraryContractsContractMapping;
import com.ibm.ws.javaee.dd.jsf.FacesConfigAttribute;
import com.ibm.ws.javaee.dd.jsf.FacesConfigBehavior;
import com.ibm.ws.javaee.dd.jsf.FacesConfigClientBehaviorRenderer;
import com.ibm.ws.javaee.dd.jsf.FacesConfigComponent;
import com.ibm.ws.javaee.dd.jsf.FacesConfigConverter;
import com.ibm.ws.javaee.dd.jsf.FacesConfigDefaultValidators;
import com.ibm.ws.javaee.dd.jsf.FacesConfigELExpression;
import com.ibm.ws.javaee.dd.jsf.FacesConfigExtension;
import com.ibm.ws.javaee.dd.jsf.FacesConfigFacet;
import com.ibm.ws.javaee.dd.jsf.FacesConfigFactory;
import com.ibm.ws.javaee.dd.jsf.FacesConfigFlowDefinition;
import com.ibm.ws.javaee.dd.jsf.FacesConfigFlowDefinitionFacesMethodCall;
import com.ibm.ws.javaee.dd.jsf.FacesConfigFlowDefinitionFlowCall;
import com.ibm.ws.javaee.dd.jsf.FacesConfigFlowDefinitionFlowCallFlowReference;
import com.ibm.ws.javaee.dd.jsf.FacesConfigFlowDefinitionFlowCallInboundParameter;
import com.ibm.ws.javaee.dd.jsf.FacesConfigFlowDefinitionFlowCallOutboundParameter;
import com.ibm.ws.javaee.dd.jsf.FacesConfigFlowDefinitionFlowCallParameter;
import com.ibm.ws.javaee.dd.jsf.FacesConfigFlowDefinitionFlowReturn;
import com.ibm.ws.javaee.dd.jsf.FacesConfigFlowDefinitionSwitchCase;
import com.ibm.ws.javaee.dd.jsf.FacesConfigFlowDefinitionSwitchType;
import com.ibm.ws.javaee.dd.jsf.FacesConfigFlowDefinitionView;
import com.ibm.ws.javaee.dd.jsf.FacesConfigLifecycle;
import com.ibm.ws.javaee.dd.jsf.FacesConfigListEntries;
import com.ibm.ws.javaee.dd.jsf.FacesConfigLocale;
import com.ibm.ws.javaee.dd.jsf.FacesConfigLocaleConfig;
import com.ibm.ws.javaee.dd.jsf.FacesConfigManagedBean;
import com.ibm.ws.javaee.dd.jsf.FacesConfigManagedBeanScopeOrNone;
import com.ibm.ws.javaee.dd.jsf.FacesConfigManagedProperty;
import com.ibm.ws.javaee.dd.jsf.FacesConfigMapEntries;
import com.ibm.ws.javaee.dd.jsf.FacesConfigMapEntry;
import com.ibm.ws.javaee.dd.jsf.FacesConfigNavigationCase;
import com.ibm.ws.javaee.dd.jsf.FacesConfigNavigationRule;
import com.ibm.ws.javaee.dd.jsf.FacesConfigOrdering;
import com.ibm.ws.javaee.dd.jsf.FacesConfigProperty;
import com.ibm.ws.javaee.dd.jsf.FacesConfigProtectedViews;
import com.ibm.ws.javaee.dd.jsf.FacesConfigRedirect;
import com.ibm.ws.javaee.dd.jsf.FacesConfigRedirectParam;
import com.ibm.ws.javaee.dd.jsf.FacesConfigReferencedBean;
import com.ibm.ws.javaee.dd.jsf.FacesConfigRenderKit;
import com.ibm.ws.javaee.dd.jsf.FacesConfigRenderer;
import com.ibm.ws.javaee.dd.jsf.FacesConfigSystemEventListener;
import com.ibm.ws.javaee.dd.jsf.FacesConfigValidator;
import com.ibm.ws.javaee.dd.jsf.FacesConfigValue;
import com.ibm.ws.javaee.ddmodel.AnySimpleType;
import com.ibm.ws.javaee.ddmodel.BooleanType;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics;
import com.ibm.ws.javaee.ddmodel.DDParser.ParsableListImplements;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.IDType;
import com.ibm.ws.javaee.ddmodel.StringType;
import com.ibm.ws.javaee.ddmodel.TokenType;
import com.ibm.ws.javaee.ddmodel.common.DescriptionGroup;
import com.ibm.ws.javaee.ddmodel.common.XSDStringType;
import com.ibm.ws.javaee.ddmodel.common.XSDTokenType;
import com.ibm.ws.javaee.ddmodel.jsf.FacesConfigType.ManagedBeanType.ListType;

/*
 *<xsd:complexType name="faces-configType">
 *    <xsd:choice minOccurs="0" maxOccurs="unbounded">
 *       <xsd:element name="application" type="javaee:faces-config-applicationType"/>
 *       <xsd:element name="ordering" type="javaee:faces-config-orderingType"/>
 *       <xsd:element name="absolute-ordering" type="javaee:faces-config-absoluteOrderingType" minOccurs="0" maxOccurs="1"/>
 *       <xsd:element name="factory" type="javaee:faces-config-factoryType"/>
 *       <xsd:element name="component" type="javaee:faces-config-componentType"/>
 *       <xsd:element name="converter" type="javaee:faces-config-converterType"/>
 *       <xsd:element name="managed-bean" type="javaee:faces-config-managed-beanType"/>
 *       <xsd:element name="name" type="XSDTokenType" minOccurs="0" maxOccurs="1" />
 *       <xsd:element name="navigation-rule" type="javaee:faces-config-navigation-ruleType"/>
 *       <xsd:element name="referenced-bean" type="javaee:faces-config-referenced-beanType"/>
 *       <xsd:element name="render-kit" type="javaee:faces-config-render-kitType"/>
 *       <xsd:element name="lifecycle" type="javaee:faces-config-lifecycleType"/>
 *       <xsd:element name="validator" type="javaee:faces-config-validatorType"/>
 *       <xsd:element name="behavior" type="javaee:faces-config-behaviorType"/>
 *       <xsd:element name="faces-config-extension" type="javaee:faces-config-extensionType" minOccurs="0" maxOccurs="unbounded"/>
 *       // Following Added for JSF2.2
 *        <xsd:element name="flow-definition" type="javaee:faces-config-flow-definitionType"/>
 *        <xsd:element name="protected-views" type="javaee:faces-config-protected-viewsType" minOccurs="0" maxOccurs="unbounded"/>
 *    </xsd:choice>
 *    <xsd:attribute name="metadata-complete" type="xsd:boolean" use="optional"/>
 *   <xsd:attribute name="id" type="xsd:ID"/>
 *   <xsd:attribute name="version" type="javaee:faces-config-versionType" use="required"/>
 *</xsd:complexType>
 */
public class FacesConfigType extends DDParser.ElementContentParsable implements FacesConfig, DDParser.RootParsable {
    public FacesConfigType(String path) {
        this.path = path;
    }

    @Override
    public String getDeploymentDescriptorPath() {
        return path;
    }

    @Override
    public Object getComponentForId(String id) {
        return idMap.getComponentForId(id);
    }

    @Override
    public String getIdForComponent(Object ddComponent) {
        return idMap.getIdForComponent(ddComponent);
    }

    @Override
    public String getVersion() {
        return version.getValue();
    }

    @Override
    public List<FacesConfigManagedBean> getManagedBeans() {
        if (managed_bean != null) {
            return managed_bean.getList();
        } else {
            return Collections.emptyList();
        }
    }

    // Added to support CDI 1.2 - new injectable objects in jsf
    // This method is only called from jsf 2.2
    @Override
    public List<String> getManagedObjects() {
        ArrayList<String> managedObjects = new ArrayList<String>();
        if (this.application != null) {
            managedObjects.addAll(application.getManagedObjects());
        }
        if (this.factory != null) {
            managedObjects.addAll(factory.getManagedObjects());
        }
        if (this.lifecycle != null) {
            managedObjects.addAll(lifecycle.getManagedObjects());
        }
        return managedObjects;
    }

    // attrs
    TokenType version;
    BooleanType metadata_complete;
    // elems
    ApplicationType application;
    OrderingType ordering;
    AbsoluteOrderingType absolute_ordering;
    FactoryType factory;
    ComponentType component;
    ConverterType converter;
    ListType managed_bean;
    FlowDefinitionType flow_definition;
    XSDTokenType name;
    NavigationRuleType navigation_rule;
    ProtectedViewsType.ListType protected_views;
    ReferencedBeanType referenced_bean;
    RenderKitType render_kit;
    LifecycleType lifecycle;
    ValidatorType validator;
    BehaviorType behavior;
    ExtensionType.ListType faces_config_extension;

    /*
     * <xsd:element name="faces-config" type="javaee:faces-configType">
     * <xsd:unique name="faces-config-behavior-ID-uniqueness">
     * <xsd:selector xpath="javaee:behavior"/>
     * <xsd:field xpath="javaee:behavior-id"/>
     * </xsd:unique>
     * <xsd:unique name="faces-config-converter-ID-uniqueness">
     * <xsd:selector xpath="javaee:converter"/>
     * <xsd:field xpath="javaee:converter-id"/>
     * </xsd:unique>
     * <xsd:unique name="faces-config-converter-for-class-uniqueness">
     * <xsd:selector xpath="javaee:converter"/>
     * <xsd:field xpath="javaee:converter-for-class"/>
     * </xsd:unique>
     * <xsd:unique name="faces-config-validator-ID-uniqueness">
     * <xsd:selector xpath="javaee:validator"/>
     * <xsd:field xpath="javaee:validator-id"/>
     * </xsd:unique>
     * <xsd:unique name="faces-config-managed-bean-name-uniqueness">
     * <xsd:selector xpath="javaee:managed-bean"/>
     * <xsd:field xpath="javaee:managed-bean-name"/>
     * </xsd:unique>
     * </xsd:element>
     */

    // unique faces-config-behavior-ID-uniqueness
    Map<XSDTokenType, BehaviorType> behaviorIDToBehaviorMap;
    // unique faces-config-converter-ID-uniqueness
    Map<XSDTokenType, ConverterType> cenverterIDToConverterMap;
    // unique faces-config-converter-for-class-uniqueness
    Map<XSDTokenType, ConverterType> converterForClassToConverterMap;
    // unique faces-config-validator-ID-uniqueness
    Map<XSDTokenType, ValidatorType> validatorIDToValidatorMap;
    // unique faces-config-managed-bean-name-uniqueness
    Map<XSDTokenType, ManagedBeanType> managedBeanNameToManagedBeanMap;

    final String path;
    // Component ID map
    DDParser.ComponentIDMap idMap;

    @Override
    public boolean isIdAllowed() {
        return true;
    }

    @Override
    public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws ParseException {
        if (nsURI == null) {
            if (parser.version >= 12 && "version".equals(localName)) {
                version = parser.parseTokenAttributeValue(index);
                return true;
            }
            if (parser.version >= 12 && "metadata-complete".equals(localName)) {
                metadata_complete = parser.parseBooleanAttributeValue(index);
                return true;
            }
        }
        return false;
    }

    @Override
    public void finish(DDParser parser) throws ParseException {
        super.finish(parser);
        if (version == null) {
            if (parser.version < 12) {
                version = parser.parseToken(parser.version == 10 ? "1.0" : "1.1");
            } else {
                throw new ParseException(parser.requiredAttributeMissing("version"));
            }
        }
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws ParseException {
        if ("application".equals(localName)) {
            ApplicationType application = new ApplicationType();
            parser.parse(application);
            this.application = application;
            return true;
        }
        if ("ordering".equals(localName)) {
            OrderingType ordering = new OrderingType();
            parser.parse(ordering);
            this.ordering = ordering;
            return true;
        }
        if ("absolute-ordering".equals(localName)) {
            AbsoluteOrderingType absolute_ordering = new AbsoluteOrderingType();
            parser.parse(absolute_ordering);
            this.absolute_ordering = absolute_ordering;
            return true;
        }
        if ("factory".equals(localName)) {
            FactoryType factory = new FactoryType();
            parser.parse(factory);
            this.factory = factory;
            return true;
        }
        if ("component".equals(localName)) {
            ComponentType component = new ComponentType();
            parser.parse(component);
            this.component = component;
            return true;
        }
        if ("converter".equals(localName)) {
            ConverterType converter = new ConverterType();
            parser.parse(converter);
            this.converter = converter;
            return true;
        }
        if ("managed-bean".equals(localName)) {
            ManagedBeanType managed_bean = new ManagedBeanType();
            parser.parse(managed_bean);
            addManagedBean(managed_bean);
            return true;
        }
        if ("flow-definition".equals(localName)) {
            FlowDefinitionType flow_definition = new FlowDefinitionType();
            parser.parse(flow_definition);
            this.flow_definition = flow_definition;
            return true;
        }
        if ("name".equals(localName)) {
            XSDTokenType name = new XSDTokenType();
            parser.parse(name);
            this.name = name;
            return true;
        }
        if ("navigation-rule".equals(localName)) {
            NavigationRuleType navigation_rule = new NavigationRuleType();
            parser.parse(navigation_rule);
            this.navigation_rule = navigation_rule;
            return true;
        }
        if ("referenced-bean".equals(localName)) {
            ReferencedBeanType referenced_bean = new ReferencedBeanType();
            parser.parse(referenced_bean);
            this.referenced_bean = referenced_bean;
            return true;
        }
        if ("protected-views".equals(localName)) {
            ProtectedViewsType protected_views = new ProtectedViewsType();
            parser.parse(protected_views);
            addProtectedViews(protected_views);
            return true;
        }
        if ("render-kit".equals(localName)) {
            RenderKitType render_kit = new RenderKitType();
            parser.parse(render_kit);
            this.render_kit = render_kit;
            return true;
        }
        if ("lifecycle".equals(localName)) {
            LifecycleType lifecycle = new LifecycleType();
            parser.parse(lifecycle);
            this.lifecycle = lifecycle;
            return true;
        }
        if ("validator".equals(localName)) {
            ValidatorType validator = new ValidatorType();
            parser.parse(validator);
            this.validator = validator;
            return true;
        }
        if ("behavior".equals(localName)) {
            BehaviorType behavior = new BehaviorType();
            parser.parse(behavior);
            this.behavior = behavior;
            return true;
        }
        if ("faces-config-extension".equals(localName)) {
            ExtensionType faces_config_extension = new ExtensionType();
            parser.parse(faces_config_extension);
            addExtension(faces_config_extension);
            return true;
        }
        return false;
    }

    private void addExtension(ExtensionType faces_config_extension) {
        if (this.faces_config_extension == null) {
            this.faces_config_extension = new ExtensionType.ListType();
        }
        this.faces_config_extension.add(faces_config_extension);
    }

    private void addManagedBean(ManagedBeanType managed_bean) {
        if (this.managed_bean == null) {
            this.managed_bean = new ManagedBeanType.ListType();
        }
        this.managed_bean.add(managed_bean);
    }

    private void addProtectedViews(ProtectedViewsType protected_views) {
        if (this.protected_views == null) {
            this.protected_views = new ProtectedViewsType.ListType();
        }
        this.protected_views.add(protected_views);
    }

    @Override
    public void describe(Diagnostics diag) {
        diag.describe("version", version);
        diag.describeIfSet("metadata-complete", metadata_complete);
        diag.describeIfSet("application", application);
        diag.describeIfSet("ordering", ordering);
        diag.describeIfSet("absolute-ordering", absolute_ordering);
        diag.describeIfSet("factory", factory);
        diag.describeIfSet("component", component);
        diag.describeIfSet("converter", converter);
        diag.describeIfSet("managed-bean", managed_bean);
        diag.describeIfSet("flow-definition", flow_definition);
        diag.describeIfSet("name", name);
        diag.describeIfSet("navigation-rule", navigation_rule);
        diag.describeIfSet("protected-views", protected_views);
        diag.describeIfSet("referenced-bean", referenced_bean);
        diag.describeIfSet("render-kit", render_kit);
        diag.describeIfSet("lifecycle", lifecycle);
        diag.describeIfSet("validator", validator);
        diag.describeIfSet("behavior", behavior);
        diag.describeIfSet("faces-config-extension", faces_config_extension);
    }

    @Override
    protected String toTracingSafeString() {
        return "faces-config";
    }

    @Override
    public void describe(StringBuilder sb) {
        DDParser.Diagnostics diag = new DDParser.Diagnostics(idMap, sb);
        diag.describe(toTracingSafeString(), this);
    }

    /*
     * <xsd:complexType name="faces-config-extensionType">
     * <xsd:sequence>
     * <xsd:any namespace="##any"
     * processContents="lax"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     *
     * Related types:
     * <xsd:complexType name="faces-config-application-extensionType">
     * <xsd:complexType name="faces-config-factory-extensionType">
     * <xsd:complexType name="faces-config-attribute-extensionType">
     * <xsd:complexType name="faces-config-component-extensionType">
     * <xsd:complexType name="faces-config-facet-extensionType">
     * <xsd:complexType name="faces-config-converter-extensionType">
     * <xsd:complexType name="faces-config-lifecycle-extensionType">
     * <xsd:complexType name="faces-config-managed-bean-extensionType">
     * <xsd:complexType name="faces-config-navigation-rule-extensionType">
     * <xsd:complexType name="faces-config-property-extensionType">
     * <xsd:complexType name="faces-config-renderer-extensionType">
     * <xsd:complexType name="faces-config-render-kit-extensionType">
     * <xsd:complexType name="faces-config-behavior-extensionType">
     * <xsd:complexType name="faces-config-validator-extensionType">
     */
    static class ExtensionType extends DDParser.ElementContentParsable implements FacesConfigExtension {

        static class ListType extends DDParser.ParsableListImplements<ExtensionType, FacesConfigExtension> {
            @Override
            public ExtensionType newInstance(DDParser parser) {
                return new ExtensionType();
            }
        }

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws ParseException {
            return true;
        }

        @Trivial
        @Override
        public boolean handleContent(DDParser parser) throws ParseException {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            return true;
        }

        @Override
        public void describe(Diagnostics diag) {
            diag.append("##any");
        }
    }

    /*
     * <xsd:complexType name="faces-config-orderingType">
     * <xsd:sequence>
     * <xsd:element name="after"
     * type="javaee:faces-config-ordering-orderingType"
     * minOccurs="0"
     * maxOccurs="1"/>
     * <xsd:element name="before"
     * type="javaee:faces-config-ordering-orderingType"
     * minOccurs="0"
     * maxOccurs="1"/>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class OrderingType extends DDParser.ElementContentParsable implements FacesConfigOrdering {

        static class ListType extends ParsableListImplements<OrderingType, FacesConfigOrdering> {
            @Override
            public OrderingType newInstance(DDParser parser) {
                return new OrderingType();
            }
        }

        @Override
        public boolean isSetAfter() {
            return after != null;
        }

        @Override
        public List<String> getAfterNames() {
            return after.getNames();
        }

        @Override
        public boolean isSetAfterOthers() {
            return after.isSetOthers();
        }

        @Override
        public boolean isSetBefore() {
            return before != null;
        }

        @Override
        public List<String> getBeforeNames() {
            return before.getNames();
        }

        @Override
        public boolean isSetBeforeOthers() {
            return before.isSetOthers();
        }

        OrderingOrderingType after;
        OrderingOrderingType before;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if ("after".equals(localName)) {
                OrderingOrderingType after = new OrderingOrderingType();
                parser.parse(after);
                this.after = after;
                return true;
            }
            if ("before".equals(localName)) {
                OrderingOrderingType before = new OrderingOrderingType();
                parser.parse(before);
                this.before = before;
                return true;
            }
            return false;
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            diag.describeIfSet("after", after);
            diag.describeIfSet("before", before);
        }
    }

    /*
     * <xsd:complexType name="faces-config-ordering-orderingType">
     * <xsd:sequence>
     * <xsd:element name="name"
     * type="XSDTokenType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:element name="others"
     * type="javaee:faces-config-ordering-othersType"
     * minOccurs="0"
     * maxOccurs="1"/>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class OrderingOrderingType extends DDParser.ElementContentParsable {

        public List<String> getNames() {
            if (name != null) {
                return name.getList();
            } else {
                return Collections.emptyList();
            }
        }

        public boolean isSetOthers() {
            return others != null;
        }

        XSDTokenType.ListType name;
        OrderingOthersType others;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if ("name".equals(localName)) {
                XSDTokenType name = new XSDTokenType();
                parser.parse(name);
                addName(name);
                return true;
            }
            if ("others".equals(localName)) {
                OrderingOthersType others = new OrderingOthersType();
                parser.parse(others);
                this.others = others;
                return true;
            }
            return false;
        }

        private void addName(XSDTokenType name) {
            if (this.name == null) {
                this.name = new XSDTokenType.ListType();
            }
            this.name.add(name);
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            diag.describeIfSet("name", name);
            diag.describeIfSet("others", others);
        }
    }

    /*
     * <xsd:complexType name="faces-config-ordering-othersType">
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class OrderingOthersType extends DDParser.ElementContentParsable {

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            return false;
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            // EMPTY
        }
    }

    /*
     * <xsd:complexType name="faces-config-absoluteOrderingType">
     * <xsd:choice minOccurs="0"
     * maxOccurs="unbounded">
     * <xsd:element name="name"
     * type="XSDTokenType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:element name="others"
     * type="javaee:faces-config-ordering-othersType"
     * minOccurs="0"
     * maxOccurs="1"/>
     * </xsd:choice>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class AbsoluteOrderingType extends DDParser.ElementContentParsable implements FacesConfigAbsoluteOrdering {

        static class ListType extends ParsableListImplements<AbsoluteOrderingType, FacesConfigAbsoluteOrdering> {
            @Override
            public AbsoluteOrderingType newInstance(DDParser parser) {
                return new AbsoluteOrderingType();
            }
        }

        @Override
        public boolean isSetOthers() {
            return others != null;
        }

        @Override
        public List<String> getNamesBeforeOthers() {
            if (name != null) {
                return name.getList();
            } else {
                return Collections.emptyList();
            }
        }

        @Override
        public List<String> getNamesAfterOthers() {
            if (name2 != null) {
                return name2.getList();
            } else {
                return Collections.emptyList();
            }
        }

        XSDTokenType.ListType name;
        OrderingOthersType others;
        XSDTokenType.ListType name2;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if ("name".equals(localName)) {
                XSDTokenType name = new XSDTokenType();
                parser.parse(name);
                addName(name);
                return true;
            }
            if ("others".equals(localName)) {
                OrderingOthersType others = new OrderingOthersType();
                parser.parse(others);
                this.others = others;
                return true;
            }
            return false;
        }

        private void addName(XSDTokenType name) {
            if (others == null) {
                if (this.name == null) {
                    this.name = new XSDTokenType.ListType();
                }
                this.name.add(name);
            } else {
                if (this.name2 == null) {
                    this.name2 = new XSDTokenType.ListType();
                }
                this.name2.add(name);
            }
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            diag.describeIfSet("name", name);
            diag.describeIfSet("others", others);
            diag.describeIfSet("name", name2);
        }
    }

    /*
     * <xsd:complexType name="faces-config-applicationType">
     * <xsd:choice minOccurs="0"
     * maxOccurs="unbounded">
     * <xsd:element name="action-listener"
     * type="XSDTokenType">
     * </xsd:element>
     * <xsd:element name="default-render-kit-id"
     * type="XSDTokenType">
     * </xsd:element>
     * <xsd:element name="message-bundle"
     * type="XSDTokenType">
     * </xsd:element>
     * <xsd:element name="navigation-handler"
     * type="XSDTokenType">
     * </xsd:element>
     * <xsd:element name="view-handler"
     * type="XSDTokenType">
     * </xsd:element>
     * <xsd:element name="state-manager"
     * type="XSDTokenType">
     * </xsd:element>
     * <xsd:element name="el-resolver"
     * type="XSDTokenType">
     * </xsd:element>
     * <xsd:element name="property-resolver"
     * type="XSDTokenType">
     * </xsd:element>
     * <xsd:element name="variable-resolver"
     * type="XSDTokenType">
     * </xsd:element>
     * <xsd:element name="resource-handler"
     * type="XSDTokenType">
     * </xsd:element>
     * Added for 2.2
     * <xsd:element name="resource-library-contracts"
     * type="javaee:faces-config-application-resource-library-contractsType">
     * </xsd:element>
     * Added for JSF 2.3
     * <xsd:element name="search-expression-handler"
     * type="XSDTokenType">
     * </xsd:element>
     * Added for JSF 2.3
     * <xsd:element name="search-keyword-resolver"
     * type="XSDTokenType">
     * </xsd:element>
     * <xsd:element name="system-event-listener"
     * type="javaee:faces-config-system-event-listenerType"
     * minOccurs="0"
     * maxOccurs="unbounded">
     * </xsd:element>
     * <xsd:element name="locale-config"
     * type="javaee:faces-config-locale-configType"/>
     * <xsd:element name="resource-bundle"
     * type="javaee:faces-config-application-resource-bundleType"/>
     * <xsd:element name="application-extension"
     * type="javaee:faces-config-extensionType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:element name="default-validators"
     * type="javaee:faces-config-default-validatorsType"/>
     * </xsd:choice>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class ApplicationType extends DDParser.ElementContentParsable implements FacesConfigApplication {

        // elems
        XSDTokenType action_listener;
        XSDTokenType default_render_kit_id;
        XSDTokenType message_bundle;
        XSDTokenType navigation_handler;
        XSDTokenType view_handler;
        XSDTokenType state_manager;
        XSDTokenType el_resolver;
        XSDTokenType property_resolver;
        XSDTokenType variable_resolver;
        XSDTokenType resource_handler;
        XSDTokenType search_expression_handler; // Added for JSF 2.3
        XSDTokenType search_keyword_resolver; //  Added for JSF 2.3
        SystemEventListenerType.ListType system_event_listener;
        LocaleConfigType locale_config;
        ApplicationResourceBundleType resource_bundle;
        ExtensionType.ListType application_extension;
        DefaultValidatorsType default_validators;
        ApplicationResourceLibraryContractsType resource_library_contracts;
        XSDTokenType.ListType managed_object;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if ("action-listener".equals(localName)) {
                XSDTokenType action_listener = new XSDTokenType();
                parser.parse(action_listener);
                this.action_listener = action_listener;
                addManagedObject(parser, action_listener);
                return true;
            }
            if ("default-render-kit-id".equals(localName)) {
                XSDTokenType default_render_kit_id = new XSDTokenType();
                parser.parse(default_render_kit_id);
                this.default_render_kit_id = default_render_kit_id;
                return true;
            }
            if ("message-bundle".equals(localName)) {
                XSDTokenType message_bundle = new XSDTokenType();
                parser.parse(message_bundle);
                this.message_bundle = message_bundle;
                return true;
            }
            if ("navigation-handler".equals(localName)) {
                XSDTokenType navigation_handler = new XSDTokenType();
                parser.parse(navigation_handler);
                this.navigation_handler = navigation_handler;
                addManagedObject(parser, navigation_handler);
                return true;
            }
            if ("view-handler".equals(localName)) {
                XSDTokenType view_handler = new XSDTokenType();
                parser.parse(view_handler);
                this.view_handler = view_handler;
                return true;
            }
            if ("state-manager".equals(localName)) {
                XSDTokenType state_manager = new XSDTokenType();
                parser.parse(state_manager);
                this.state_manager = state_manager;
                addManagedObject(parser, state_manager);
                return true;
            }
            if ("el-resolver".equals(localName)) {
                XSDTokenType el_resolver = new XSDTokenType();
                parser.parse(el_resolver);
                this.el_resolver = el_resolver;
                addManagedObject(parser, el_resolver);
                return true;
            }
            if ("property-resolver".equals(localName)) {
                XSDTokenType property_resolver = new XSDTokenType();
                parser.parse(property_resolver);
                this.property_resolver = property_resolver;
                return true;
            }
            if ("variable-resolver".equals(localName)) {
                XSDTokenType variable_resolver = new XSDTokenType();
                parser.parse(variable_resolver);
                this.variable_resolver = variable_resolver;
                return true;
            }
            if ("resource-handler".equals(localName)) {
                XSDTokenType resource_handler = new XSDTokenType();
                parser.parse(resource_handler);
                this.resource_handler = resource_handler;
                addManagedObject(parser, resource_handler);
                return true;
            }
            if ("system-event-listener".equals(localName)) {
                SystemEventListenerType system_event_listener = new SystemEventListenerType();
                parser.parse(system_event_listener);
                addSystemEventListener(system_event_listener);
                addManagedObject(parser, system_event_listener.system_event_listener_class);
                return true;
            }
            if ("locale-config".equals(localName)) {
                LocaleConfigType locale_config = new LocaleConfigType();
                parser.parse(locale_config);
                this.locale_config = locale_config;
                return true;
            }
            if ("resource-bundle".equals(localName)) {
                ApplicationResourceBundleType resource_bundle = new ApplicationResourceBundleType();
                parser.parse(resource_bundle);
                this.resource_bundle = resource_bundle;
                return true;
            }
            if ("application-extension".equals(localName)) {
                ExtensionType application_extension = new ExtensionType();
                parser.parse(application_extension);
                addApplicationExtension(application_extension);
                return true;
            }
            if ("default-validators".equals(localName)) {
                DefaultValidatorsType default_validators = new DefaultValidatorsType();
                parser.parse(default_validators);
                this.default_validators = default_validators;
                return true;
            }
            if ("resource-library-contracts".equals(localName)) {
                ApplicationResourceLibraryContractsType resource_library_contracts = new ApplicationResourceLibraryContractsType();
                parser.parse(resource_library_contracts);
                this.resource_library_contracts = resource_library_contracts;
                return true;
            }
            // Added for JSF 2.3
            if ("search-expression-handler".equals(localName)) {
                XSDTokenType search_expression_handler = new XSDTokenType();
                parser.parse(search_expression_handler);
                this.search_expression_handler = search_expression_handler;
                return true;
            }
            // Added for JSF 2.3
            if ("search-keyword-resolver".equals(localName)) {
                XSDTokenType search_keyword_resolver = new XSDTokenType();
                parser.parse(search_keyword_resolver);
                this.search_keyword_resolver = search_keyword_resolver;
                return true;
            }

            return false;
        }

        private void addSystemEventListener(SystemEventListenerType system_event_listener) {
            if (this.system_event_listener == null) {
                this.system_event_listener = new SystemEventListenerType.ListType();
            }
            this.system_event_listener.add(system_event_listener);
        }

        private void addApplicationExtension(ExtensionType application_extension) {
            if (this.application_extension == null) {
                this.application_extension = new ExtensionType.ListType();
            }
            this.application_extension.add(application_extension);
        }

        private void addManagedObject(DDParser parser, XSDTokenType managed_object) {
            if (parser.version >= 22) {
                if (this.managed_object == null) {
                    this.managed_object = new XSDTokenType.ListType();
                }
                this.managed_object.add(managed_object);
            }
        }

        public List<String> getManagedObjects() {
            if (this.managed_object == null) {
                return Collections.emptyList();
            }
            return this.managed_object.getList();
        }

        @Override
        public void describe(Diagnostics diag) {
            diag.describeIfSet("action-listener", action_listener);
            diag.describeIfSet("default-render-kit-id", default_render_kit_id);
            diag.describeIfSet("message-bundle", message_bundle);
            diag.describeIfSet("navigation-handler", navigation_handler);
            diag.describeIfSet("view-handler", view_handler);
            diag.describeIfSet("state-manager", state_manager);
            diag.describeIfSet("el-resolver", el_resolver);
            diag.describeIfSet("property-resolver", property_resolver);
            diag.describeIfSet("variable-resolver", variable_resolver);
            diag.describeIfSet("resource-handler", resource_handler);
            diag.describeIfSet("system-event-listener", system_event_listener);
            diag.describeIfSet("resource-library-contracts", resource_library_contracts);
            diag.describeIfSet("search-expression-handler", search_expression_handler); // Added for JSF 2.3
            diag.describeIfSet("search-keyword-resolver", search_keyword_resolver); // Added for JSF 2.3
            diag.describeIfSet("locale-config", locale_config);
            diag.describeIfSet("resource-bundle", resource_bundle);
            diag.describeIfSet("application-extension", application_extension);
            diag.describeIfSet("default-validators", default_validators);
        }
    }

    /*
     * <xsd:complexType name="faces-config-application-resource-bundleType">
     * <xsd:sequence>
     * <xsd:group ref="javaee:descriptionGroup"/>
     * <xsd:element name="base-name"
     * type="XSDTokenType">
     * </xsd:element>
     * <xsd:element name="var"
     * type="XSDTokenType">
     * </xsd:element>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class ApplicationResourceBundleType extends DescriptionGroup implements FacesConfigApplicationResourceBundle {

        // elems
        XSDTokenType base_name;
        XSDTokenType var;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if (super.handleChild(parser, localName)) {
                return true;
            }
            if ("base-name".equals(localName)) {
                XSDTokenType base_name = new XSDTokenType();
                parser.parse(base_name);
                this.base_name = base_name;
                return true;
            }
            if ("var".equals(localName)) {
                XSDTokenType var = new XSDTokenType();
                parser.parse(var);
                this.var = var;
                return true;
            }
            return false;
        }

        @Override
        public void describe(Diagnostics diag) {
            super.describe(diag);
            diag.describe("base-name", base_name);
            diag.describe("var", var);
        }
    }

    /*
     * <xsd:complexType name="faces-config-factoryType">
     * <xsd:choice minOccurs="0"
     * maxOccurs="unbounded">
     * <xsd:element name="application-factory"
     * type="XSDTokenType">
     * </xsd:element>
     * <xsd:element name="exception-handler-factory"
     * type="XSDTokenType">
     * </xsd:element>
     * <xsd:element name="external-context-factory"
     * type="XSDTokenType">
     * </xsd:element>
     * <xsd:element name="faces-context-factory"
     * type="XSDTokenType">
     * </xsd:element>
     * <xsd:element name="facelet-cache-factory"
     * type="XSDTokenType">
     * </xsd:element>
     * <xsd:element name="partial-view-context-factory"
     * type="XSDTokenType">
     * </xsd:element>
     * <xsd:element name="lifecycle-factory"
     * type="XSDTokenType">
     * </xsd:element>
     * <xsd:element name="view-declaration-language-factory"
     * type="XSDTokenType">
     * </xsd:element>
     * <xsd:element name="tag-handler-delegate-factory"
     * type="XSDTokenType">
     * </xsd:element>
     * <xsd:element name="render-kit-factory"
     * type="XSDTokenType">
     * </xsd:element>
     * <xsd:element name="visit-context-factory"
     * type="XSDTokenType">
     * </xsd:element>
     * Start for 2.2
     * <xsd:element name="flash-factory" type="javaee:fully-qualified-classType" />
     * <xsd:element name="flow-handler-factory" type="javaee:fully-qualified-classType">
     * Complete for 2.2
     * Added for JSF 2.3
     * <xsd:element name="client-window-factory"
     * type="XSDTokenType">
     * </xsd:element>
     * Added for JSF 2.3
     * <xsd:element name="search-expression-context-factory"
     * <type="XSDTokenType">
     * </xsd:element>
     * <xsd:element name="factory-extension"
     * type="javaee:faces-config-extensionType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * </xsd:choice>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class FactoryType extends DDParser.ElementContentParsable implements FacesConfigFactory {

        //elems
        XSDTokenType application_factory;
        XSDTokenType exception_handler_factory;
        XSDTokenType external_context_factory;
        XSDTokenType faces_context_factory;
        XSDTokenType facelet_cache_factory;
        XSDTokenType partial_view_context_factory;
        XSDTokenType lifecycle_factory;
        XSDTokenType view_declaration_language_factory;
        XSDTokenType tag_handler_delegate_factory;
        XSDTokenType render_kit_factory;
        XSDTokenType visit_context_factory;
        XSDTokenType flash_factory;
        XSDTokenType flow_handler_factory;
        XSDTokenType client_window_factory; // Added for JSF 2.3
        XSDTokenType search_expression_context_factory; // Added for JSF 2.3
        ExtensionType.ListType factory_extension;
        XSDTokenType.ListType managed_object;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if ("application-factory".equals(localName)) {
                XSDTokenType application_factory = new XSDTokenType();
                parser.parse(application_factory);
                this.application_factory = application_factory;
                addManagedObject(parser, application_factory);
                return true;
            }
            if ("exception-handler-factory".equals(localName)) {
                XSDTokenType exception_handler_factory = new XSDTokenType();
                parser.parse(exception_handler_factory);
                this.exception_handler_factory = exception_handler_factory;
                addManagedObject(parser, exception_handler_factory);
                return true;
            }
            if ("external-context-factory".equals(localName)) {
                XSDTokenType external_context_factory = new XSDTokenType();
                parser.parse(external_context_factory);
                this.external_context_factory = external_context_factory;
                addManagedObject(parser, external_context_factory);
                return true;
            }
            if ("faces-context-factory".equals(localName)) {
                XSDTokenType faces_context_factory = new XSDTokenType();
                parser.parse(faces_context_factory);
                this.faces_context_factory = faces_context_factory;
                addManagedObject(parser, faces_context_factory);
                return true;
            }
            if ("facelet-cache-factory".equals(localName)) {
                XSDTokenType facelet_cache_factory = new XSDTokenType();
                parser.parse(facelet_cache_factory);
                this.facelet_cache_factory = facelet_cache_factory;
                addManagedObject(parser, facelet_cache_factory);
                return true;
            }
            if ("partial-view-context-factory".equals(localName)) {
                XSDTokenType partial_view_context_factory = new XSDTokenType();
                parser.parse(partial_view_context_factory);
                this.partial_view_context_factory = partial_view_context_factory;
                addManagedObject(parser, partial_view_context_factory);
                return true;
            }
            if ("lifecycle-factory".equals(localName)) {
                XSDTokenType lifecycle_factory = new XSDTokenType();
                parser.parse(lifecycle_factory);
                this.lifecycle_factory = lifecycle_factory;
                addManagedObject(parser, lifecycle_factory);
                return true;
            }
            if ("view-declaration-language-factory".equals(localName)) {
                XSDTokenType view_declaration_language_factory = new XSDTokenType();
                parser.parse(view_declaration_language_factory);
                this.view_declaration_language_factory = view_declaration_language_factory;
                addManagedObject(parser, view_declaration_language_factory);
                return true;
            }
            if ("tag-handler-delegate-factory".equals(localName)) {
                XSDTokenType tag_handler_delegate_factory = new XSDTokenType();
                parser.parse(tag_handler_delegate_factory);
                this.tag_handler_delegate_factory = tag_handler_delegate_factory;
                addManagedObject(parser, tag_handler_delegate_factory);
                return true;
            }
            if ("render-kit-factory".equals(localName)) {
                XSDTokenType render_kit_factory = new XSDTokenType();
                parser.parse(render_kit_factory);
                this.render_kit_factory = render_kit_factory;
                addManagedObject(parser, render_kit_factory);
                return true;
            }
            if ("visit-context-factory".equals(localName)) {
                XSDTokenType visit_context_factory = new XSDTokenType();
                parser.parse(visit_context_factory);
                this.visit_context_factory = visit_context_factory;
                addManagedObject(parser, visit_context_factory);
                return true;
            }
            if ("flash-factory".equals(localName)) {
                XSDTokenType flash_factory = new XSDTokenType();
                parser.parse(flash_factory);
                this.flash_factory = flash_factory;
                return true;
            }
            if ("flow-handler-factory".equals(localName)) {
                XSDTokenType flow_handler_factory = new XSDTokenType();
                parser.parse(flow_handler_factory);
                this.flow_handler_factory = flow_handler_factory;
                return true;
            }
            if ("factory-extension".equals(localName)) {
                ExtensionType factory_extension = new ExtensionType();
                parser.parse(factory_extension);
                addFactoryExtension(factory_extension);
                return true;
            }
            // Added for JSF 2.3
            if ("client-window-factory".equals(localName)) {
                XSDTokenType client_window_factory = new XSDTokenType();
                parser.parse(client_window_factory);
                this.client_window_factory = client_window_factory;
                addManagedObject(parser, client_window_factory);
                return true;
            }
            // Added for JSF 2.3
            if ("search-expression-context-factory".equals(localName)) {
                XSDTokenType search_expression_context_factory = new XSDTokenType();
                parser.parse(search_expression_context_factory);
                this.search_expression_context_factory = search_expression_context_factory;
                return true;
            }
            return false;
        }

        private void addFactoryExtension(ExtensionType factory_extension) {
            if (this.factory_extension == null) {
                this.factory_extension = new ExtensionType.ListType();
            }
            this.factory_extension.add(factory_extension);
        }

        private void addManagedObject(DDParser parser, XSDTokenType managed_object) {
            if (parser.version >= 22) {
                if (this.managed_object == null) {
                    this.managed_object = new XSDTokenType.ListType();
                }
                this.managed_object.add(managed_object);
            }
        }

        public List<String> getManagedObjects() {
            if (this.managed_object == null) {
                return Collections.emptyList();
            }
            return this.managed_object.getList();
        }

        @Override
        public void describe(Diagnostics diag) {
            diag.describeIfSet("application-factory", application_factory);
            diag.describeIfSet("exception-handler-factory", exception_handler_factory);
            diag.describeIfSet("external-context-factory", external_context_factory);
            diag.describeIfSet("faces-context-factory", faces_context_factory);
            diag.describeIfSet("facelet-cache-factory", facelet_cache_factory);
            diag.describeIfSet("partial-view-context-factory", partial_view_context_factory);
            diag.describeIfSet("lifecycle-factory", lifecycle_factory);
            diag.describeIfSet("view-declaration-language-factory", view_declaration_language_factory);
            diag.describeIfSet("tag-handler-delegate-factory", tag_handler_delegate_factory);
            diag.describeIfSet("render-kit-factory", render_kit_factory);
            diag.describeIfSet("visit-context-factory", visit_context_factory);
            diag.describeIfSet("flash-factory", flash_factory);
            diag.describeIfSet("flow-handler-factory", flow_handler_factory);
            diag.describeIfSet("client-window-factory", client_window_factory); // Added for JSF 2.3
            diag.describeIfSet("search-expression-context-factory", search_expression_context_factory); // Added for JSF 2.3
            diag.describeIfSet("factory-extension", factory_extension);
        }
    }

    /*
     * <xsd:complexType name="faces-config-attributeType">
     * <xsd:sequence>
     * <xsd:group ref="javaee:descriptionGroup"/>
     * <xsd:element name="attribute-name"
     * type="XSDTokenType">
     * </xsd:element>
     * <xsd:element name="attribute-class"
     * type="XSDTokenType">
     * </xsd:element>
     * <xsd:element name="default-value"
     * type="javaee:faces-config-default-valueType"
     * minOccurs="0"/>
     * <xsd:element name="suggested-value"
     * type="javaee:faces-config-suggested-valueType"
     * minOccurs="0"/>
     * <xsd:element name="attribute-extension"
     * type="javaee:faces-config-extensionType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class AttributeType extends DescriptionGroup implements FacesConfigAttribute {

        static class ListType extends ParsableListImplements<AttributeType, FacesConfigAttribute> {
            @Override
            public AttributeType newInstance(DDParser parser) {
                return new AttributeType();
            }
        }

        // elems
        XSDTokenType attribute_name;
        XSDTokenType attribute_class;
        XSDTokenType default_value;
        XSDTokenType suggested_value;
        ExtensionType.ListType attribute_extension;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if (super.handleChild(parser, localName)) {
                return true;
            }
            if ("attribute-name".equals(localName)) {
                XSDTokenType attribute_name = new XSDTokenType();
                parser.parse(attribute_name);
                this.attribute_name = attribute_name;
                return true;
            }
            if ("attribute-class".equals(localName)) {
                XSDTokenType attribute_class = new XSDTokenType();
                parser.parse(attribute_class);
                this.attribute_class = attribute_class;
                return true;
            }
            if ("default-value".equals(localName)) {
                XSDTokenType default_value = new XSDTokenType();
                parser.parse(default_value);
                this.default_value = default_value;
                return true;
            }
            if ("suggested-value".equals(localName)) {
                XSDTokenType suggested_value = new XSDTokenType();
                parser.parse(suggested_value);
                this.suggested_value = suggested_value;
                return true;
            }
            if ("attribute-extension".equals(localName)) {
                ExtensionType attribute_extension = new ExtensionType();
                parser.parse(attribute_extension);
                addAttributeExtension(attribute_extension);
                return true;
            }
            return false;
        }

        private void addAttributeExtension(ExtensionType attribute_extension) {
            if (this.attribute_extension == null) {
                this.attribute_extension = new ExtensionType.ListType();
            }
            this.attribute_extension.add(attribute_extension);
        }

        @Override
        public void describe(Diagnostics diag) {
            super.describe(diag);
            diag.describe("attribute-name", attribute_name);
            diag.describe("attribute-class", attribute_class);
            diag.describeIfSet("default-value", default_value);
            diag.describeIfSet("suggested-value", suggested_value);
            diag.describeIfSet("attribute-extension", attribute_extension);
        }
    }

    /*
     * <xsd:complexType name="faces-config-componentType">
     * <xsd:sequence>
     * <xsd:group ref="javaee:descriptionGroup"/>
     * <xsd:element name="component-type"
     * type="XSDTokenType">
     * </xsd:element>
     * <xsd:element name="component-class"
     * type="XSDTokenType">
     * </xsd:element>
     * <xsd:element name="facet"
     * type="javaee:faces-config-facetType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:element name="attribute"
     * type="javaee:faces-config-attributeType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:element name="property"
     * type="javaee:faces-config-propertyType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:element name="component-extension"
     * type="javaee:faces-config-extensionType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class ComponentType extends DescriptionGroup implements FacesConfigComponent {

        // elems
        XSDTokenType component_type;
        XSDTokenType component_class;
        FacetType.ListType facet;
        AttributeType.ListType attribute;
        PropertyType.ListType property;
        ExtensionType.ListType component_extension;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if (super.handleChild(parser, localName)) {
                return true;
            }
            if ("component-type".equals(localName)) {
                XSDTokenType component_type = new XSDTokenType();
                parser.parse(component_type);
                this.component_type = component_type;
                return true;
            }
            if ("component-class".equals(localName)) {
                XSDTokenType component_class = new XSDTokenType();
                parser.parse(component_class);
                this.component_class = component_class;
                return true;
            }
            if ("facet".equals(localName)) {
                FacetType facet = new FacetType();
                parser.parse(facet);
                addFacet(facet);
                return true;
            }
            if ("attribute".equals(localName)) {
                AttributeType attribute = new AttributeType();
                parser.parse(attribute);
                addAttribute(attribute);
                return true;
            }
            if ("property".equals(localName)) {
                PropertyType property = new PropertyType();
                parser.parse(property);
                addProperty(property);
                return true;
            }
            if ("component-extension".equals(localName)) {
                ExtensionType component_extension = new ExtensionType();
                parser.parse(component_extension);
                addComponentExtension(component_extension);
                return true;
            }
            return false;
        }

        private void addFacet(FacetType facet) {
            if (this.facet == null) {
                this.facet = new FacetType.ListType();
            }
            this.facet.add(facet);
        }

        private void addAttribute(AttributeType attribute) {
            if (this.attribute == null) {
                this.attribute = new AttributeType.ListType();
            }
            this.attribute.add(attribute);
        }

        private void addProperty(PropertyType property) {
            if (this.property == null) {
                this.property = new PropertyType.ListType();
            }
            this.property.add(property);
        }

        private void addComponentExtension(ExtensionType component_extension) {
            if (this.component_extension == null) {
                this.component_extension = new ExtensionType.ListType();
            }
            this.component_extension.add(component_extension);
        }

        @Override
        public void describe(Diagnostics diag) {
            super.describe(diag);
            diag.describe("component-type", component_type);
            diag.describe("component-class", component_class);
            diag.describeIfSet("facet", facet);
            diag.describeIfSet("attribute", attribute);
            diag.describeIfSet("property", property);
            diag.describeIfSet("component-extension", component_extension);
        }
    }

    /*
     * <xsd:complexType name="faces-config-default-localeType">
     * <xsd:simpleContent>
     * <xsd:extension base="javaee:faces-config-localeType">
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:extension>
     * </xsd:simpleContent>
     * </xsd:complexType>
     */
    /*
     * <xsd:simpleType name="faces-config-localeType">
     * <xsd:restriction base="xsd:string">
     * <xsd:pattern value="([a-z]{2})[_|\-]?([\p{L}]{2})?[_|\-]?(\w+)?"/>
     * </xsd:restriction>
     * </xsd:simpleType>
     */
    /*
     * <xsd:complexType name="faces-config-supported-localeType">
     * <xsd:simpleContent>
     * <xsd:extension base="javaee:faces-config-localeType">
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:extension>
     * </xsd:simpleContent>
     * </xsd:complexType>
     */
    static class LocaleType extends XSDStringType implements FacesConfigLocale {
        @Override
        public void finish(DDParser parser) throws ParseException {
            super.finish(parser);
            // add lexical checking if needed
        }
    }

    /*
     * <xsd:complexType name="faces-config-default-valueType">
     * <xsd:simpleContent>
     * <xsd:restriction base="XSDTokenType">
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:restriction>
     * </xsd:simpleContent>
     * </xsd:complexType>
     */
    /*
     * <xsd:complexType name="faces-config-suggested-valueType">
     * <xsd:simpleContent>
     * <xsd:restriction base="XSDTokenType"/>
     * </xsd:simpleContent>
     * </xsd:complexType>
     */
    //
    // Just use XSDTokenType directly
    //

    /*
     * <xsd:simpleType name="faces-config-el-expressionType">
     * <xsd:restriction base="xsd:string">
     * <xsd:pattern value="#\{.*\}"/>
     * </xsd:restriction>
     * </xsd:simpleType>
     */
    static class ELExpressionType extends StringType implements FacesConfigELExpression {
        @Override
        public void finish(DDParser parser) throws ParseException {
            super.finish(parser);
            // add lexical checking if needed
        }
    }

    /*
     * <xsd:complexType name="faces-config-facetType">
     * <xsd:sequence>
     * <xsd:group ref="javaee:descriptionGroup"/>
     * <xsd:element name="facet-name"
     * type="XSDTokenType">
     * </xsd:element>
     * <xsd:element name="facet-extension"
     * type="javaee:faces-config-extensionType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class FacetType extends DescriptionGroup implements FacesConfigFacet {

        static class ListType extends ParsableListImplements<FacetType, FacesConfigFacet> {
            @Override
            public FacetType newInstance(DDParser parser) {
                return new FacetType();
            }
        }

        // elems
        XSDTokenType facet_name;
        ExtensionType.ListType facet_extension;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if (super.handleChild(parser, localName)) {
                return true;
            }
            if ("facet-name".equals(localName)) {
                XSDTokenType facet_name = new XSDTokenType();
                parser.parse(facet_name);
                this.facet_name = facet_name;
                return true;
            }
            if ("facet-extension".equals(localName)) {
                ExtensionType facet_extension = new ExtensionType();
                parser.parse(facet_extension);
                addFacetExtension(facet_extension);
                return true;
            }
            return false;
        }

        private void addFacetExtension(ExtensionType facet_extension) {
            if (this.facet_extension == null) {
                this.facet_extension = new ExtensionType.ListType();
            }
            this.facet_extension.add(facet_extension);
        }

        @Override
        public void describe(Diagnostics diag) {
            super.describe(diag);
            diag.describe("facet-name", facet_name);
            diag.describeIfSet("facet-extension", facet_extension);
        }
    }

    /*
     * <xsd:complexType name="faces-config-from-actionType">
     * <xsd:simpleContent>
     * <xsd:extension base="javaee:faces-config-el-expressionType">
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:extension>
     * </xsd:simpleContent>
     * </xsd:complexType>
     */
    /*
     * <xsd:complexType name="faces-config-ifType">
     * <xsd:simpleContent>
     * <xsd:extension base="javaee:faces-config-el-expressionType">
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:extension>
     * </xsd:simpleContent>
     * </xsd:complexType>
     */
    static class ELExpressionWithIDType extends ELExpressionType {
        @Override
        public boolean isIdAllowed() {
            return true;
        }
    }

    /*
     * <xsd:complexType name="faces-config-converterType">
     * <xsd:sequence>
     * <xsd:group ref="javaee:descriptionGroup"/>
     * <xsd:choice>
     * <xsd:element name="converter-id"
     * type="XSDTokenType">
     * </xsd:element>
     * <xsd:element name="converter-for-class"
     * type="XSDTokenType">
     * </xsd:element>
     * </xsd:choice>
     * <xsd:element name="converter-class"
     * type="XSDTokenType">
     * </xsd:element>
     * <xsd:element name="attribute"
     * type="javaee:faces-config-attributeType"
     * minOccurs="0"
     * maxOccurs="unbounded">
     * </xsd:element>
     * <xsd:element name="property"
     * type="javaee:faces-config-propertyType"
     * minOccurs="0"
     * maxOccurs="unbounded">
     * </xsd:element>
     * <xsd:element name="converter-extension"
     * type="javaee:faces-config-extensionType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class ConverterType extends DescriptionGroup implements FacesConfigConverter {

        // elems
        XSDTokenType converter_id;
        XSDTokenType converter_for_class;
        XSDTokenType converter_class;
        AttributeType.ListType attribute;
        PropertyType.ListType property;
        ExtensionType.ListType converter_extension;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if (super.handleChild(parser, localName)) {
                return true;
            }
            if ("converter-id".equals(localName)) {
                XSDTokenType converter_id = new XSDTokenType();
                parser.parse(converter_id);
                this.converter_id = converter_id;
                return true;
            }
            if ("converter-for-class".equals(localName)) {
                XSDTokenType converter_for_class = new XSDTokenType();
                parser.parse(converter_for_class);
                this.converter_for_class = converter_for_class;
                return true;
            }
            if ("converter-class".equals(localName)) {
                XSDTokenType converter_class = new XSDTokenType();
                parser.parse(converter_class);
                this.converter_class = converter_class;
                return true;
            }
            if ("attribute".equals(localName)) {
                AttributeType attribute = new AttributeType();
                parser.parse(attribute);
                addAttribute(attribute);
                return true;
            }
            if ("property".equals(localName)) {
                PropertyType property = new PropertyType();
                parser.parse(property);
                addProperty(property);
                return true;
            }
            if ("converter-extension".equals(localName)) {
                ExtensionType converter_extension = new ExtensionType();
                parser.parse(converter_extension);
                addConverterExtension(converter_extension);
                return true;
            }
            return false;
        }

        private void addAttribute(AttributeType attribute) {
            if (this.attribute == null) {
                this.attribute = new AttributeType.ListType();
            }
            this.attribute.add(attribute);
        }

        private void addProperty(PropertyType property) {
            if (this.property == null) {
                this.property = new PropertyType.ListType();
            }
            this.property.add(property);
        }

        private void addConverterExtension(ExtensionType converter_extension) {
            if (this.converter_extension == null) {
                this.converter_extension = new ExtensionType.ListType();
            }
            this.converter_extension.add(converter_extension);
        }

        @Override
        public void describe(Diagnostics diag) {
            super.describe(diag);
            if (converter_id != null) {
                diag.describe("converter-id", converter_id);
            } else {
                diag.describe("converter-for-class", converter_for_class);
            }
            diag.describe("converter-class", converter_class);
            diag.describeIfSet("attribute", attribute);
            diag.describeIfSet("property", property);
            diag.describeIfSet("converter-extension", converter_extension);
        }
    }

    /*
     * <xsd:complexType name="faces-config-lifecycleType">
     * <xsd:sequence>
     * <xsd:element name="phase-listener"
     * type="XSDTokenType"
     * minOccurs="0"
     * maxOccurs="unbounded">
     * </xsd:element>
     * <xsd:element name="lifecycle-extension"
     * type="javaee:faces-config-extensionType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class LifecycleType extends DDParser.ElementContentParsable implements FacesConfigLifecycle {

        //elems
        XSDTokenType.ListType phase_listener;
        ExtensionType.ListType lifecycle_extension;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if ("phase-listener".equals(localName)) {
                XSDTokenType phase_listener = new XSDTokenType();
                parser.parse(phase_listener);
                addPhaseListener(phase_listener);
                return true;
            }
            if ("lifecycle-extension".equals(localName)) {
                ExtensionType lifecycle_extension = new ExtensionType();
                parser.parse(lifecycle_extension);
                addLifecycleExtension(lifecycle_extension);
                return true;
            }
            return false;
        }

        private void addPhaseListener(XSDTokenType phase_listener) {
            if (this.phase_listener == null) {
                this.phase_listener = new XSDTokenType.ListType();
            }
            this.phase_listener.add(phase_listener);
        }

        private void addLifecycleExtension(ExtensionType lifecycle_extension) {
            if (this.lifecycle_extension == null) {
                this.lifecycle_extension = new ExtensionType.ListType();
            }
            this.lifecycle_extension.add(lifecycle_extension);
        }

        public List<String> getManagedObjects() {
            if (this.phase_listener == null) {
                return Collections.emptyList();
            }
            return this.phase_listener.getList();
        }

        @Override
        public void describe(Diagnostics diag) {
            diag.describeIfSet("phase-listener", phase_listener);
            diag.describeIfSet("lifecycle-extension", lifecycle_extension);
        }
    }

    /*
     * <xsd:complexType name="faces-config-locale-configType">
     * <xsd:sequence>
     * <xsd:element name="default-locale"
     * type="javaee:faces-config-default-localeType"
     * minOccurs="0">
     * </xsd:element>
     * <xsd:element name="supported-locale"
     * type="javaee:faces-config-supported-localeType"
     * minOccurs="0"
     * maxOccurs="unbounded">
     * </xsd:element>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class LocaleConfigType extends DDParser.ElementContentParsable implements FacesConfigLocaleConfig {

        //elems
        LocaleType default_locale;
        LocaleType.ListType supported_locale;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if ("default-locale".equals(localName)) {
                LocaleType default_locale = new LocaleType();
                parser.parse(default_locale);
                this.default_locale = default_locale;
                return true;
            }
            if ("supported-locale".equals(localName)) {
                LocaleType supported_locale = new LocaleType();
                parser.parse(supported_locale);
                addSupportedLocale(supported_locale);
                return true;
            }
            return false;
        }

        private void addSupportedLocale(LocaleType supported_locale) {
            if (this.supported_locale == null) {
                this.supported_locale = new LocaleType.ListType();
            }
            this.supported_locale.add(supported_locale);
        }

        @Override
        public void describe(Diagnostics diag) {
            diag.describeIfSet("default-locale", default_locale);
            diag.describeIfSet("supported-locale", supported_locale);
        }
    }

    /*
     * <xsd:complexType name="faces-config-default-validatorsType">
     * <xsd:sequence>
     * <xsd:element name="validator-id"
     * type="XSDTokenType"
     * minOccurs="0"
     * maxOccurs="unbounded">
     * </xsd:element>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class DefaultValidatorsType extends DDParser.ElementContentParsable implements FacesConfigDefaultValidators {

        //elems
        XSDTokenType.ListType validator_id;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if ("validator-id".equals(localName)) {
                XSDTokenType validator_id = new XSDTokenType();
                parser.parse(validator_id);
                addValidatorID(validator_id);
                return true;
            }
            return false;
        }

        private void addValidatorID(XSDTokenType validator_id) {
            if (this.validator_id == null) {
                this.validator_id = new XSDTokenType.ListType();
            }
            this.validator_id.add(validator_id);
        }

        @Override
        public void describe(Diagnostics diag) {
            diag.describeIfSet("validator-id", validator_id);
        }
    }

    /*
     * <xsd:complexType name="faces-config-managed-beanType">
     * <xsd:sequence>
     * <xsd:group ref="javaee:descriptionGroup"/>
     * <xsd:element name="managed-bean-name"
     * type="XSDTokenType">
     * </xsd:element>
     * <xsd:element name="managed-bean-class"
     * type="XSDTokenType">
     * </xsd:element>
     * <xsd:element name="managed-bean-scope"
     * type="javaee:faces-config-managed-bean-scopeOrNoneType">
     * </xsd:element>
     * <xsd:choice>
     * <xsd:element name="managed-property"
     * type="javaee:faces-config-managed-propertyType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:element name="map-entries"
     * type="javaee:faces-config-map-entriesType"/>
     * <xsd:element name="list-entries"
     * type="javaee:faces-config-list-entriesType"/>
     * </xsd:choice>
     * <xsd:element name="managed-bean-extension"
     * type="javaee:faces-config-extensionType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * </xsd:sequence>
     * <xsd:attribute name="eager"
     * type="xsd:boolean"
     * use="optional">
     * </xsd:attribute>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class ManagedBeanType extends DescriptionGroup implements FacesConfigManagedBean {

        public static class ListType extends ParsableListImplements<ManagedBeanType, FacesConfigManagedBean> {
            @Override
            public ManagedBeanType newInstance(DDParser parser) {
                return new ManagedBeanType();
            }
        }

        // attrs
        BooleanType eager;
        // elems
        XSDTokenType managed_bean_name;
        XSDTokenType managed_bean_class;
        ManagedBeanScopeOrNoneType managed_bean_scope;
        ManagedPropertyType.ListType managed_property;
        MapEntriesType map_entries;
        ListEntriesType list_entries;
        ExtensionType.ListType managed_bean_extension;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public String getManagedBeanName() {
            return managed_bean_name != null ? managed_bean_name.getValue() : null;
        }

        @Override
        public String getManagedBeanClass() {
            return managed_bean_class != null ? managed_bean_class.getValue() : null;
        }

        @Override
        public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws ParseException {
            if (nsURI == null) {
                if ("eager".equals(localName)) {
                    eager = parser.parseBooleanAttributeValue(index);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if (super.handleChild(parser, localName)) {
                return true;
            }
            if ("managed-bean-name".equals(localName)) {
                XSDTokenType managed_bean_name = new XSDTokenType();
                parser.parse(managed_bean_name);
                this.managed_bean_name = managed_bean_name;
                return true;
            }
            if ("managed-bean-class".equals(localName)) {
                XSDTokenType managed_bean_class = new XSDTokenType();
                parser.parse(managed_bean_class);
                this.managed_bean_class = managed_bean_class;
                return true;
            }
            if ("managed-bean-scope".equals(localName)) {
                ManagedBeanScopeOrNoneType managed_bean_scope = new ManagedBeanScopeOrNoneType();
                parser.parse(managed_bean_scope);
                this.managed_bean_scope = managed_bean_scope;
                return true;
            }
            if ("managed-property".equals(localName)) {
                ManagedPropertyType managed_property = new ManagedPropertyType();
                parser.parse(managed_property);
                addManagedProperty(managed_property);
                return true;
            }
            if ("map-entries".equals(localName)) {
                MapEntriesType map_entries = new MapEntriesType();
                parser.parse(map_entries);
                this.map_entries = map_entries;
                return true;
            }
            if ("list-entries".equals(localName)) {
                ListEntriesType list_entries = new ListEntriesType();
                parser.parse(list_entries);
                this.list_entries = list_entries;
                return true;
            }
            if ("managed-bean-extension".equals(localName)) {
                ExtensionType managed_bean_extension = new ExtensionType();
                parser.parse(managed_bean_extension);
                addManagedBeanExtension(managed_bean_extension);
                return true;
            }
            return false;
        }

        private void addManagedProperty(ManagedPropertyType managed_property) {
            if (this.managed_property == null) {
                this.managed_property = new ManagedPropertyType.ListType();
            }
            this.managed_property.add(managed_property);
        }

        private void addManagedBeanExtension(ExtensionType managed_bean_extension) {
            if (this.managed_bean_extension == null) {
                this.managed_bean_extension = new ExtensionType.ListType();
            }
            this.managed_bean_extension.add(managed_bean_extension);
        }

        @Override
        public void describe(Diagnostics diag) {
            super.describe(diag);
            diag.describeIfSet("eager", eager);
            diag.describe("managed-bean-name", managed_bean_name);
            diag.describe("managed-bean-class", managed_bean_class);
            diag.describe("managed-bean-scope", managed_bean_scope);
            diag.describeIfSet("managed-property", managed_property);
            diag.describeIfSet("map-entries", map_entries);
            diag.describeIfSet("list-entries", list_entries);
            diag.describeIfSet("managed-bean-extension", managed_bean_extension);
        }
    }

    /*
     * <xsd:complexType name="faces-config-managed-bean-scopeOrNoneType">
     * <xsd:simpleContent>
     * <xsd:restriction base="XSDTokenType">
     * <xsd:pattern value="view|request|session|application|none|#\{.*\}"/>
     * </xsd:restriction>
     * </xsd:simpleContent>
     * </xsd:complexType>
     */
    static class ManagedBeanScopeOrNoneType extends XSDTokenType implements FacesConfigManagedBeanScopeOrNone {

        @Override
        public boolean isELExpression() {
            return enum_value == null;
        }

        @Override
        public String getELExpression() {
            return getValue();
        }

        @Override
        public ScopeEnum getScopeEnum() {
            return enum_value;
        }

        ScopeEnum enum_value;

        @Override
        public void finish(DDParser parser) throws ParseException {
            super.finish(parser);
            String v = super.getValue();
            if (!v.startsWith("#{") || !v.endsWith("}")) {
                enum_value = ScopeEnum.valueOf(v);
            }
            // add lexical checking if needed
        }
    }

    /*
     * <xsd:complexType name="faces-config-managed-propertyType">
     * <xsd:sequence>
     * <xsd:group ref="javaee:descriptionGroup"/>
     * <xsd:element name="property-name"
     * type="XSDTokenType">
     * </xsd:element>
     * <xsd:element name="property-class"
     * type="XSDTokenType"
     * minOccurs="0">
     * </xsd:element>
     * <xsd:choice>
     * <xsd:element name="map-entries"
     * type="javaee:faces-config-map-entriesType"/>
     * <xsd:element name="null-value"
     * type="javaee:faces-config-null-valueType">
     * </xsd:element>
     * <xsd:element name="value"
     * type="javaee:faces-config-valueType"/>
     * <xsd:element name="list-entries"
     * type="javaee:faces-config-list-entriesType"/>
     * </xsd:choice>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class ManagedPropertyType extends DescriptionGroup implements FacesConfigManagedProperty {

        static class ListType extends ParsableListImplements<ManagedPropertyType, FacesConfigManagedProperty> {
            @Override
            public ManagedPropertyType newInstance(DDParser parser) {
                return new ManagedPropertyType();
            }
        }

        // elems
        XSDTokenType property_name;
        XSDTokenType property_class;
        MapEntriesType map_entries;
        NullValueType null_value;
        ValueType value;
        ListEntriesType list_entries;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if (super.handleChild(parser, localName)) {
                return true;
            }
            if ("property-name".equals(localName)) {
                XSDTokenType property_name = new XSDTokenType();
                parser.parse(property_name);
                this.property_name = property_name;
                return true;
            }
            if ("property-class".equals(localName)) {
                XSDTokenType property_class = new XSDTokenType();
                parser.parse(property_class);
                this.property_class = property_class;
                return true;
            }
            if ("map-entries".equals(localName)) {
                MapEntriesType map_entries = new MapEntriesType();
                parser.parse(map_entries);
                this.map_entries = map_entries;
                return true;
            }
            if ("null-value".equals(localName)) {
                NullValueType null_value = new NullValueType();
                parser.parse(null_value);
                this.null_value = null_value;
                return true;
            }
            if ("value".equals(localName)) {
                ValueType value = new ValueType();
                parser.parse(value);
                this.value = value;
                return true;
            }
            if ("list-entries".equals(localName)) {
                ListEntriesType list_entries = new ListEntriesType();
                parser.parse(list_entries);
                this.list_entries = list_entries;
                return true;
            }
            return false;
        }

        @Override
        public void describe(Diagnostics diag) {
            super.describe(diag);
            diag.describe("property-name", property_name);
            diag.describeIfSet("property-class", property_class);
            diag.describeIfSet("map-entries", map_entries);
            diag.describeIfSet("null-value", null_value);
            diag.describeIfSet("value", value);
            diag.describeIfSet("list-entries", list_entries);
        }
    }

    /*
     * <xsd:complexType name="faces-config-map-entryType">
     * <xsd:sequence>
     * <xsd:element name="key"
     * type="XSDTokenType">
     * </xsd:element>
     * <xsd:choice>
     * <xsd:element name="null-value"
     * type="javaee:faces-config-null-valueType"/>
     * <xsd:element name="value"
     * type="javaee:faces-config-valueType"/>
     * </xsd:choice>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class MapEntryType extends DDParser.ElementContentParsable implements FacesConfigMapEntry {

        static class ListType extends DDParser.ParsableListImplements<MapEntryType, FacesConfigMapEntry> {
            @Override
            public MapEntryType newInstance(DDParser parser) {
                return new MapEntryType();
            }
        }

        //elems
        XSDTokenType key;
        NullValueType null_value;
        ValueType value;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if ("key".equals(localName)) {
                XSDTokenType key = new XSDTokenType();
                parser.parse(key);
                this.key = key;
                return true;
            }
            if ("null-value".equals(localName)) {
                NullValueType null_value = new NullValueType();
                parser.parse(null_value);
                this.null_value = null_value;
                return true;
            }
            if ("value".equals(localName)) {
                ValueType value = new ValueType();
                parser.parse(value);
                this.value = value;
                return true;
            }
            return false;
        }

        @Override
        public void describe(Diagnostics diag) {
            diag.describe("key", key);
            diag.describeIfSet("null-value", null_value);
            diag.describeIfSet("value", value);
        }
    }

    /*
     * <xsd:complexType name="faces-config-map-entriesType">
     * <xsd:sequence>
     * <xsd:element name="key-class"
     * type="XSDTokenType"
     * minOccurs="0">
     * </xsd:element>
     * <xsd:element name="value-class"
     * type="XSDTokenType"
     * minOccurs="0"/>
     * <xsd:element name="map-entry"
     * type="javaee:faces-config-map-entryType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class MapEntriesType extends DDParser.ElementContentParsable implements FacesConfigMapEntries {

        //elems
        XSDTokenType key_class;
        XSDTokenType value_class;
        MapEntryType.ListType map_entry;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if ("key-class".equals(localName)) {
                XSDTokenType key_class = new XSDTokenType();
                parser.parse(key_class);
                this.key_class = key_class;
                return true;
            }
            if ("value-class".equals(localName)) {
                XSDTokenType value_class = new XSDTokenType();
                parser.parse(value_class);
                this.value_class = value_class;
                return true;
            }
            if ("map-entry".equals(localName)) {
                MapEntryType map_entry = new MapEntryType();
                parser.parse(map_entry);
                addMapEntry(map_entry);
                return true;
            }
            return false;
        }

        private void addMapEntry(MapEntryType map_entry) {
            if (this.map_entry == null) {
                this.map_entry = new MapEntryType.ListType();
            }
            this.map_entry.add(map_entry);
        }

        @Override
        public void describe(Diagnostics diag) {
            diag.describeIfSet("key-class", key_class);
            diag.describeIfSet("value-class", value_class);
            diag.describeIfSet("map-entry", map_entry);
        }
    }

    /*
     * <xsd:complexType name="faces-config-navigation-caseType">
     * <xsd:sequence>
     * <xsd:group ref="javaee:descriptionGroup"/>
     * <xsd:element name="from-action"
     * type="javaee:faces-config-from-actionType"
     * minOccurs="0">
     * </xsd:element>
     * <xsd:element name="from-outcome"
     * type="XSDTokenType"
     * minOccurs="0">
     * </xsd:element>
     * <xsd:element name="if"
     * type="javaee:faces-config-ifType"
     * minOccurs="0">
     * </xsd:element>
     * <xsd:element name="to-view-id"
     * type="javaee:faces-config-valueType">
     * </xsd:element>
     * <xsd:element name="to-flow-document-id" type="javaee:java-identifierType" minOccurs="0" /> //added for 2.2
     * <xsd:element name="redirect"
     * type="javaee:faces-config-redirectType"
     * minOccurs="0"/>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class NavigationCaseType extends DescriptionGroup implements FacesConfigNavigationCase {

        static class ListType extends DDParser.ParsableListImplements<NavigationCaseType, FacesConfigNavigationCase> {
            @Override
            public NavigationCaseType newInstance(DDParser parser) {
                return new NavigationCaseType();
            }
        }

        // elems
        ELExpressionWithIDType from_action;
        XSDTokenType from_outcome;
        ELExpressionWithIDType if_expr;
        ValueType to_view_id;
        RedirectType redirect;
        XSDTokenType to_flow_document_id;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if (super.handleChild(parser, localName)) {
                return true;
            }
            if ("from-action".equals(localName)) {
                ELExpressionWithIDType from_action = new ELExpressionWithIDType();
                parser.parse(from_action);
                this.from_action = from_action;
                return true;
            }
            if ("from-outcome".equals(localName)) {
                XSDTokenType from_outcome = new XSDTokenType();
                parser.parse(from_outcome);
                this.from_outcome = from_outcome;
                return true;
            }
            if ("if".equals(localName)) {
                ELExpressionWithIDType if_expr = new ELExpressionWithIDType();
                parser.parse(if_expr);
                this.if_expr = if_expr;
                return true;
            }
            if ("to-view-id".equals(localName)) {
                ValueType to_view_id = new ValueType();
                parser.parse(to_view_id);
                this.to_view_id = to_view_id;
                return true;
            }
            if ("to-flow-document-id".equals(localName)) {
                XSDTokenType to_flow_document_id = new XSDTokenType();
                parser.parse(to_flow_document_id);
                this.to_flow_document_id = to_flow_document_id;
                return true;
            }
            if ("redirect".equals(localName)) {
                RedirectType redirect = new RedirectType();
                parser.parse(redirect);
                this.redirect = redirect;
                return true;
            }
            return false;
        }

        @Override
        public void describe(Diagnostics diag) {
            super.describe(diag);
            diag.describeIfSet("from-action", from_action);
            diag.describeIfSet("from-outcome", from_outcome);
            diag.describeIfSet("if", if_expr);
            diag.describe("to-view-id", to_view_id);
            diag.describeIfSet("redirect", redirect);
            diag.describe("to-flow-document-id", to_flow_document_id);
        }
    }

    /*
     * <xsd:complexType name="faces-config-navigation-ruleType">
     * <xsd:sequence>
     * <xsd:group ref="javaee:descriptionGroup"/>
     * <xsd:element name="from-view-id"
     * type="javaee:faces-config-from-view-idType"
     * minOccurs="0"/>
     * <xsd:element name="navigation-case"
     * type="javaee:faces-config-navigation-caseType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:element name="navigation-rule-extension"
     * type="javaee:faces-config-extensionType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class NavigationRuleType extends DescriptionGroup implements FacesConfigNavigationRule {

        // elems
        XSDTokenType from_view_id;
        NavigationCaseType.ListType navigation_case;
        ExtensionType.ListType navigation_rule_extension;

        static class ListType extends DDParser.ParsableListImplements<NavigationRuleType, FacesConfigNavigationRule> {
            @Override
            public NavigationRuleType newInstance(DDParser parser) {
                return new NavigationRuleType();
            }
        }

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if (super.handleChild(parser, localName)) {
                return true;
            }
            if ("from-view-id".equals(localName)) {
                XSDTokenType from_view_id = new XSDTokenType();
                parser.parse(from_view_id);
                this.from_view_id = from_view_id;
                return true;
            }
            if ("navigation-case".equals(localName)) {
                NavigationCaseType navigation_case = new NavigationCaseType();
                parser.parse(navigation_case);
                addNavigationCase(navigation_case);
                return true;
            }
            if ("navigation-rule-extension".equals(localName)) {
                ExtensionType navigation_rule_extension = new ExtensionType();
                parser.parse(navigation_rule_extension);
                addNavigationRuleExtension(navigation_rule_extension);
                return true;
            }
            return false;
        }

        private void addNavigationCase(NavigationCaseType navigation_case) {
            if (this.navigation_case == null) {
                this.navigation_case = new NavigationCaseType.ListType();
            }
            this.navigation_case.add(navigation_case);
        }

        private void addNavigationRuleExtension(ExtensionType navigation_rule_extension) {
            if (this.navigation_rule_extension == null) {
                this.navigation_rule_extension = new ExtensionType.ListType();
            }
            this.navigation_rule_extension.add(navigation_rule_extension);
        }

        @Override
        public void describe(Diagnostics diag) {
            super.describe(diag);
            diag.describeIfSet("from-view-id", from_view_id);
            diag.describeIfSet("navigation-case", navigation_case);
            diag.describeIfSet("navigation-rule-extension", navigation_rule_extension);
        }
    }

    /*
     * <xsd:complexType name="faces-config-null-valueType">
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class NullValueType extends AnySimpleType/* DDParser.ElementContentParsable */ {

        protected NullValueType() {
            super(Whitespace.preserve);
        }

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            // EMPTY
        }

        @Override
        protected void setValueFromLexical(DDParser parser, String lexical) throws ParseException {
            // not called
        }
    }

    /*
     * <xsd:complexType name="faces-config-propertyType">
     * <xsd:sequence>
     * <xsd:group ref="javaee:descriptionGroup"/>
     * <xsd:element name="property-name"
     * type="XSDTokenType">
     * </xsd:element>
     * <xsd:element name="property-class"
     * type="XSDTokenType">
     * </xsd:element>
     * <xsd:element name="default-value"
     * type="javaee:faces-config-default-valueType"
     * minOccurs="0"/>
     * <xsd:element name="suggested-value"
     * type="javaee:faces-config-suggested-valueType"
     * minOccurs="0"/>
     * <xsd:element name="property-extension"
     * type="javaee:faces-config-extensionType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class PropertyType extends DescriptionGroup implements FacesConfigProperty {

        static class ListType extends ParsableListImplements<PropertyType, FacesConfigProperty> {
            @Override
            public PropertyType newInstance(DDParser parser) {
                return new PropertyType();
            }
        }

        // elems
        XSDTokenType property_name;
        XSDTokenType property_class;
        XSDTokenType default_value;
        XSDTokenType suggested_value;
        ExtensionType.ListType property_extension;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if (super.handleChild(parser, localName)) {
                return true;
            }
            if ("property-name".equals(localName)) {
                XSDTokenType property_name = new XSDTokenType();
                parser.parse(property_name);
                this.property_name = property_name;
                return true;
            }
            if ("property-class".equals(localName)) {
                XSDTokenType property_class = new XSDTokenType();
                parser.parse(property_class);
                this.property_class = property_class;
                return true;
            }
            if ("default-value".equals(localName)) {
                XSDTokenType default_value = new XSDTokenType();
                parser.parse(default_value);
                this.default_value = default_value;
                return true;
            }
            if ("suggested-value".equals(localName)) {
                XSDTokenType suggested_value = new XSDTokenType();
                parser.parse(suggested_value);
                this.suggested_value = suggested_value;
                return true;
            }
            if ("property-extension".equals(localName)) {
                ExtensionType property_extension = new ExtensionType();
                parser.parse(property_extension);
                addPropertyExtension(property_extension);
                return true;
            }
            return false;
        }

        private void addPropertyExtension(ExtensionType property_extension) {
            if (this.property_extension == null) {
                this.property_extension = new ExtensionType.ListType();
            }
            this.property_extension.add(property_extension);
        }

        @Override
        public void describe(Diagnostics diag) {
            super.describe(diag);
            diag.describe("property-name", property_name);
            diag.describe("property-class", property_class);
            diag.describeIfSet("default-value", default_value);
            diag.describeIfSet("suggested-value", suggested_value);
            diag.describeIfSet("property-extension", property_extension);
        }
    }

    /*
     * <xsd:complexType name="faces-config-redirectType">
     * <xsd:sequence>
     * <xsd:element name="view-param"
     * type="javaee:faces-config-redirect-viewParamType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:element name="redirect-param"
     * type="javaee:faces-config-redirect-redirectParamType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * <xsd:attribute name="include-view-params"
     * type="xsd:boolean"
     * use="optional"/>
     * </xsd:complexType>
     */
    static class RedirectType extends DDParser.ElementContentParsable implements FacesConfigRedirect {

        //attrs
        BooleanType include_view_params;
        //elems
        //RedirectParamType.ListType view_param;
        RedirectParamType.ListType redirect_param;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws ParseException {
            if (nsURI == null) {
                if ("include-view-params".equals(localName)) {
                    include_view_params = parser.parseBooleanAttributeValue(index);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if ("view-param".equals(localName)) {
                // any view-param elements are added as redirect-param elements since this was
                // a mistake in the spec and not as intended.
                RedirectParamType view_param = new RedirectParamType();
                parser.parse(view_param);
                addRedirectParam(view_param);
                return true;
            }
            if ("redirect-param".equals(localName)) {
                RedirectParamType redirect_param = new RedirectParamType();
                parser.parse(redirect_param);
                addRedirectParam(redirect_param);
                return true;
            }
            return false;
        }

        private void addRedirectParam(RedirectParamType redirect_param) {
            if (this.redirect_param == null) {
                this.redirect_param = new RedirectParamType.ListType();
            }
            this.redirect_param.add(redirect_param);
        }

        @Override
        public void describe(Diagnostics diag) {
            diag.describeIfSet("include-view-params", include_view_params);
            diag.describeIfSet("redirect-param", redirect_param);
        }
    }

    /*
     * <xsd:complexType name="faces-config-redirect-viewParamType">
     * <xsd:sequence>
     * <xsd:element name="name"
     * type="XSDTokenType"
     * minOccurs="1"
     * maxOccurs="1"/>
     * <xsd:element name="value"
     * type="XSDTokenType"
     * minOccurs="1"
     * maxOccurs="1"/>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    /*
     * <xsd:complexType name="faces-config-redirect-redirectParamType">
     * <xsd:sequence>
     * <xsd:element name="name"
     * type="XSDTokenType"
     * minOccurs="1"
     * maxOccurs="1"/>
     * <xsd:element name="value"
     * type="XSDTokenType"
     * minOccurs="1"
     * maxOccurs="1"/>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class RedirectParamType extends DDParser.ElementContentParsable implements FacesConfigRedirectParam {

        static class ListType extends ParsableListImplements<RedirectParamType, FacesConfigRedirectParam> {
            @Override
            public RedirectParamType newInstance(DDParser parser) {
                return new RedirectParamType();
            }
        }

        //elems
        XSDTokenType name;
        XSDTokenType value;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if ("name".equals(localName)) {
                XSDTokenType name = new XSDTokenType();
                parser.parse(name);
                this.name = name;
                return true;
            }
            if ("value".equals(localName)) {
                XSDTokenType value = new XSDTokenType();
                parser.parse(value);
                this.value = value;
                return true;
            }
            return false;
        }

        @Override
        public void describe(Diagnostics diag) {
            diag.describe("name", name);
            diag.describe("value", value);
        }
    }

    /*
     * <xsd:complexType name="faces-config-referenced-beanType">
     * <xsd:sequence>
     * <xsd:group ref="javaee:descriptionGroup"/>
     * <xsd:element name="referenced-bean-name"
     * type="XSDTokenType">
     * </xsd:element>
     * <xsd:element name="referenced-bean-class"
     * type="XSDTokenType">
     * </xsd:element>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class ReferencedBeanType extends DescriptionGroup implements FacesConfigReferencedBean {

        // elems
        XSDTokenType referenced_bean_name;
        XSDTokenType referenced_bean_class;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if (super.handleChild(parser, localName)) {
                return true;
            }
            if ("referenced-bean-name".equals(localName)) {
                XSDTokenType referenced_bean_name = new XSDTokenType();
                parser.parse(referenced_bean_name);
                this.referenced_bean_name = referenced_bean_name;
                return true;
            }
            if ("referenced-bean-class".equals(localName)) {
                XSDTokenType referenced_bean_class = new XSDTokenType();
                parser.parse(referenced_bean_class);
                this.referenced_bean_class = referenced_bean_class;
                return true;
            }
            return false;
        }

        @Override
        public void describe(Diagnostics diag) {
            super.describe(diag);
            diag.describe("referenced-bean-name", referenced_bean_name);
            diag.describe("referenced-bean-class", referenced_bean_class);
        }
    }

    /*
     * <xsd:complexType name="faces-config-render-kitType">
     * <xsd:sequence>
     * <xsd:group ref="javaee:descriptionGroup"/>
     * <xsd:element name="render-kit-id"
     * type="XSDTokenType"
     * minOccurs="0">
     * </xsd:element>
     * <xsd:element name="render-kit-class"
     * type="XSDTokenType"
     * minOccurs="0">
     * </xsd:element>
     * <xsd:element name="renderer"
     * type="javaee:faces-config-rendererType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:element name="client-behavior-renderer"
     * type="javaee:faces-config-client-behavior-rendererType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:element name="render-kit-extension"
     * type="javaee:faces-config-extensionType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class RenderKitType extends DescriptionGroup implements FacesConfigRenderKit {

        // elems
        XSDTokenType render_kit_id;
        XSDTokenType render_kit_class;
        RendererType.ListType renderer;
        ClientBehaviorRendererType.ListType client_behavior_renderer;
        ExtensionType.ListType render_kit_extension;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if (super.handleChild(parser, localName)) {
                return true;
            }
            if ("render-kit-id".equals(localName)) {
                XSDTokenType render_kit_id = new XSDTokenType();
                parser.parse(render_kit_id);
                this.render_kit_id = render_kit_id;
                return true;
            }
            if ("render-kit-class".equals(localName)) {
                XSDTokenType render_kit_class = new XSDTokenType();
                parser.parse(render_kit_class);
                this.render_kit_class = render_kit_class;
                return true;
            }
            if ("renderer".equals(localName)) {
                RendererType renderer = new RendererType();
                parser.parse(renderer);
                addRenderer(renderer);
                return true;
            }
            if ("client-behavior-renderer".equals(localName)) {
                ClientBehaviorRendererType client_behavior_renderer = new ClientBehaviorRendererType();
                parser.parse(client_behavior_renderer);
                addClientBehaviorRenderer(client_behavior_renderer);
                return true;
            }
            if ("render-kit-extension".equals(localName)) {
                ExtensionType render_kit_extension = new ExtensionType();
                parser.parse(render_kit_extension);
                addRenderKitExtension(render_kit_extension);
                return true;
            }
            return false;
        }

        private void addRenderer(RendererType renderer) {
            if (this.renderer == null) {
                this.renderer = new RendererType.ListType();
            }
            this.renderer.add(renderer);
        }

        private void addClientBehaviorRenderer(ClientBehaviorRendererType client_behavior_renderer) {
            if (this.client_behavior_renderer == null) {
                this.client_behavior_renderer = new ClientBehaviorRendererType.ListType();
            }
            this.client_behavior_renderer.add(client_behavior_renderer);
        }

        private void addRenderKitExtension(ExtensionType render_kit_extension) {
            if (this.render_kit_extension == null) {
                this.render_kit_extension = new ExtensionType.ListType();
            }
            this.render_kit_extension.add(render_kit_extension);
        }

        @Override
        public void describe(Diagnostics diag) {
            super.describe(diag);
            diag.describeIfSet("render-kit-id", render_kit_id);
            diag.describeIfSet("render-kit-class", render_kit_class);
            diag.describeIfSet("renderer", renderer);
            diag.describeIfSet("client-behavior-renderer", client_behavior_renderer);
            diag.describeIfSet("render-kit-extension", render_kit_extension);
        }
    }

    /*
     * <xsd:complexType name="faces-config-client-behavior-rendererType">
     * <xsd:sequence>
     * <xsd:element name="client-behavior-renderer-type"
     * type="XSDTokenType">
     * </xsd:element>
     * <xsd:element name="client-behavior-renderer-class"
     * type="XSDTokenType">
     * </xsd:element>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class ClientBehaviorRendererType extends DescriptionGroup implements FacesConfigClientBehaviorRenderer {

        static class ListType extends ParsableListImplements<ClientBehaviorRendererType, FacesConfigClientBehaviorRenderer> {
            @Override
            public ClientBehaviorRendererType newInstance(DDParser parser) {
                return new ClientBehaviorRendererType();
            }
        }

        // elems
        XSDTokenType client_behavior_renderer_type;
        XSDTokenType client_behavior_renderer_class;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if (super.handleChild(parser, localName)) {
                return true;
            }
            if ("client-behavior-renderer-type".equals(localName)) {
                XSDTokenType client_behavior_renderer_type = new XSDTokenType();
                parser.parse(client_behavior_renderer_type);
                this.client_behavior_renderer_type = client_behavior_renderer_type;
                return true;
            }
            if ("client-behavior-renderer-class".equals(localName)) {
                XSDTokenType client_behavior_renderer_class = new XSDTokenType();
                parser.parse(client_behavior_renderer_class);
                this.client_behavior_renderer_class = client_behavior_renderer_class;
                return true;
            }
            return false;
        }

        @Override
        public void describe(Diagnostics diag) {
            super.describe(diag);
            diag.describe("client-behavior-renderer-type", client_behavior_renderer_type);
            diag.describe("client-behavior-renderer-class", client_behavior_renderer_class);
        }
    }

    /*
     * <xsd:complexType name="faces-config-rendererType">
     * <xsd:sequence>
     * <xsd:group ref="javaee:descriptionGroup"/>
     * <xsd:element name="component-family"
     * type="XSDTokenType">
     * </xsd:element>
     * <xsd:element name="renderer-type"
     * type="XSDTokenType">
     * </xsd:element>
     * <xsd:element name="renderer-class"
     * type="XSDTokenType">
     * </xsd:element>
     * <xsd:element name="facet"
     * type="javaee:faces-config-facetType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:element name="attribute"
     * type="javaee:faces-config-attributeType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:element name="renderer-extension"
     * type="javaee:faces-config-extensionType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class RendererType extends DescriptionGroup implements FacesConfigRenderer {

        static class ListType extends ParsableListImplements<RendererType, FacesConfigRenderer> {
            @Override
            public RendererType newInstance(DDParser parser) {
                return new RendererType();
            }
        }

        // elems
        XSDTokenType component_family;
        XSDTokenType renderer_type;
        XSDTokenType renderer_class;
        FacetType.ListType facet;
        AttributeType.ListType attribute;
        ExtensionType.ListType renderer_extension;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if (super.handleChild(parser, localName)) {
                return true;
            }
            if ("component-family".equals(localName)) {
                XSDTokenType component_family = new XSDTokenType();
                parser.parse(component_family);
                this.component_family = component_family;
                return true;
            }
            if ("renderer-type".equals(localName)) {
                XSDTokenType renderer_type = new XSDTokenType();
                parser.parse(renderer_type);
                this.renderer_type = renderer_type;
                return true;
            }
            if ("renderer-class".equals(localName)) {
                XSDTokenType renderer_class = new XSDTokenType();
                parser.parse(renderer_class);
                this.renderer_class = renderer_class;
                return true;
            }
            if ("facet".equals(localName)) {
                FacetType facet = new FacetType();
                parser.parse(facet);
                addFacet(facet);
                return true;
            }
            if ("attribute".equals(localName)) {
                AttributeType attribute = new AttributeType();
                parser.parse(attribute);
                addAttribute(attribute);
                return true;
            }
            if ("renderer-extension".equals(localName)) {
                ExtensionType renderer_extension = new ExtensionType();
                parser.parse(renderer_extension);
                addRendererExtension(renderer_extension);
                return true;
            }
            return false;
        }

        private void addFacet(FacetType facet) {
            if (this.facet == null) {
                this.facet = new FacetType.ListType();
            }
            this.facet.add(facet);
        }

        private void addAttribute(AttributeType attribute) {
            if (this.attribute == null) {
                this.attribute = new AttributeType.ListType();
            }
            this.attribute.add(attribute);
        }

        private void addRendererExtension(ExtensionType renderer_extension) {
            if (this.renderer_extension == null) {
                this.renderer_extension = new ExtensionType.ListType();
            }
            this.renderer_extension.add(renderer_extension);
        }

        @Override
        public void describe(Diagnostics diag) {
            super.describe(diag);
            diag.describe("component-family", component_family);
            diag.describe("renderer-type", renderer_type);
            diag.describe("renderer-class", renderer_class);
            diag.describeIfSet("facet", facet);
            diag.describeIfSet("attribute", attribute);
            diag.describeIfSet("renderer-extension", renderer_extension);
        }
    }

    /*
     * <xsd:complexType name="faces-config-behaviorType">
     * <xsd:sequence>
     * <xsd:group ref="javaee:descriptionGroup"/>
     * <xsd:element name="behavior-id"
     * type="XSDTokenType">
     * </xsd:element>
     * <xsd:element name="behavior-class"
     * type="XSDTokenType">
     * </xsd:element>
     * <xsd:element name="attribute"
     * type="javaee:faces-config-attributeType"
     * minOccurs="0"
     * maxOccurs="unbounded">
     * </xsd:element>
     * <xsd:element name="property"
     * type="javaee:faces-config-propertyType"
     * minOccurs="0"
     * maxOccurs="unbounded">
     * </xsd:element>
     * <xsd:element name="behavior-extension"
     * type="javaee:faces-config-extensionType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class BehaviorType extends DescriptionGroup implements FacesConfigBehavior {

        // elems
        XSDTokenType behavior_id;
        XSDTokenType behavior_class;
        AttributeType.ListType attribute;
        PropertyType.ListType property;
        ExtensionType.ListType behavior_extension;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if (super.handleChild(parser, localName)) {
                return true;
            }
            if ("behavior-id".equals(localName)) {
                XSDTokenType behavior_id = new XSDTokenType();
                parser.parse(behavior_id);
                this.behavior_id = behavior_id;
                return true;
            }
            if ("behavior-class".equals(localName)) {
                XSDTokenType behavior_class = new XSDTokenType();
                parser.parse(behavior_class);
                this.behavior_class = behavior_class;
                return true;
            }
            if ("attribute".equals(localName)) {
                AttributeType attribute = new AttributeType();
                parser.parse(attribute);
                addAttribute(attribute);
                return true;
            }
            if ("property".equals(localName)) {
                PropertyType property = new PropertyType();
                parser.parse(property);
                addProperty(property);
                return true;
            }
            if ("behavior-extension".equals(localName)) {
                ExtensionType behavior_extension = new ExtensionType();
                parser.parse(behavior_extension);
                addBehaviorExtension(behavior_extension);
                return true;
            }
            return false;
        }

        private void addAttribute(AttributeType attribute) {
            if (this.attribute == null) {
                this.attribute = new AttributeType.ListType();
            }
            this.attribute.add(attribute);
        }

        private void addProperty(PropertyType property) {
            if (this.property == null) {
                this.property = new PropertyType.ListType();
            }
            this.property.add(property);
        }

        private void addBehaviorExtension(ExtensionType behavior_extension) {
            if (this.behavior_extension == null) {
                this.behavior_extension = new ExtensionType.ListType();
            }
            this.behavior_extension.add(behavior_extension);
        }

        @Override
        public void describe(Diagnostics diag) {
            super.describe(diag);
            diag.describe("behavior-id", behavior_id);
            diag.describe("behavior-class", behavior_class);
            diag.describeIfSet("attribute", attribute);
            diag.describeIfSet("property", property);
            diag.describeIfSet("behavior-extension", behavior_extension);
        }
    }

    /*
     * <xsd:complexType name="faces-config-validatorType">
     * <xsd:sequence>
     * <xsd:group ref="javaee:descriptionGroup"/>
     * <xsd:element name="validator-id"
     * type="XSDTokenType">
     * </xsd:element>
     * <xsd:element name="validator-class"
     * type="XSDTokenType">
     * </xsd:element>
     * <xsd:element name="attribute"
     * type="javaee:faces-config-attributeType"
     * minOccurs="0"
     * maxOccurs="unbounded">
     * </xsd:element>
     * <xsd:element name="property"
     * type="javaee:faces-config-propertyType"
     * minOccurs="0"
     * maxOccurs="unbounded">
     * </xsd:element>
     * <xsd:element name="validator-extension"
     * type="javaee:faces-config-extensionType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class ValidatorType extends DescriptionGroup implements FacesConfigValidator {

        // elems
        XSDTokenType validator_id;
        XSDTokenType validator_class;
        AttributeType.ListType attribute;
        PropertyType.ListType property;
        ExtensionType.ListType validator_extension;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if (super.handleChild(parser, localName)) {
                return true;
            }
            if ("validator-id".equals(localName)) {
                XSDTokenType validator_id = new XSDTokenType();
                parser.parse(validator_id);
                this.validator_id = validator_id;
                return true;
            }
            if ("validator-class".equals(localName)) {
                XSDTokenType validator_class = new XSDTokenType();
                parser.parse(validator_class);
                this.validator_class = validator_class;
                return true;
            }
            if ("attribute".equals(localName)) {
                AttributeType attribute = new AttributeType();
                parser.parse(attribute);
                addAttribute(attribute);
                return true;
            }
            if ("property".equals(localName)) {
                PropertyType property = new PropertyType();
                parser.parse(property);
                addProperty(property);
                return true;
            }
            if ("validator-extension".equals(localName)) {
                ExtensionType validator_extension = new ExtensionType();
                parser.parse(validator_extension);
                addValidatorExtension(validator_extension);
                return true;
            }
            return false;
        }

        private void addAttribute(AttributeType attribute) {
            if (this.attribute == null) {
                this.attribute = new AttributeType.ListType();
            }
            this.attribute.add(attribute);
        }

        private void addProperty(PropertyType property) {
            if (this.property == null) {
                this.property = new PropertyType.ListType();
            }
            this.property.add(property);
        }

        private void addValidatorExtension(ExtensionType validator_extension) {
            if (this.validator_extension == null) {
                this.validator_extension = new ExtensionType.ListType();
            }
            this.validator_extension.add(validator_extension);
        }

        @Override
        public void describe(Diagnostics diag) {
            super.describe(diag);
            diag.describe("validator-id", validator_id);
            diag.describe("validator-class", validator_class);
            diag.describeIfSet("attribute", attribute);
            diag.describeIfSet("property", property);
            diag.describeIfSet("validator-extension", validator_extension);
        }
    }

    /*
     * <xsd:simpleType name="faces-config-valueType">
     * <xsd:union memberTypes="javaee:faces-config-el-expressionType xsd:string"/>
     * </xsd:simpleType>
     */
    static class ValueType extends StringType implements FacesConfigValue {

        public boolean isELExpresion() {
            String v = getValue();
            return v != null && v.startsWith("#{") && v.endsWith("}");
        }

        @Override
        public String getValue() {
            return super.getValue();
        }
    }

    /*
     * <xsd:complexType name="faces-config-list-entriesType">
     * <xsd:sequence>
     * <xsd:element name="value-class"
     * type="XSDTokenType"
     * minOccurs="0"/>
     * <xsd:choice minOccurs="0"
     * maxOccurs="unbounded">
     * <xsd:element name="null-value"
     * type="javaee:faces-config-null-valueType"/>
     * <xsd:element name="value"
     * type="javaee:faces-config-valueType"/>
     * </xsd:choice>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class ListEntriesType extends DDParser.ElementContentParsable implements FacesConfigListEntries {

        //elems
        XSDTokenType value_class;
        //NullValueType null_value;
        //ValueType value;

        AnySimpleType.ListType entries;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if ("value-class".equals(localName)) {
                XSDTokenType value_class = new XSDTokenType();
                parser.parse(value_class);
                this.value_class = value_class;
                return true;
            }
            if ("null-value".equals(localName)) {
                NullValueType null_value = new NullValueType();
                parser.parse(null_value);
                addEntry(null_value);
                return true;
            }
            if ("value".equals(localName)) {
                ValueType value = new ValueType();
                parser.parse(value);
                addEntry(value);
                return true;
            }
            return false;
        }

        private void addEntry(AnySimpleType entry) {
            if (this.entries == null) {
                this.entries = new AnySimpleType.ListType();
            }
            this.entries.add(entry);
        }

        @Override
        public void describe(Diagnostics diag) {
            diag.describeIfSet("value-class", value_class);
            diag.describeIfSet("entries", entries);
        }
    }

    /*
     * <xsd:complexType name="faces-config-system-event-listenerType">
     * <xsd:sequence>
     * <xsd:element name="system-event-listener-class"
     * type="XSDTokenType">
     * </xsd:element>
     * <xsd:element name="system-event-class"
     * type="XSDTokenType">
     * </xsd:element>
     * <xsd:element name="source-class"
     * minOccurs="0"
     * type="XSDTokenType">
     * </xsd:element>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class SystemEventListenerType extends DDParser.ElementContentParsable implements FacesConfigSystemEventListener {

        static class ListType extends ParsableListImplements<SystemEventListenerType, FacesConfigSystemEventListener> {
            @Override
            public SystemEventListenerType newInstance(DDParser parser) {
                return new SystemEventListenerType();
            }
        }

        //elems
        XSDTokenType system_event_listener_class;
        XSDTokenType system_event_class;
        XSDTokenType source_class;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if ("system-event-listener-class".equals(localName)) {
                XSDTokenType system_event_listener_class = new XSDTokenType();
                parser.parse(system_event_listener_class);
                this.system_event_listener_class = system_event_listener_class;
                return true;
            }
            if ("system-event-class".equals(localName)) {
                XSDTokenType system_event_class = new XSDTokenType();
                parser.parse(system_event_class);
                this.system_event_class = system_event_class;
                return true;
            }
            if ("source-class".equals(localName)) {
                XSDTokenType source_class = new XSDTokenType();
                parser.parse(source_class);
                this.source_class = source_class;
                return true;
            }
            return false;
        }

        @Override
        public void describe(Diagnostics diag) {
            diag.describe("system-event-listener-class", system_event_listener_class);
            diag.describe("system-event-class", system_event_class);
            diag.describeIfSet("source-class", source_class);
        }
    }

    // Contract Additions in XSD for JSF22

    /*
     * <xsd:complexType name="faces-config-application-resource-library-contracts-contract-mappingType">
     * <xsd:sequence>
     * <xsd:group ref="javaee:descriptionGroup"/>
     * <xsd:element name="url-pattern" type="javaee:url-patternType" maxOccurs="unbounded"> </xsd:element>
     * <xsd:element name="contracts" type="javaee:string" maxOccurs="unbounded"> </xsd:element>
     * </xsd:sequence>
     * <xsd:attribute name="id" type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class ResourceLibraryContractsContractMappingType extends DescriptionGroup implements FacesConfigApplicationResourceLibraryContractsContractMapping {

        // elements
        StringType.ListType url_pattern = new StringType.ListType();
        StringType.ListType contracts = new StringType.ListType();

        public static class ListType extends ParsableListImplements<ResourceLibraryContractsContractMappingType, FacesConfigApplicationResourceLibraryContractsContractMapping> {
            @Override
            public ResourceLibraryContractsContractMappingType newInstance(DDParser parser) {
                return new ResourceLibraryContractsContractMappingType();
            }
        }

        @Override
        public List<String> getContracts() {
            return contracts.getList();
        }

        @Override
        public List<String> getURLPatterns() {
            return url_pattern.getList();
        }

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {

            if ("url-pattern".equals(localName)) {
                StringType url_pattern = new StringType();
                parser.parse(url_pattern);
                this.url_pattern.add(url_pattern);
                return true;
            }

            if ("contracts".equals(localName)) {
                StringType contracts = new StringType();
                parser.parse(contracts);
                this.contracts.add(contracts);
                return true;
            }

            return false;
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            diag.describe("url-pattern", url_pattern);
            diag.describe("contracts", contracts);
        }

    }

    /*
     * <xsd:complexType name="faces-config-application-resource-library-contractsType">
     * <xsd:sequence>
     * <xsd:group ref="javaee:descriptionGroup"/>
     * <xsd:element name="contract-mapping" type="javaee:faces-config-application-resource-library-contracts-contract-mappingType"
     * maxOccurs="unbounded">
     * </xsd:element>
     * </xsd:sequence>
     * <xsd:attribute name="id"type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class ApplicationResourceLibraryContractsType extends DescriptionGroup implements FacesConfigApplicationResourceLibraryContracts {
        // elems
        ResourceLibraryContractsContractMappingType.ListType contract_mapping;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if ("contract-mapping".equals(localName)) {
                ResourceLibraryContractsContractMappingType contract_mapping = new ResourceLibraryContractsContractMappingType();
                parser.parse(contract_mapping);
                addContractsContractMappingType(contract_mapping);
                return true;
            }
            return false;
        }

        private void addContractsContractMappingType(ResourceLibraryContractsContractMappingType contract_mapping) {
            if (this.contract_mapping == null) {
                this.contract_mapping = new ResourceLibraryContractsContractMappingType.ListType();
            }
            this.contract_mapping.add(contract_mapping);
        }
    }

    // Flow Additions in XSD for JSF22

    /*
     * <xsd:complexType name="faces-config-flow-definitionType">
     *      <xsd:sequence>
     *       <xsd:group ref="javaee:descriptionGroup"/>
     *       <xsd:element name="start-node" type="javaee:java-identifierType" minOccurs="0" />
     *       <xsd:element name="view" type="javaee:faces-config-flow-definition-viewType" minOccurs="0" maxOccurs="unbounded" />
     *       <xsd:element name="switch" type="javaee:faces-config-flow-definition-switchType" minOccurs="0" maxOccurs="unbounded" />
     *       <xsd:element name="flow-return" type="javaee:faces-config-flow-definition-flow-returnType" minOccurs="0" maxOccurs="unbounded"/>
     *       <xsd:element name="navigation-rule" type="javaee:faces-config-navigation-ruleType" minOccurs="0" maxOccurs="unbounded"/>
     *       <xsd:element name="flow-call" type="javaee:faces-config-flow-definition-flow-callType" minOccurs="0" maxOccurs="unbounded"/>
     *       <xsd:element name="method-call" type="javaee:faces-config-flow-definition-faces-method-callType" minOccurs="0" maxOccurs="unbounded"/>
     *       <xsd:element name="initializer" type="javaee:faces-config-flow-definition-initializerType" minOccurs="0" />
     *       <xsd:element name="finalizer" type="javaee:faces-config-flow-definition-finalizerType" minOccurs="0" />
     *       <xsd:element name="inbound-parameter" type="javaee:faces-config-flow-definition-inbound-parameterType" minOccurs="0" maxOccurs="unbounded"/>
     *     </xsd:sequence>
     *     <xsd:attribute name="id" type="xsd:ID" use="required" />
     *   </xsd:complexType>
     */

    static class FlowDefinitionType extends DescriptionGroup implements FacesConfigFlowDefinition {

        // elems
        XSDTokenType start_node;
        FlowDefinitionViewType.ListType view;
        FlowDefinitionSwitchType.ListType switch_expr;
        FlowDefinitionFlowReturnType.ListType flow_return;
        NavigationRuleType.ListType navigation_rule;
        FlowDefinitionFlowCallType.ListType flow_call;
        FlowDefinitionFacesMethodCallType.ListType method_call;
        FlowDefinitionInitializerType initializer;
        FlowDefinitionFinalizerType finalizer;
        FlowDefinitionFlowCallInboundParameterType.ListType inbound_parameter;

        //atribute required
        IDType id;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws ParseException {
            if (nsURI == null) {
                if ("id".equals(localName)) {
                    id = parser.parseIDAttributeValue(index);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if (super.handleChild(parser, localName)) {
                return true;
            }
            if ("start-node".equals(localName)) {
                XSDTokenType start_node = new XSDTokenType();
                parser.parse(start_node);
                this.start_node = start_node;
                return true;
            }
            if ("view".equals(localName)) {
                FlowDefinitionViewType view = new FlowDefinitionViewType();
                parser.parse(view);
                addView(view);
                return true;
            }
            if ("switch".equals(localName)) {
                FlowDefinitionSwitchType switch_expr = new FlowDefinitionSwitchType();
                parser.parse(switch_expr);
                addSwitch(switch_expr);
                return true;
            }
            if ("flow-return".equals(localName)) {
                FlowDefinitionFlowReturnType flow_return = new FlowDefinitionFlowReturnType();
                parser.parse(flow_return);
                addFlowReturn(flow_return);
                return true;
            }
            if ("navigation-rule".equals(localName)) {
                NavigationRuleType navigation_rule = new NavigationRuleType();
                parser.parse(navigation_rule);
                addNavigationRule(navigation_rule);
                return true;
            }
            if ("flow-call".equals(localName)) {
                FlowDefinitionFlowCallType flow_call = new FlowDefinitionFlowCallType();
                parser.parse(flow_call);
                addFlowCall(flow_call);
                return true;
            }
            if ("method-call".equals(localName)) {
                FlowDefinitionFacesMethodCallType method_call = new FlowDefinitionFacesMethodCallType();
                parser.parse(method_call);
                addMethodCall(method_call);
                return true;
            }
            if ("initializer".equals(localName)) {
                FlowDefinitionInitializerType initializer = new FlowDefinitionInitializerType();
                parser.parse(initializer);
                this.initializer = initializer;
                return true;
            }
            if ("finalizer".equals(localName)) {
                FlowDefinitionFinalizerType finalizer = new FlowDefinitionFinalizerType();
                parser.parse(finalizer);
                this.finalizer = finalizer;
                return true;
            }
            if ("inbound-parameter".equals(localName)) {
                FlowDefinitionFlowCallInboundParameterType inbound_parameter = new FlowDefinitionFlowCallInboundParameterType();
                parser.parse(inbound_parameter);
                addInboundParameterType(inbound_parameter);
                return true;
            }

            return false;
        }

        private void addView(FlowDefinitionViewType view) {
            if (this.view == null) {
                this.view = new FlowDefinitionViewType.ListType();
            }
            this.view.add(view);
        }

        private void addSwitch(FlowDefinitionSwitchType switch_expr) {
            if (this.switch_expr == null) {
                this.switch_expr = new FlowDefinitionSwitchType.ListType();
            }
            this.switch_expr.add(switch_expr);
        }

        private void addFlowReturn(FlowDefinitionFlowReturnType flow_return) {
            if (this.flow_return == null) {
                this.flow_return = new FlowDefinitionFlowReturnType.ListType();
            }
            this.flow_return.add(flow_return);
        }

        private void addNavigationRule(NavigationRuleType navigation_rule) {
            if (this.navigation_rule == null) {
                this.navigation_rule = new NavigationRuleType.ListType();
            }
            this.navigation_rule.add(navigation_rule);
        }

        private void addFlowCall(FlowDefinitionFlowCallType flow_call) {
            if (this.flow_call == null) {
                this.flow_call = new FlowDefinitionFlowCallType.ListType();
            }
            this.flow_call.add(flow_call);
        }

        private void addMethodCall(FlowDefinitionFacesMethodCallType method_call) {
            if (this.method_call == null) {
                this.method_call = new FlowDefinitionFacesMethodCallType.ListType();
            }
            this.method_call.add(method_call);
        }

        private void addInboundParameterType(FlowDefinitionFlowCallInboundParameterType inbound_parameter) {
            if (this.inbound_parameter == null) {
                this.inbound_parameter = new FlowDefinitionFlowCallInboundParameterType.ListType();
            }
            this.inbound_parameter.add(inbound_parameter);
        }

        @Override
        public void describe(Diagnostics diag) {
            super.describe(diag);
            diag.describe("start-node", start_node);
            diag.describe("view", view);
            diag.describe("switch", switch_expr);
            diag.describe("flow-return", flow_return);
            diag.describe("navigation-rule", navigation_rule);
            diag.describe("flow-call", flow_call);
            diag.describe("method-call", method_call);
            diag.describe("initializer", initializer);
            diag.describe("finalizer", finalizer);
            diag.describe("inbound-parameter", inbound_parameter);
        }
    }

    /*
     * <xsd:complexType name="faces-config-flow-definition-faces-method-call-methodType">
     * <xsd:simpleContent>
     * <xsd:extension base="javaee:faces-config-el-expressionType">
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:extension>
     * </xsd:simpleContent>
     * </xsd:complexType>
     */
    static class FlowDefinitionFacesMethodCallMethodType extends ELExpressionType {
        @Override
        public boolean isIdAllowed() {
            return true;
        }
    }

    /*
     * <xsd:complexType name="faces-config-flow-definition-parameter-valueType">
     *  * <xsd:simpleContent>
     *  *    <xsd:extension base="javaee:faces-config-el-expressionType">
     *  *       <xsd:attribute name="id" type="xsd:ID"/>
     *  *    </xsd:extension>
     *  *  </xsd:simpleContent>
     *  * </xsd:complexType>
     */
    static class FlowDefinitionParameterValueType extends ELExpressionType {
        @Override
        public boolean isIdAllowed() {
            return true;
        }
    }

    /*
     * <xsd:complexType name="faces-config-flow-definition-flow-call-parameterType">
     * <xsd:sequence>
     * <xsd:element name="class" type="javaee:string"  minOccurs="0">
     * </xsd:element>
     * <xsd:element name="value"  type="javaee:faces-config-flow-definition-parameter-valueType" >
     * </xsd:sequence>
     * </xsd:complexType>
     */
    static class FlowDefinitionFlowCallParameterType extends DDParser.ElementContentParsable implements FacesConfigFlowDefinitionFlowCallParameter {

        // elems
        FlowDefinitionParameterValueType value;
        StringType clazz;

        public static class ListType extends ParsableListImplements<FlowDefinitionFlowCallParameterType, FacesConfigFlowDefinitionFlowCallParameter> {
            @Override
            public FlowDefinitionFlowCallParameterType newInstance(DDParser parser) {
                return new FlowDefinitionFlowCallParameterType();
            }
        }

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if ("value".equals(localName)) {
                FlowDefinitionParameterValueType value = new FlowDefinitionParameterValueType();
                parser.parse(value);
                this.value = value;
                return true;
            }
            if ("class".equals(localName)) {
                StringType clazz = new StringType();
                parser.parse(clazz);
                this.clazz = clazz;
                return true;
            }

            return false;
        }

        @Override
        public void describe(Diagnostics diag) {
            diag.describeIfSet("value", value);
            diag.describeIfSet("clazz", clazz);
        }
    }

    /*
     * <xsd:complexType name="faces-config-flow-definition-viewType">
     * <xsd:sequence>
     * <xsd:element name="vdl-document"  type="javaee:java-identifierType">
     * </xsd:element>
     * </xsd:sequence>
     * <xsd:attribute name="id"     type="xsd:ID"    use="required">
     * </xsd:attribute>
     * </xsd:complexType>
     */
    static class FlowDefinitionViewType extends DDParser.ElementContentParsable implements FacesConfigFlowDefinitionView {

        // elems
        XSDTokenType vdl_document;
        IDType id;

        public static class ListType extends ParsableListImplements<FlowDefinitionViewType, FacesConfigFlowDefinitionView> {
            @Override
            public FlowDefinitionViewType newInstance(DDParser parser) {
                return new FlowDefinitionViewType();
            }
        }

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws ParseException {
            if (nsURI == null) {
                if ("id".equals(localName)) {
                    id = parser.parseIDAttributeValue(index);
                    return true;
                }
            }
            return false;
        }

        /*
         * (non-Javadoc)
         *
         * @see com.ibm.ws.javaee.ddmodel.DDParser.ParsableElement#handleChild(com.ibm.ws.javaee.ddmodel.DDParser, java.lang.String)
         */
        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if ("vdl-document".equals(localName)) {
                XSDTokenType vdl_document = new XSDTokenType();
                parser.parse(vdl_document);
                this.vdl_document = vdl_document;
                return true;
            }
            return false;
        }

        /*
         * (non-Javadoc)
         *
         * @see com.ibm.ws.javaee.ddmodel.DDParser.Parsable#describe(com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics)
         */
        @Override
        public void describe(Diagnostics diag) {
            diag.describeIfSet("vdl-document", vdl_document);

        }
    }

    /*
     * <xsd:complexType name="faces-config-flow-definition-switchType">
     * <xsd:sequence>    
     * <xsd:element name="case"   type="javaee:faces-config-flow-definition-switch-caseType"  minOccurs="0"  maxOccurs="unbounded">          
     * </xsd:element>   
     * <xsd:element name="default-outcome"  type="javaee:string"   minOccurs="0"  >    
     * </xsd:element>  
     * </xsd:sequence>   
     * <xsd:attribute name="id"  type="xsd:ID"   use="required">          
     * </xsd:attribute>
     * </xsd:complexType>
     */
    static class FlowDefinitionSwitchType extends DDParser.ElementContentParsable implements FacesConfigFlowDefinitionSwitchType {
        //elems
        FlowDefinitionSwitchCaseType.ListType case_expr;
        StringType default_outcome;

        //atribute required
        IDType id;

        public static class ListType extends ParsableListImplements<FlowDefinitionSwitchType, FacesConfigFlowDefinitionSwitchType> {
            @Override
            public FlowDefinitionSwitchType newInstance(DDParser parser) {
                return new FlowDefinitionSwitchType();
            }
        }

        @Override
        public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws ParseException {
            if (nsURI == null) {
                if ("id".equals(localName)) {
                    id = parser.parseIDAttributeValue(index);
                    return true;
                }
            }
            return false;
        }

        /*
         * (non-Javadoc)
         *
         * @see com.ibm.ws.javaee.ddmodel.DDParser.ParsableElement#handleChild(com.ibm.ws.javaee.ddmodel.DDParser, java.lang.String)
         */
        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if ("case".equals(localName)) {
                FlowDefinitionSwitchCaseType case_expr = new FlowDefinitionSwitchCaseType();
                parser.parse(case_expr);
                addSwitchCaseType(case_expr);
                return true;
            }
            if ("default-outcome".equals(localName)) {
                StringType default_outcome = new StringType();
                parser.parse(default_outcome);
                this.default_outcome = default_outcome;
                return true;
            }
            return false;
        }

        private void addSwitchCaseType(FlowDefinitionSwitchCaseType case_expr) {
            if (this.case_expr == null) {
                this.case_expr = new FlowDefinitionSwitchCaseType.ListType();
            }
            this.case_expr.add(case_expr);
        }

        /*
         * (non-Javadoc)
         *
         * @see com.ibm.ws.javaee.ddmodel.DDParser.Parsable#describe(com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics)
         */
        @Override
        public void describe(Diagnostics diag) {
            diag.describeIfSet("case", case_expr);
            diag.describeIfSet("default-outcome", default_outcome);

        }

    }

    /*
     * <xsd:complexType name="faces-config-flow-definition-switch-caseType">
     * <xsd:sequence> 
     * <xsd:group ref="javaee:descriptionGroup"/>   
     * <xsd:element name="if" type="javaee:faces-config-ifType"minOccurs="0">      
     * </xsd:element>   
     * <xsd:element name="from-outcome" type="javaee:string"  minOccurs="0"> 
     * </xsd:element>  
     * </xsd:sequence>   
     * <xsd:attribute name="id"  type="xsd:ID">          
     * </xsd:attribute>
     * </xsd:complexType>
     */
    static class FlowDefinitionSwitchCaseType extends DescriptionGroup implements FacesConfigFlowDefinitionSwitchCase {

        // elems
        StringType from_outcome;
        ELExpressionWithIDType if_expr;

        public static class ListType extends ParsableListImplements<FlowDefinitionSwitchCaseType, FacesConfigFlowDefinitionSwitchCase> {
            @Override
            public FlowDefinitionSwitchCaseType newInstance(DDParser parser) {
                return new FlowDefinitionSwitchCaseType();
            }
        }

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if (super.handleChild(parser, localName)) {
                return true;
            }

            if ("from-outcome".equals(localName)) {
                StringType from_outcome = new StringType();
                parser.parse(from_outcome);
                this.from_outcome = from_outcome;
                return true;
            }
            if ("if".equals(localName)) {
                ELExpressionWithIDType if_expr = new ELExpressionWithIDType();
                parser.parse(if_expr);
                this.if_expr = if_expr;
                return true;
            }

            return false;
        }

        @Override
        public void describe(Diagnostics diag) {
            super.describe(diag);

            diag.describeIfSet("from-outcome", from_outcome);
            diag.describeIfSet("if", if_expr);

        }
    }

    /*
     * <xsd:complexType name="faces-config-flow-definition-flow-returnType">
     * <xsd:element name="from-outcome" type="javaee:string" />
     * <xsd:attribute name="id"  type="xsd:ID"   use="required" /> 
     * </xsd:complexType>
     */
    static class FlowDefinitionFlowReturnType extends DDParser.ElementContentParsable implements FacesConfigFlowDefinitionFlowReturn {

        // elems
        StringType from_outcome;
        //atribute required
        IDType id;

        public static class ListType extends ParsableListImplements<FlowDefinitionFlowReturnType, FacesConfigFlowDefinitionFlowReturn> {
            @Override
            public FlowDefinitionFlowReturnType newInstance(DDParser parser) {
                return new FlowDefinitionFlowReturnType();
            }
        }

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws ParseException {
            if (nsURI == null) {
                if ("id".equals(localName)) {
                    id = parser.parseIDAttributeValue(index);
                    return true;
                }
            }
            return false;
        }

        /*
         * (non-Javadoc)
         *
         * @see com.ibm.ws.javaee.ddmodel.DDParser.ParsableElement#handleChild(com.ibm.ws.javaee.ddmodel.DDParser, java.lang.String)
         */
        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if ("from-outcome".equals(localName)) {
                StringType from_outcome = new StringType();
                parser.parse(from_outcome);
                this.from_outcome = from_outcome;
                return true;
            }
            return false;
        }

        /*
         * (non-Javadoc)
         *
         * @see com.ibm.ws.javaee.ddmodel.DDParser.Parsable#describe(com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics)
         */
        @Override
        public void describe(Diagnostics diag) {
            diag.describeIfSet("from-outcome", from_outcome);

        }
    }

    /*
     * <xsd:complexType name="faces-config-flow-definition-flow-callType">
     * <xsd:element name="flow-reference" type="javaee:faces-config-flow-definition-flow-call-flow-referenceType" />
     * <xsd:element name="outbound-parameter"
     * type="javaee:faces-config-flow-definition-flow-call-outbound-parameterType"   minOccurs="0" maxOccurs="unbounded" />
     * <xsd:attribute name="id"  type="xsd:ID"   use="required" /> 
     * </xsd:complexType>
     */
    static class FlowDefinitionFlowCallType extends DDParser.ElementContentParsable implements FacesConfigFlowDefinitionFlowCall {
        FlowDefinitionFlowCallFlowReferenceType flow_reference;
        FlowDefinitionFlowCallOutboundParameterType.ListType outbound_parameter;

        //atribute required
        IDType id;

        public static class ListType extends ParsableListImplements<FlowDefinitionFlowCallType, FacesConfigFlowDefinitionFlowCall> {
            @Override
            public FlowDefinitionFlowCallType newInstance(DDParser parser) {
                return new FlowDefinitionFlowCallType();
            }
        }

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws ParseException {
            if (nsURI == null) {
                if ("id".equals(localName)) {
                    id = parser.parseIDAttributeValue(index);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if ("flow-reference".equals(localName)) {
                FlowDefinitionFlowCallFlowReferenceType flow_reference = new FlowDefinitionFlowCallFlowReferenceType();
                parser.parse(flow_reference);
                this.flow_reference = flow_reference;
                return true;
            }
            if ("outbound-parameter".equals(localName)) {
                FlowDefinitionFlowCallOutboundParameterType outbound_parameter = new FlowDefinitionFlowCallOutboundParameterType();
                parser.parse(outbound_parameter);
                addFlowCallOutboundParameterType(outbound_parameter);
                return true;
            }

            return false;
        }

        private void addFlowCallOutboundParameterType(FlowDefinitionFlowCallOutboundParameterType outbound_parameter) {
            if (this.outbound_parameter == null) {
                this.outbound_parameter = new FlowDefinitionFlowCallOutboundParameterType.ListType();
            }
            this.outbound_parameter.add(outbound_parameter);
        }

        @Override
        public void describe(Diagnostics diag) {
            diag.describeIfSet("flow-reference", flow_reference);
            diag.describeIfSet("outbound-parameter", outbound_parameter);
        }

    }

    /*
     * <xsd:complexType name="faces-config-flow-definition-flow-call-flow-referenceType">
     * <xsd:element name="flow-document-id" type="javaee:java-identifierType" minOccurs="0" />
     * <xsd:element name="flow-id" type="javaee:java-identifierType" />
     * </xsd:complexType>
     */
    static class FlowDefinitionFlowCallFlowReferenceType extends DDParser.ElementContentParsable implements FacesConfigFlowDefinitionFlowCallFlowReference {
        XSDTokenType flow_document_id;
        XSDTokenType flow_id;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if ("flow-document-id".equals(localName)) {
                XSDTokenType flow_document_id = new XSDTokenType();
                parser.parse(flow_document_id);
                this.flow_document_id = flow_document_id;
                return true;
            }
            if ("flow-id".equals(localName)) {
                XSDTokenType flow_id = new XSDTokenType();
                parser.parse(flow_id);
                this.flow_id = flow_id;
                return true;
            }

            return false;
        }

        @Override
        public void describe(Diagnostics diag) {
            diag.describeIfSet("flow-document-id", flow_document_id);
            diag.describeIfSet("flow-id", flow_id);
        }

    }

    /*
     * <xsd:complexType name="faces-config-flow-definition-flow-call-outbound-parameterType">
     * <xsd:element name="name" type="javaee:java-identifierType" />
     * <xsd:element name="value" type="javaee:faces-config-flow-definition-parameter-valueType" />
     * </xsd:complexType>
     */
    static class FlowDefinitionFlowCallOutboundParameterType extends DDParser.ElementContentParsable implements FacesConfigFlowDefinitionFlowCallOutboundParameter {
        XSDTokenType name;
        FlowDefinitionParameterValueType value;

        public static class ListType extends ParsableListImplements<FlowDefinitionFlowCallOutboundParameterType, FacesConfigFlowDefinitionFlowCallOutboundParameter> {
            @Override
            public FlowDefinitionFlowCallOutboundParameterType newInstance(DDParser parser) {
                return new FlowDefinitionFlowCallOutboundParameterType();
            }
        }

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if ("value".equals(localName)) {
                FlowDefinitionParameterValueType value = new FlowDefinitionParameterValueType();
                parser.parse(value);
                this.value = value;
                return true;
            }
            if ("name".equals(localName)) {
                XSDTokenType name = new XSDTokenType();
                parser.parse(name);
                this.name = name;
                return true;
            }

            return false;
        }

        @Override
        public void describe(Diagnostics diag) {
            diag.describeIfSet("value", value);
            diag.describeIfSet("name", name);
        }

    }

    /*
     * <xsd:complexType name="faces-config-flow-definition-flow-call-inbound-parameterType">
     * <xsd:element name="name" type="javaee:java-identifierType" />
     * <xsd:element name="value" type="javaee:faces-config-flow-definition-parameter-valueType" />
     * </xsd:complexType>
     */
    static class FlowDefinitionFlowCallInboundParameterType extends DDParser.ElementContentParsable implements FacesConfigFlowDefinitionFlowCallInboundParameter {
        XSDTokenType name;
        FlowDefinitionParameterValueType value;

        public static class ListType extends ParsableListImplements<FlowDefinitionFlowCallInboundParameterType, FacesConfigFlowDefinitionFlowCallInboundParameter> {
            @Override
            public FlowDefinitionFlowCallInboundParameterType newInstance(DDParser parser) {
                return new FlowDefinitionFlowCallInboundParameterType();
            }
        }

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if ("value".equals(localName)) {
                FlowDefinitionParameterValueType value = new FlowDefinitionParameterValueType();
                parser.parse(value);
                this.value = value;
                return true;
            }
            if ("name".equals(localName)) {
                XSDTokenType name = new XSDTokenType();
                parser.parse(name);
                this.name = name;
                return true;
            }

            return false;
        }

        @Override
        public void describe(Diagnostics diag) {
            diag.describeIfSet("value", value);
            diag.describeIfSet("name", name);
        }

    }

    /*
     * <xsd:complexType name="faces-config-flow-definition-faces-method-callType">
     *   <xsd:element name="method" type="javaee:faces-config-flow-definition-faces-method-call-methodType"  />
     *   <xsd:element name="default-outcome"  type="javaee:string" />
     *   <xsd:element name="parameter"  type="javaee:faces-config-flow-definition-flow-call-parameterType" minOccurs="0" maxOccurs="unbounded" />
     *   </xsd:complexType>
     */

    static class FlowDefinitionFacesMethodCallType extends DDParser.ElementContentParsable implements FacesConfigFlowDefinitionFacesMethodCall {
        //elements
        FlowDefinitionFacesMethodCallMethodType method;
        StringType default_outcome;
        FlowDefinitionFlowCallParameterType.ListType parameter;

        public static class ListType extends ParsableListImplements<FlowDefinitionFacesMethodCallType, FacesConfigFlowDefinitionFacesMethodCall> {
            @Override
            public FlowDefinitionFacesMethodCallType newInstance(DDParser parser) {
                return new FlowDefinitionFacesMethodCallType();
            }
        }

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {

            if ("method".equals(localName)) {
                FlowDefinitionFacesMethodCallMethodType method = new FlowDefinitionFacesMethodCallMethodType();
                parser.parse(method);
                this.method = method;
                return true;
            }
            if ("default-outcome".equals(localName)) {
                StringType default_outcome = new StringType();
                parser.parse(default_outcome);
                this.default_outcome = default_outcome;
                return true;
            }
            if ("parameter".equals(localName)) {
                FlowDefinitionFlowCallParameterType parameter = new FlowDefinitionFlowCallParameterType();
                parser.parse(parameter);
                addParameter(parameter);
                return true;
            }

            return false;
        }

        private void addParameter(FlowDefinitionFlowCallParameterType parameter) {
            if (this.parameter == null) {
                this.parameter = new FlowDefinitionFlowCallParameterType.ListType();
            }
            this.parameter.add(parameter);
        }

        @Override
        public void describe(Diagnostics diag) {
            diag.describeIfSet("method", method);
            diag.describeIfSet("default-outcome", default_outcome);
            diag.describeIfSet("parameter", parameter);
        }

    }

    /*
     * <xsd:complexType name="faces-config-flow-definition-initializerType">
     *  <xsd:simpleContent>
     *  <xsd:extension base="javaee:faces-config-el-expressionType">
     *  <xsd:attribute name="id" type="xsd:ID"/>
     *  </xsd:extension>
     * </xsd:simpleContent>
     *  </xsd:complexType>
     */
    static class FlowDefinitionInitializerType extends ELExpressionType {
        @Override
        public boolean isIdAllowed() {
            return true;
        }
    }

    /*
     * <xsd:complexType name="faces-config-flow-definition-finalizerType">
     *  <xsd:simpleContent>
     *  <xsd:extension base="javaee:faces-config-el-expressionType">
     *  <xsd:attribute name="id" type="xsd:ID"/>
     *  </xsd:extension>
     * </xsd:simpleContent>
     *  </xsd:complexType>
     */
    static class FlowDefinitionFinalizerType extends ELExpressionType {
        @Override
        public boolean isIdAllowed() {
            return true;
        }
    }

    /*
     * <xsd:complexType name="faces-config-protected-viewsType">
     * <xsd:element name="url-pattern" type="javaee:url-patternType" maxOccurs="unbounded"/>
     * </xsd:complexType>
     */
    static class ProtectedViewsType extends DescriptionGroup implements FacesConfigProtectedViews {

        // elements
        StringType.ListType url_pattern = new StringType.ListType();

        public static class ListType extends ParsableListImplements<ProtectedViewsType, FacesConfigProtectedViews> {
            @Override
            public ProtectedViewsType newInstance(DDParser parser) {
                return new ProtectedViewsType();
            }
        }

        @Override
        public List<String> getURLPatterns() {
            return url_pattern.getList();
        }

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {

            if ("url-pattern".equals(localName)) {
                StringType url_pattern = new StringType();
                parser.parse(url_pattern);
                this.url_pattern.add(url_pattern);
                return true;
            }

            return false;
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            diag.describe("url-pattern", url_pattern);

        }

    }

}
