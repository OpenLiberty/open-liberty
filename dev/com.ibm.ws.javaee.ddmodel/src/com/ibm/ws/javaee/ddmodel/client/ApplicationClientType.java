/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.client;

import java.util.Collections;
import java.util.List;

import com.ibm.ws.javaee.dd.DeploymentDescriptor;
import com.ibm.ws.javaee.dd.client.ApplicationClient;
import com.ibm.ws.javaee.dd.common.Description;
import com.ibm.ws.javaee.dd.common.DisplayName;
import com.ibm.ws.javaee.dd.common.Icon;
import com.ibm.ws.javaee.dd.common.MessageDestination;
import com.ibm.ws.javaee.ddmodel.AnySimpleType;
import com.ibm.ws.javaee.ddmodel.BooleanType;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.TokenType;
import com.ibm.ws.javaee.ddmodel.common.DescriptionType;
import com.ibm.ws.javaee.ddmodel.common.DisplayNameType;
import com.ibm.ws.javaee.ddmodel.common.IconType;
import com.ibm.ws.javaee.ddmodel.common.JNDIEnvironmentRefsGroup;
import com.ibm.ws.javaee.ddmodel.common.MessageDestinationType;
import com.ibm.ws.javaee.ddmodel.common.XSDTokenType;

/*
 <xsd:element name="application-client"
 type="javaee:application-clientType">
 <xsd:unique name="env-entry-name-uniqueness">
 <xsd:selector xpath="javaee:env-entry"/>
 <xsd:field xpath="javaee:env-entry-name"/>
 </xsd:unique>
 <xsd:unique name="ejb-ref-name-uniqueness">
 <xsd:selector xpath="javaee:ejb-ref"/>
 <xsd:field xpath="javaee:ejb-ref-name"/>
 </xsd:unique>
 <xsd:unique name="res-ref-name-uniqueness">
 <xsd:selector xpath="javaee:resource-ref"/>
 <xsd:field xpath="javaee:res-ref-name"/>
 </xsd:unique>
 <xsd:unique name="resource-env-ref-uniqueness">
 <xsd:selector xpath="javaee:resource-env-ref"/>
 <xsd:field xpath="javaee:resource-env-ref-name"/>
 </xsd:unique>
 <xsd:unique name="message-destination-ref-uniqueness">
 <xsd:selector xpath="javaee:message-destination-ref"/>
 <xsd:field xpath="javaee:message-destination-ref-name"/>
 </xsd:unique>
 </xsd:element>

 <xsd:complexType name="application-clientType">
 <xsd:sequence>
 <xsd:element name="module-name"
 type="javaee:string"
 minOccurs="0"/>
 <xsd:group ref="javaee:descriptionGroup"/>
 <xsd:element name="env-entry"
 type="javaee:env-entryType"
 minOccurs="0"
 maxOccurs="unbounded"/>
 <xsd:element name="ejb-ref"
 type="javaee:ejb-refType"
 minOccurs="0"
 maxOccurs="unbounded"/>
 // <xsd:group ref="javaee:service-refGroup"/>
 <xsd:element name="service-ref"
 type="javaee:service-refType"
 minOccurs="0"
 maxOccurs="unbounded">
 <xsd:key name="service-ref_handler-name-key">
 <xsd:selector xpath="javaee:handler"/>
 <xsd:field xpath="javaee:handler-name"/>
 </xsd:key>
 </xsd:element>
 // <xsd:group ref="javaee:service-refGroup"/>
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
 <xsd:element name="persistence-unit-ref"
 type="javaee:persistence-unit-refType"
 minOccurs="0"
 maxOccurs="unbounded"/>
 <xsd:element name="post-construct"
 type="javaee:lifecycle-callbackType"
 minOccurs="0"
 maxOccurs="unbounded"/>
 <xsd:element name="pre-destroy"
 type="javaee:lifecycle-callbackType"
 minOccurs="0"
 maxOccurs="unbounded"/>
 <xsd:element name="callback-handler"
 type="javaee:fully-qualified-classType"
 minOccurs="0"/>
 <xsd:element name="message-destination"
 type="javaee:message-destinationType"
 minOccurs="0"
 maxOccurs="unbounded"/>
 <xsd:element name="data-source"
 type="javaee:data-sourceType"
 minOccurs="0"
 maxOccurs="unbounded"/>
 <xsd:element name="jms-connection-factory"
 type="javaee:jms-connection-factoryType"
 minOccurs="0"
 maxOccurs="unbounded"/>
 <xsd:element name="jms-destination"
 type="javaee:jms-destinationType"
 minOccurs="0"
 maxOccurs="unbounded"/>
 <xsd:element name="mail-session"
 type="javaee:mail-sessionType"
 minOccurs="0"
 maxOccurs="unbounded"/>
 <xsd:element name="connection-factory"
 type="javaee:connection-factory-resourceType"
 minOccurs="0"
 maxOccurs="unbounded"/>
 <xsd:element name="administered-object"
 type="javaee:administered-objectType"
 minOccurs="0"
 maxOccurs="unbounded"/>
 </xsd:sequence>
 <xsd:attribute name="version"
 type="javaee:dewey-versionType"
 fixed="7"
 use="required"/>
 <xsd:attribute name="metadata-complete"
 type="xsd:boolean"/>
 <xsd:attribute name="id"
 type="xsd:ID"/>
 </xsd:complexType>
 */
