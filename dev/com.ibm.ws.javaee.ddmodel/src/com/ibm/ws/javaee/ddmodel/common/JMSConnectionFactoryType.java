/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.common;

import java.util.Collections;
import java.util.List;

import com.ibm.ws.javaee.dd.common.Description;
import com.ibm.ws.javaee.dd.common.JMSConnectionFactory;
import com.ibm.ws.javaee.dd.common.Property;
import com.ibm.ws.javaee.ddmodel.AnySimpleType;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParsableListImplements;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;

public class JMSConnectionFactoryType extends JNDIEnvironmentRefType implements JMSConnectionFactory {
    public static class ListType extends ParsableListImplements<JMSConnectionFactoryType, JMSConnectionFactory> {
        @Override
        public JMSConnectionFactoryType newInstance(DDParser parser) {
            return new JMSConnectionFactoryType();
        }
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
    public String getInterfaceNameValue() {
        return interface_name != null ? interface_name.getValue() : null;
    }

    @Override
    public String getClassNameValue() {
        return class_name != null ? class_name.getValue() : null;
    }

    @Override
    public String getResourceAdapter() {
        return resource_adapter != null ? resource_adapter.getValue() : null;
    }

    @Override
    public String getUser() {
        return user != null ? user.getValue() : null;
    }

    @Override
    public String getPassword() {
        return password != null ? password.getValue() : null;
    }

    @Override
    public String getClientId() {
        return client_id != null ? client_id.getValue() : null;
    }

    @Override
    public List<Property> getProperties() {
        if (property != null) {
            return property.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public boolean isSetTransactional() {
        return AnySimpleType.isSet(transactional);
    }

    @Override
    public boolean isTransactional() {
        return transactional != null ? transactional.getBooleanValue() : false;
    }

    @Override
    public boolean isSetMaxPoolSize() {
        return AnySimpleType.isSet(max_pool_size);
    }

    @Override
    public int getMaxPoolSize() {
        return max_pool_size != null ? max_pool_size.getIntValue() : 0;
    }

    @Override
    public boolean isSetMinPoolSize() {
        return AnySimpleType.isSet(min_pool_size);
    }

    @Override
    public int getMinPoolSize() {
        return min_pool_size != null ? min_pool_size.getIntValue() : 0;
    }

    // elements
    private DescriptionType.ListType description;
    private XSDTokenType interface_name;
    private XSDTokenType class_name;
    private XSDTokenType resource_adapter;
    private XSDTokenType user;
    private XSDTokenType password;
    private XSDTokenType client_id;
    private PropertyType.ListType property;
    private XSDBooleanType transactional;
    private XSDIntegerType max_pool_size;
    private XSDIntegerType min_pool_size;

    public JMSConnectionFactoryType() {
        super("name");
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
        if ("description".equals(localName)) {
            DescriptionType description = new DescriptionType();
            parser.parse(description);
            addDescription(description);
            return true;
        }
        if ("interface-name".equals(localName)) {
            XSDTokenType interface_name = new XSDTokenType();
            parser.parse(interface_name);
            this.interface_name = interface_name;
            return true;
        }
        if ("class-name".equals(localName)) {
            XSDTokenType class_name = new XSDTokenType();
            parser.parse(class_name);
            this.class_name = class_name;
            return true;
        }
        if ("resource-adapter".equals(localName)) {
            XSDTokenType resource_adapter = new XSDTokenType();
            parser.parse(resource_adapter);
            this.resource_adapter = resource_adapter;
            return true;
        }
        if ("user".equals(localName)) {
            XSDTokenType user = new XSDTokenType();
            parser.parse(user);
            this.user = user;
            return true;
        }
        if ("password".equals(localName)) {
            XSDTokenType password = new XSDTokenType();
            parser.parse(password);
            this.password = password;
            return true;
        }
        if ("client-id".equals(localName)) {
            XSDTokenType client_id = new XSDTokenType();
            parser.parse(client_id);
            this.client_id = client_id;
            return true;
        }
        if ("property".equals(localName)) {
            PropertyType property = new PropertyType();
            parser.parse(property);
            addProperty(property);
            return true;
        }
        if ("transactional".equals(localName)) {
            XSDBooleanType transactional = new XSDBooleanType();
            parser.parse(transactional);
            this.transactional = transactional;
            return true;
        }
        if ("max-pool-size".equals(localName)) {
            XSDIntegerType max_pool_size = new XSDIntegerType();
            parser.parse(max_pool_size);
            this.max_pool_size = max_pool_size;
            return true;
        }
        if ("min-pool-size".equals(localName)) {
            XSDIntegerType min_pool_size = new XSDIntegerType();
            parser.parse(min_pool_size);
            this.min_pool_size = min_pool_size;
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

    private void addProperty(PropertyType property) {
        if (this.property == null) {
            this.property = new PropertyType.ListType();
        }
        this.property.add(property);
    }

    @Override
    public void describe(DDParser.Diagnostics diag) {
        diag.describeIfSet("description", description);
        super.describe(diag);
        diag.describeIfSet("interface-name", interface_name);
        diag.describeIfSet("class-name", class_name);
        diag.describeIfSet("resource-adapter", resource_adapter);
        diag.describeIfSet("user", user);
        diag.describeIfSet("password", password);
        diag.describeIfSet("client-id", client_id);
        diag.describeIfSet("property", property);
        diag.describeIfSet("transactional", transactional);
        diag.describeIfSet("max-pool-size", max_pool_size);
        diag.describeIfSet("min-pool-size", min_pool_size);
    }
}
