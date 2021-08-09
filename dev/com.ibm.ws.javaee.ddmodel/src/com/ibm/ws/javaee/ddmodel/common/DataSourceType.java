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
package com.ibm.ws.javaee.ddmodel.common;

import java.sql.Connection;
import java.util.Collections;
import java.util.List;

import com.ibm.ws.javaee.dd.common.DataSource;
import com.ibm.ws.javaee.dd.common.Property;
import com.ibm.ws.javaee.ddmodel.AnySimpleType;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParsableListImplements;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;

/*
 <xsd:complexType name="data-sourceType">
 <xsd:sequence>
 <xsd:element name="description"
 type="javaee:descriptionType"
 minOccurs="0">
 </xsd:element>
 <xsd:element name="name"
 type="javaee:jndi-nameType">
 </xsd:element>
 <xsd:element name="class-name"
 type="javaee:fully-qualified-classType"
 minOccurs="0">
 </xsd:element>
 <xsd:element name="server-name"
 type="javaee:xsdTokenType"
 minOccurs="0">
 </xsd:element>
 <xsd:element name="port-number"
 type="javaee:xsdIntegerType"
 minOccurs="0">
 </xsd:element>
 <xsd:element name="database-name"
 type="javaee:xsdTokenType"
 minOccurs="0">
 </xsd:element>
 <xsd:element name="url"
 type="javaee:jdbc-urlType"
 minOccurs="0">
 </xsd:element>
 <xsd:element name="user"
 type="javaee:xsdTokenType"
 minOccurs="0">
 </xsd:element>
 <xsd:element name="password"
 type="javaee:xsdTokenType"
 minOccurs="0">
 </xsd:element>
 <xsd:element name="property"
 type="javaee:propertyType"
 minOccurs="0"
 maxOccurs="unbounded">
 </xsd:element>
 <xsd:element name="login-timeout"
 type="javaee:xsdIntegerType"
 minOccurs="0">
 </xsd:element>
 <xsd:element name="transactional"
 type="javaee:xsdBooleanType"
 minOccurs="0">
 </xsd:element>
 <xsd:element name="isolation-level"
 type="javaee:isolation-levelType"
 minOccurs="0">
 </xsd:element>
 <xsd:element name="initial-pool-size"
 type="javaee:xsdIntegerType"
 minOccurs="0">
 </xsd:element>
 <xsd:element name="max-pool-size"
 type="javaee:xsdIntegerType"
 minOccurs="0">
 </xsd:element>
 <xsd:element name="min-pool-size"
 type="javaee:xsdIntegerType"
 minOccurs="0">
 </xsd:element>
 <xsd:element name="max-idle-time"
 type="javaee:xsdIntegerType"
 minOccurs="0">
 </xsd:element>
 <xsd:element name="max-statements"
 type="javaee:xsdIntegerType"
 minOccurs="0">
 </xsd:element>
 </xsd:sequence>
 <xsd:attribute name="id"
 type="xsd:ID"/>
 </xsd:complexType>
 */

public class DataSourceType extends JNDIEnvironmentRefType implements DataSource {

    public static class ListType extends ParsableListImplements<DataSourceType, DataSource> {
        @Override
        public DataSourceType newInstance(DDParser parser) {
            return new DataSourceType();
        }
    }

    @Override
    public String getDescription() {
        return description != null ? description.getValue() : null;
    }

    @Override
    public String getClassNameValue() {
        return class_name != null ? class_name.getValue() : null;
    }

    @Override
    public String getServerName() {
        return server_name != null ? server_name.getValue() : null;
    }

    @Override
    public boolean isSetPortNumber() {
        return AnySimpleType.isSet(port_number);
    }

    @Override
    public int getPortNumber() {
        return port_number != null ? port_number.getIntValue() : 0;
    }

    @Override
    public String getDatabaseName() {
        return database_name != null ? database_name.getValue() : null;
    }

