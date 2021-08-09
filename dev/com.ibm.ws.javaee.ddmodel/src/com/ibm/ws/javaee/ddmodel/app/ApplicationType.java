/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.app;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.ibm.ws.javaee.dd.app.Application;
import com.ibm.ws.javaee.dd.app.Module;
import com.ibm.ws.javaee.dd.common.Description;
import com.ibm.ws.javaee.dd.common.DisplayName;
import com.ibm.ws.javaee.dd.common.Icon;
import com.ibm.ws.javaee.dd.common.MessageDestination;
import com.ibm.ws.javaee.dd.common.SecurityRole;
import com.ibm.ws.javaee.ddmodel.AnySimpleType;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParsableListImplements;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.TokenType;
import com.ibm.ws.javaee.ddmodel.common.DescriptionType;
import com.ibm.ws.javaee.ddmodel.common.DisplayNameType;
import com.ibm.ws.javaee.ddmodel.common.IconType;
import com.ibm.ws.javaee.ddmodel.common.JNDIEnvironmentRefs;
import com.ibm.ws.javaee.ddmodel.common.MessageDestinationType;
import com.ibm.ws.javaee.ddmodel.common.SecurityRoleType;
import com.ibm.ws.javaee.ddmodel.common.XSDBooleanType;
import com.ibm.ws.javaee.ddmodel.common.XSDTokenType;

/*
 <xsd:element name="application"
 type="javaee:applicationType">
 <xsd:unique name="context-root-uniqueness">
 <xsd:selector xpath="javaee:module/javaee:web"/>
 <xsd:field xpath="javaee:context-root"/>
 </xsd:unique>
 <xsd:unique name="security-role-uniqueness">
 <xsd:selector xpath="javaee:security-role"/>
 <xsd:field xpath="javaee:role-name"/>
 </xsd:unique>
 </xsd:element>
 */

/*
 <xsd:complexType name="applicationType">
 <xsd:sequence>
 <xsd:element name="application-name"
 type="javaee:xsdTokenType"
 minOccurs="0"/>
 <xsd:group ref="javaee:descriptionGroup"/>
 <xsd:element name="initialize-in-order"
 type="javaee:generic-booleanType"
 minOccurs="0"
 maxOccurs="1">
 </xsd:element>
 <xsd:element name="module"
 type="javaee:moduleType"
 maxOccurs="unbounded">
 </xsd:element>
 <xsd:element name="security-role"
 type="javaee:security-roleType"
 minOccurs="0"
 maxOccurs="unbounded"/>
 <xsd:element name="library-directory"
 type="javaee:pathType"
 minOccurs="0"
 maxOccurs="1">
 </xsd:element>
 <xsd:element name="env-entry"
 type="javaee:env-entryType"
 minOccurs="0"
 maxOccurs="unbounded"/>
 <xsd:element name="ejb-ref"
 type="javaee:ejb-refType"
 minOccurs="0"
 maxOccurs="unbounded"/>
 <xsd:element name="ejb-local-ref"
 type="javaee:ejb-local-refType"
 minOccurs="0"
 maxOccurs="unbounded"/>
 <xsd:group ref="javaee:service-refGroup"/>
 <xsd:element name="resource-ref"
 type="javaee:resource-refType"
 minOccurs="0"
 maxOccurs="unbounded"/>
 <xsd:element name="resource-env-ref"
 type="javaee:resource-env-refType"
 minOccurs="0"
 maxOccurs="unbounded"/>
 <xsd:element name="message-destination-ref"
 type="javaee:message-destination-refType"
 minOccurs="0"
 maxOccurs="unbounded"/>
 <xsd:element name="persistence-context-ref"
 type="javaee:persistence-context-refType"
 minOccurs="0"
 maxOccurs="unbounded"/>
 <xsd:element name="persistence-unit-ref"
 type="javaee:persistence-unit-refType"
 minOccurs="0"
 maxOccurs="unbounded"/>
 <xsd:element name="message-destination"
 type="javaee:message-destinationType"
 minOccurs="0"
 maxOccurs="unbounded"/>
 <xsd:element name="data-source"
 type="javaee:data-sourceType"
 minOccurs="0"
 maxOccurs="unbounded"/>
 </xsd:sequence>
 <xsd:attribute name="version"
 type="javaee:dewey-versionType"
 fixed="6"
 use="required">
 </xsd:attribute>
 <xsd:attribute name="id"
 type="xsd:ID"/>
 </xsd:complexType>

 <xsd:simpleType name="dewey-versionType">
 <xsd:restriction base="xsd:token">
 <xsd:pattern value="\.?[0-9]+(\.[0-9]+)*"/>
 </xsd:restriction>
 </xsd:simpleType>
 */