public class ApplicationClientType extends JNDIEnvironmentRefsGroup implements ApplicationClient, DDParser.RootParsable {
    public ApplicationClientType(String path) {
        this.path = path;
    }

    @Override
    protected boolean isEJBLocalRefSupported() {
        return false;
    }

    @Override
    protected boolean isPersistenceContextRefSupported() {
        return false;
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
    public String getModuleName() {
        return module_name != null ? module_name.getValue() : null;
    }

    @Override
    public boolean isSetMetadataComplete() {
        return AnySimpleType.isSet(metadata_complete);
    }

    @Override
    public boolean isMetadataComplete() {
        return metadata_complete != null && metadata_complete.getBooleanValue();
    }

    @Override
    public String getVersion() {
        return version.getValue();
    }

    @Override
    public int getVersionID() {
        return versionId;
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
    public String getCallbackHandler() {
        return callback_handler != null ? callback_handler.getValue() : null;
    }

    @Override
    public List<MessageDestination> getMessageDestinations() {
        if (message_destination != null) {
            return message_destination.getList();
        } else {
            return Collections.emptyList();
        }
    }

    private final String path;
    private int versionId;
    // attributes
    private TokenType version;
    private BooleanType metadata_complete;
    // elements
    private DescriptionType.ListType description;
    private DisplayNameType.ListType display_name;
    private IconType.ListType icon;
    private IconType compatIcon;
    private XSDTokenType module_name;
    private XSDTokenType callback_handler;
    private MessageDestinationType.ListType message_destination;

    // key env-entry-name
    // <xsd:selector xpath="javaee:env-entry"/>
    // <xsd:field xpath="javaee:env-entry-name"/>
    // key ejb-ref-name
    // <xsd:selector xpath="javaee:ejb-ref"/>
    // <xsd:field xpath="javaee:ejb-ref-name"/>
    // key res-ref-name
    // <xsd:selector xpath="javaee:resource-ref"/>
    // <xsd:field xpath="javaee:res-ref-name"/>
    // key resource-env-ref
    // <xsd:selector xpath="javaee:resource-env-ref"/>
    // <xsd:field xpath="javaee:resource-env-ref-name"/>
    // key message-destination-ref
    // <xsd:selector xpath="javaee:message-destination-ref"/>
    // <xsd:field xpath="javaee:message-destination-ref-name"/>

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
            if (parser.version >= 50 && "metadata-complete".equals(localName)) {
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
            if (parser.version < 14) {
                version = parser.parseToken(parser.version == 12 ? "1.2" : "1.3");
            } else {
                throw new ParseException(parser.requiredAttributeMissing("version"));
            }
        }
        this.versionId = parser.version;
        this.idMap = parser.idMap;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws ParseException {
        if (super.handleChild(parser, localName)) {
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
        if (parser.version < 14 && ("small-icon".equals(localName) || "large-icon".equals(localName))) {
            if (compatIcon == null) {
                compatIcon = new IconType();
                addIcon(compatIcon);
            }
            return compatIcon.handleChild(parser, localName);
        }
        if ("icon".equals(localName)) {
            IconType icon = new IconType();
            parser.parse(icon);
            addIcon(icon);
            return true;
        }
        if ("module-name".equals(localName)) {
            XSDTokenType module_name = new XSDTokenType();
            parser.parse(module_name);
            this.module_name = module_name;
            return true;
        }
        if ("callback-handler".equals(localName)) {
            XSDTokenType callback_handler = new XSDTokenType();
            parser.parse(callback_handler);
            this.callback_handler = callback_handler;
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

    protected void addIcon(IconType icon) {
        if (this.icon == null) {
            this.icon = new IconType.ListType();
        }
        this.icon.add(icon);
    }

    private void addMessageDestination(MessageDestinationType message_destination) {
        if (this.message_destination == null) {
            this.message_destination = new MessageDestinationType.ListType();
        }
        this.message_destination.add(message_destination);
    }

    @Override
    public void describe(DDParser.Diagnostics diag) {
        diag.describe("version", version);
        diag.describeIfSet("metadata-complete", metadata_complete);
        diag.describeIfSet("module-name", module_name);
        diag.describeIfSet("description", description);
        diag.describeIfSet("display-name", display_name);
        diag.describeIfSet("icon", icon);
        super.describe(diag);
        diag.describeIfSet("callback-handler", callback_handler);
        diag.describeIfSet("message-destination", message_destination);
    }

    @Override
    protected String toTracingSafeString() {
        return "application-client";
    }

    @Override
    public void describe(StringBuilder sb) {
        DDParser.Diagnostics diag = new DDParser.Diagnostics(idMap, sb);
        diag.describe(toTracingSafeString(), this);
    }
}