    @Override
    public String getUrl() {
        return url != null ? url.getValue() : null;
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
    public List<Property> getProperties() {
        if (property != null) {
            return property.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public boolean isSetLoginTimeout() {
        return AnySimpleType.isSet(login_timeout);
    }

    @Override
    public int getLoginTimeout() {
        return login_timeout != null ? login_timeout.getIntValue() : 0;
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
    public int getIsolationLevelValue() {
        if (isolation_level != null) {
            switch (isolation_level.value) {
                case TRANSACTION_READ_UNCOMMITTED:
                    return Connection.TRANSACTION_READ_UNCOMMITTED;
                case TRANSACTION_READ_COMMITTED:
                    return Connection.TRANSACTION_READ_COMMITTED;
                case TRANSACTION_REPEATABLE_READ:
                    return Connection.TRANSACTION_REPEATABLE_READ;
                case TRANSACTION_SERIALIZABLE:
                    return Connection.TRANSACTION_SERIALIZABLE;
            }
        }
        return Connection.TRANSACTION_NONE;
    }

    @Override
    public boolean isSetInitialPoolSize() {
        return AnySimpleType.isSet(initial_pool_size);
    }

    @Override
    public int getInitialPoolSize() {
        return initial_pool_size != null ? initial_pool_size.getIntValue() : 0;
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
    public boolean isSetMaxIdleTime() {
        return AnySimpleType.isSet(max_idle_time);
    }

    @Override
    public int getMaxIdleTime() {
        return max_idle_time != null ? max_idle_time.getIntValue() : 0;
    }

    @Override
    public boolean isSetMaxStatements() {
        return AnySimpleType.isSet(max_statements);
    }

    @Override
    public int getMaxStatements() {
        return max_statements != null ? max_statements.getIntValue() : 0;
    }

    // elements
    DescriptionType description;
    XSDTokenType class_name;
    XSDTokenType server_name;
    XSDIntegerType port_number;
    XSDTokenType database_name;
    XSDTokenType url;
    XSDTokenType user;
    XSDTokenType password;
    PropertyType.ListType property;
    XSDIntegerType login_timeout;
    XSDBooleanType transactional;
    IsolationLevelType isolation_level;
    XSDIntegerType initial_pool_size;
    XSDIntegerType max_pool_size;
    XSDIntegerType min_pool_size;
    XSDIntegerType max_idle_time;
    XSDIntegerType max_statements;

    public DataSourceType() {
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
            this.description = description;
            return true;
        }
        if ("class-name".equals(localName)) {
            XSDTokenType class_name = new XSDTokenType();
            parser.parse(class_name);
            this.class_name = class_name;
            return true;
        }
        if ("server-name".equals(localName)) {
            XSDTokenType server_name = new XSDTokenType();
            parser.parse(server_name);
            this.server_name = server_name;
            return true;
        }
        if ("port-number".equals(localName)) {
            XSDIntegerType port_number = new XSDIntegerType();
            parser.parse(port_number);
            this.port_number = port_number;
            return true;
        }
        if ("database-name".equals(localName)) {
            XSDTokenType database_name = new XSDTokenType();
            parser.parse(database_name);
            this.database_name = database_name;
            return true;
        }
        if ("url".equals(localName)) {
            XSDTokenType url = new XSDTokenType();
            parser.parse(url);
            this.url = url;
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
        if ("property".equals(localName)) {
            PropertyType property = new PropertyType();
            parser.parse(property);
            addProperty(property);
            return true;
        }
        if ("login-timeout".equals(localName)) {
            XSDIntegerType login_timeout = new XSDIntegerType();
            parser.parse(login_timeout);
            this.login_timeout = login_timeout;
            return true;
        }
        if ("transactional".equals(localName)) {
            XSDBooleanType transactional = new XSDBooleanType();
            parser.parse(transactional);
            this.transactional = transactional;
            return true;
        }
        if ("isolation-level".equals(localName)) {
            IsolationLevelType isolation_level = new IsolationLevelType();
            parser.parse(isolation_level);
            this.isolation_level = isolation_level;
            return true;
        }
        if ("initial-pool-size".equals(localName)) {
            XSDIntegerType initial_pool_size = new XSDIntegerType();
            parser.parse(initial_pool_size);
            this.initial_pool_size = initial_pool_size;
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
        if ("max-idle-time".equals(localName)) {
            XSDIntegerType max_idle_time = new XSDIntegerType();
            parser.parse(max_idle_time);
            this.max_idle_time = max_idle_time;
            return true;
        }
        if ("max-statements".equals(localName)) {
            XSDIntegerType max_statements = new XSDIntegerType();
            parser.parse(max_statements);
            this.max_statements = max_statements;
            return true;
        }
        return false;
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
        diag.describeIfSet("class-name", class_name);
        diag.describeIfSet("server-name", server_name);
        diag.describeIfSet("port-number", port_number);
        diag.describeIfSet("database-name", database_name);
        diag.describeIfSet("url", url);
        diag.describeIfSet("user", user);
        diag.describeIfSet("password", password);
        diag.describeIfSet("property", property);
        diag.describeIfSet("login-timeout", login_timeout);
        diag.describeIfSet("transactional", transactional);
        diag.describeIfSet("isolation-level", isolation_level);
        diag.describeIfSet("initial-pool-size", initial_pool_size);
        diag.describeIfSet("max-pool-size", max_pool_size);
        diag.describeIfSet("min-pool-size", min_pool_size);
        diag.describeIfSet("max-idle-time", max_idle_time);
        diag.describeIfSet("max-statements", max_statements);
    }
}
