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

import com.ibm.ws.javaee.dd.common.ConnectionFactory;
import com.ibm.ws.javaee.dd.common.Description;
import com.ibm.ws.javaee.dd.common.Property;
import com.ibm.ws.javaee.ddmodel.AnySimpleType;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParsableListImplements;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;

public class ConnectionFactoryType extends JNDIEnvironmentRefType implements ConnectionFactory {
    public static class ListType extends ParsableListImplements<ConnectionFactoryType, ConnectionFactory> {
        @Override
        public ConnectionFactoryType newInstance(DDParser parser) {
            return new ConnectionFactoryType();
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
    public String getResourceAdapter() {
        return resource_adapter != null ? resource_adapter.getValue() : null;
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

    @Override
    public int getTransactionSupportValue() {
        return transaction_support != null ? transaction_support.value.value : ConnectionFactory.TRANSACTION_SUPPORT_UNSPECIFIED;
    }

    @Override
    public List<Property> getProperties() {
        if (property != null) {
            return property.getList();
        } else {
            return Collections.emptyList();
        }
    }

    // elements
    private DescriptionType.ListType description;
    private XSDTokenType interface_name;
    private XSDTokenType resource_adapter;
    private XSDIntegerType max_pool_size;
    private XSDIntegerType min_pool_size;
    private TransactionSupportType transaction_support;
    private PropertyType.ListType property;

    public ConnectionFactoryType() {
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
        if ("resource-adapter".equals(localName)) {
            XSDTokenType resource_adapter = new XSDTokenType();
            parser.parse(resource_adapter);
            this.resource_adapter = resource_adapter;
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
        if ("transaction-support".equals(localName)) {
            TransactionSupportType transaction_support = new TransactionSupportType();
            parser.parse(transaction_support);
            this.transaction_support = transaction_support;
            return true;
        }
        if ("property".equals(localName)) {
            PropertyType property = new PropertyType();
            parser.parse(property);
            addProperty(property);
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
        diag.describeIfSet("resource-adapter", resource_adapter);
        diag.describeIfSet("max-pool-size", max_pool_size);
        diag.describeIfSet("min-pool-size", min_pool_size);
        diag.describeIfSet("transaction-support", transaction_support);
        diag.describeIfSet("property", property);
    }

    static enum TransactionSupportEnum {
        NoTransaction(ConnectionFactory.TRANSACTION_SUPPORT_NO_TRANSACTION),
        LocalTransaction(ConnectionFactory.TRANSACTION_SUPPORT_LOCAL_TRANSACTION),
        XATransaction(ConnectionFactory.TRANSACTION_SUPPORT_XA_TRANSACTION);

        final int value;

        TransactionSupportEnum(int value) {
            this.value = value;
        }
    }

    static class TransactionSupportType extends XSDTokenType {
        TransactionSupportEnum value;

        @Override
        public void finish(DDParser parser) throws ParseException {
            super.finish(parser);
            if (!isNil()) {
                value = parseEnumValue(parser, TransactionSupportEnum.class);
            }
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            diag.describeEnum(value);
        }
    }
}