public class ApplicationType extends JNDIEnvironmentRefs implements Application, DDParser.RootParsable {
    public ApplicationType(String path) {
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
    public String getApplicationName() {
        return application_name != null ? application_name.getValue() : null;
    }

    @Override
    public List<Description> getDescriptions() {
        if (description != null) {
            return description.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<DisplayName> getDisplayNames() {
        if (display_name != null) {
            return display_name.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<Icon> getIcons() {
        if (icon != null) {
            return icon.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public boolean isSetInitializeInOrder() {
        return AnySimpleType.isSet(initialize_in_order);
    }

    @Override
    public boolean isInitializeInOrder() {
        if (initialize_in_order != null) {
            return initialize_in_order.getBooleanValue();
        } else {
            return false;
        }
    }

    @Override
    public List<Module> getModules() {
        return module.getList();
    }

    @Override
    public List<SecurityRole> getSecurityRoles() {
        if (security_role != null) {
            return security_role.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public String getLibraryDirectory() {
        return library_directory != null ? library_directory.getValue() : null;
    }

    @Override
    public List<MessageDestination> getMessageDestinations() {
        if (message_destination != null) {
            return message_destination.getList();
        } else {
            return Collections.emptyList();
        }
    }

    // attributes
    TokenType version;
    // elements
    XSDTokenType application_name;
    DescriptionType.ListType description;
    DisplayNameType.ListType display_name;
    IconType.ListType icon;
    GenericBooleanType initialize_in_order;
    ModuleType.ListType module = new ModuleType.ListType();
    SecurityRoleType.ListType security_role;
    XSDTokenType library_directory;
    MessageDestinationType.ListType message_destination;

    // unique context-root-uniqueness
    Map<XSDTokenType, WebType> contextRootToWebMap;
    // unique security-role-uniqueness
    Map<XSDTokenType, SecurityRoleType> roleNameToSecurityRoleMap;
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
            if (parser.version >= 14 && "version".equals(localName)) {
                version = parser.parseTokenAttributeValue(index);
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
        if ("application-name".equals(localName)) {
            XSDTokenType application_name = new XSDTokenType();
            parser.parse(application_name);
            this.application_name = application_name;
            return true;
        }
        if ("description".equals(localName)) {
            DescriptionType description = new DescriptionType();
            parser.parse(description);
            addDescription(description);
            return true;
        }
        if ("display-name".equals(localName)) {
            DisplayNameType display_name = new DisplayNameType();
            parser.parse(display_name);
            addDisplayName(display_name);
            return true;
        }
        if ("icon".equals(localName)) {
            IconType icon = new IconType();
            parser.parse(icon);
            addIcon(icon);
            return true;
        }
        if ("initialize-in-order".equals(localName)) {
            GenericBooleanType initialize_in_order = new GenericBooleanType();
            parser.parse(initialize_in_order);
            this.initialize_in_order = initialize_in_order;
            return true;
        }
        if ("module".equals(localName)) {
            ModuleType module = new ModuleType();
            parser.parse(module);
            this.module.add(module);
            return true;
        }
        if ("security-role".equals(localName)) {
            SecurityRoleType security_role = new SecurityRoleType();
            parser.parse(security_role);
            addSecurityRole(security_role);
            return true;
        }
        if ("library-directory".equals(localName)) {
            XSDTokenType library_directory = new XSDTokenType();
            parser.parse(library_directory);
            this.library_directory = library_directory;
            return true;
        }
        if ("message-destination".equals(localName)) {
            MessageDestinationType message_destination = new MessageDestinationType();
            parser.parse(message_destination);
            addMessageDestination(message_destination);
            return true;
        }
        return false;
    }

    private void addDescription(DescriptionType description) {
        if (this.description == null) {
            this.description = new DescriptionType.ListType();
        }
        this.description.add(description);
    }

    private void addDisplayName(DisplayNameType display_name) {
        if (this.display_name == null) {
            this.display_name = new DisplayNameType.ListType();
        }
        this.display_name.add(display_name);
    }

    private void addIcon(IconType icon) {
        if (this.icon == null) {
            this.icon = new IconType.ListType();
        }
        this.icon.add(icon);
    }

    private void addSecurityRole(SecurityRoleType security_role) {
        if (this.security_role == null) {
            this.security_role = new SecurityRoleType.ListType();
        }
        this.security_role.add(security_role);
    }

    private void addMessageDestination(MessageDestinationType message_destination) {
        if (this.message_destination == null) {
            this.message_destination = new MessageDestinationType.ListType();
        }
        this.message_destination.add(message_destination);
    }

    @Override
    public void finish(DDParser parser) throws ParseException {
        if (version == null) {
            if (parser.version < 14) {
                version = parser.parseToken(parser.version == 12 ? "1.2" : "1.3");
            } else {
                throw new ParseException(parser.requiredAttributeMissing("version"));
            }
        }
        this.idMap = parser.idMap;
    }

    @Override
    public void describe(DDParser.Diagnostics diag) {
        diag.describe("version", version);
        diag.describeIfSet("application-name", application_name);
        diag.describeIfSet("description", description);
        diag.describeIfSet("display-name", display_name);
        diag.describeIfSet("icon", icon);
        diag.describeIfSet("initialize-in-order", initialize_in_order);
        diag.describe("module", module);
        diag.describeIfSet("security-role", security_role);
        diag.describeIfSet("library-directory", library_directory);
        super.describe(diag);
        diag.describeIfSet("message-destination", message_destination);
    }

    @Override
    protected String toTracingSafeString() {
        return "application";
    }

    @Override
    public void describe(StringBuilder sb) {
        DDParser.Diagnostics diag = new DDParser.Diagnostics(idMap, sb);
        diag.describe(toTracingSafeString(), this);
    }

    /*
     * <xsd:complexType name="moduleType">
     * <xsd:sequence>
     * <xsd:choice>
     * <xsd:element name="connector"
     * type="javaee:pathType">
     * </xsd:element>
     * <xsd:element name="ejb"
     * type="javaee:pathType">
     * </xsd:element>
     * <xsd:element name="java"
     * type="javaee:pathType">
     * </xsd:element>
     * <xsd:element name="web"
     * type="javaee:webType"/>
     * </xsd:choice>
     * <xsd:element name="alt-dd"
     * type="javaee:pathType"
     * minOccurs="0">
     * </xsd:element>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */

    public static class ModuleType extends DDParser.ElementContentParsable implements Module {

        public static class ListType extends ParsableListImplements<ModuleType, Module> {
            @Override
            public ModuleType newInstance(DDParser parser) {
                return new ModuleType();
            }
        }

        @Override
        public int getModuleType() {
            switch (choiceType) {
                case CONNECTOR:
                    return TYPE_CONNECTOR;
                case EJB:
                    return TYPE_EJB;
                case JAVA:
                    return TYPE_JAVA;
                default: // case WEB:
                    return TYPE_WEB;
            }
        }

        @Override
        public String getModulePath() {
            if (choiceType == ModuleTypeEnum.WEB) {
                return ((WebType) choice).web_uri.getValue();
            } else {
                return ((XSDTokenType) choice).getValue();
            }
        }

        @Override
        public String getContextRoot() {
            return choiceType == ModuleTypeEnum.WEB ? ((WebType) choice).context_root.getValue() : null;
        }

        @Override
        public String getAltDD() {
            return alt_dd != null ? alt_dd.getValue() : null;
        }

        public static enum ModuleTypeEnum {
            CONNECTOR,
            EJB,
            JAVA,
            WEB
        }

        // elements
        ModuleTypeEnum choiceType;
        DDParser.ParsableElement choice; // XSDTokenType or WebType
        XSDTokenType alt_dd;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if ("connector".equals(localName)) {
                XSDTokenType connector = new XSDTokenType();
                parser.parse(connector);
                choiceType = ModuleTypeEnum.CONNECTOR;
                choice = connector;
                return true;
            }
            if ("ejb".equals(localName)) {
                XSDTokenType ejb = new XSDTokenType();
                parser.parse(ejb);
                choiceType = ModuleTypeEnum.EJB;
                choice = ejb;
                return true;
            }
            if ("java".equals(localName)) {
                XSDTokenType java = new XSDTokenType();
                parser.parse(java);
                choiceType = ModuleTypeEnum.JAVA;
                choice = java;
                return true;
            }
            if ("web".equals(localName)) {
                WebType web = new WebType();
                parser.parse(web);
                choiceType = ModuleTypeEnum.WEB;
                choice = web;
                return true;
            }
            if ("alt-dd".equals(localName)) {
                XSDTokenType alt_dd = new XSDTokenType();
                parser.parse(alt_dd);
                this.alt_dd = alt_dd;
                return true;
            }
            return false;
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            if (choice != null) {
                switch (choiceType) {
                    case CONNECTOR:
                        diag.describe("connector", choice);
                        break;
                    case EJB:
                        diag.describe("ejb", choice);
                        break;
                    case JAVA:
                        diag.describe("java", choice);
                        break;
                    case WEB:
                        diag.describe("web", choice);
                        break;
                }
            }
            diag.describeIfSet("alt-dd", alt_dd);
        }
    }

    /*
     * <xsd:complexType name="webType">
     * <xsd:sequence>
     * <xsd:element name="web-uri"
     * type="javaee:pathType">
     * </xsd:element>
     * <xsd:element name="context-root"
     * type="javaee:string">
     * </xsd:element>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */

    public static class WebType extends DDParser.ElementContentParsable {
        // elements
        XSDTokenType web_uri = new XSDTokenType();
        XSDTokenType context_root = new XSDTokenType();

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if ("web-uri".equals(localName)) {
                parser.parse(web_uri);
                return true;
            }
            if ("context-root".equals(localName)) {
                parser.parse(context_root);
                return true;
            }
            return false;
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            diag.describe("web-uri", web_uri);
            diag.describe("context-root", context_root);
        }
    }

    /*
     * <xsd:complexType name="generic-booleanType">
     * <xsd:simpleContent>
     * <xsd:restriction base="javaee:xsdTokenType">
     * <xsd:enumeration value="true"/>
     * <xsd:enumeration value="false"/>
     * <xsd:enumeration value="yes"/>
     * <xsd:enumeration value="no"/>
     * </xsd:restriction>
     * </xsd:simpleContent>
     * </xsd:complexType>
     */
    public static class GenericBooleanType extends XSDBooleanType {
        @Override
        protected void setValueFromLexical(DDParser parser, String lexical) throws ParseException {
            if ("true".equals(lexical) || "yes".equals(lexical)) {
                value = Boolean.TRUE;
            } else if ("false".equals(lexical) || "no".equals(lexical)) {
                value = Boolean.FALSE;
            } else {
                throw new ParseException(parser.invalidEnumValue(lexical, "true", "yes", "false", "no"));
            }
        }
    }
}
