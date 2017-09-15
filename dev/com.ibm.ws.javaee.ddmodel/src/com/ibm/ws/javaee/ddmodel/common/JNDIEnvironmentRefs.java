/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
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

import com.ibm.ws.javaee.dd.common.AdministeredObject;
import com.ibm.ws.javaee.dd.common.ConnectionFactory;
import com.ibm.ws.javaee.dd.common.DataSource;
import com.ibm.ws.javaee.dd.common.EJBRef;
import com.ibm.ws.javaee.dd.common.EnvEntry;
import com.ibm.ws.javaee.dd.common.JMSConnectionFactory;
import com.ibm.ws.javaee.dd.common.JMSDestination;
import com.ibm.ws.javaee.dd.common.MailSession;
import com.ibm.ws.javaee.dd.common.MessageDestinationRef;
import com.ibm.ws.javaee.dd.common.PersistenceContextRef;
import com.ibm.ws.javaee.dd.common.PersistenceUnitRef;
import com.ibm.ws.javaee.dd.common.ResourceEnvRef;
import com.ibm.ws.javaee.dd.common.ResourceRef;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.common.wsclient.ServiceRefGroup;

/*
 <xsd:group name="jndiEnvironmentRefsGroup">
 <xsd:sequence>
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
 <xsd:element name="post-construct"
 type="javaee:lifecycle-callbackType"
 minOccurs="0"
 maxOccurs="unbounded"/>
 <xsd:element name="pre-destroy"
 type="javaee:lifecycle-callbackType"
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
 </xsd:group>
 */

public class JNDIEnvironmentRefs extends ServiceRefGroup implements com.ibm.ws.javaee.dd.common.JNDIEnvironmentRefs {

    @Override
    public List<EnvEntry> getEnvEntries() {
        if (env_entry != null) {
            return env_entry.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<EJBRef> getEJBRefs() {
        if (ejb_ref != null) {
            return ejb_ref.getList();
        } else {
            return Collections.emptyList();
        }
    }

    protected boolean isEJBLocalRefSupported() {
        return true;
    }

    @Override
    public List<EJBRef> getEJBLocalRefs() {
        if (!isEJBLocalRefSupported()) {
            return null;
        }
        if (ejb_local_ref != null) {
            return ejb_local_ref.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<ResourceRef> getResourceRefs() {
        if (resource_ref != null) {
            return resource_ref.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<ResourceEnvRef> getResourceEnvRefs() {
        if (resource_env_ref != null) {
            return resource_env_ref.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<MessageDestinationRef> getMessageDestinationRefs() {
        if (message_destination_ref != null) {
            return message_destination_ref.getList();
        } else {
            return Collections.emptyList();
        }
    }

    protected boolean isPersistenceContextRefSupported() {
        return true;
    }

    @Override
    public List<PersistenceContextRef> getPersistenceContextRefs() {
        if (!isPersistenceContextRefSupported()) {
            return null;
        }
        if (persistence_context_ref != null) {
            return persistence_context_ref.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<PersistenceUnitRef> getPersistenceUnitRefs() {
        if (persistence_unit_ref != null) {
            return persistence_unit_ref.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<DataSource> getDataSources() {
        if (data_source != null) {
            return data_source.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<JMSConnectionFactory> getJMSConnectionFactories() {
        if (jms_connection_factory != null) {
            return jms_connection_factory.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<JMSDestination> getJMSDestinations() {
        if (jms_destination != null) {
            return jms_destination.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<MailSession> getMailSessions() {
        if (mail_session != null) {
            return mail_session.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<ConnectionFactory> getConnectionFactories() {
        if (connection_factory != null) {
            return connection_factory.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<AdministeredObject> getAdministeredObjects() {
        if (administered_object != null) {
            return administered_object.getList();
        } else {
            return Collections.emptyList();
        }
    }

    private EnvEntryType.ListType env_entry;
    private EJBRefType.ListType ejb_ref;
    private EJBLocalRefType.ListType ejb_local_ref;
    // ServiceRefGroup fields appear here in sequence
    private ResourceRefType.ListType resource_ref;
    private ResourceEnvRefType.ListType resource_env_ref;
    private MessageDestinationRefType.ListType message_destination_ref;
    private PersistenceContextRefType.ListType persistence_context_ref;
    private PersistenceUnitRefType.ListType persistence_unit_ref;
    private DataSourceType.ListType data_source;
    private JMSConnectionFactoryType.ListType jms_connection_factory;
    private JMSDestinationType.ListType jms_destination;
    private MailSessionType.ListType mail_session;
    private ConnectionFactoryType.ListType connection_factory;
    private AdministeredObjectType.ListType administered_object;

    @Override
    public boolean handleChild(DDParser parser, String localName) throws ParseException {
        if (super.handleChild(parser, localName)) {
            return true;
        }
        if ("env-entry".equals(localName)) {
            EnvEntryType env_entry = new EnvEntryType();
            parser.parse(env_entry);
            addEnvEntry(env_entry);
            return true;
        }
        if ("ejb-ref".equals(localName)) {
            EJBRefType ejb_ref = new EJBRefType();
            parser.parse(ejb_ref);
            addEJBRef(ejb_ref);
            return true;
        }
        if (isEJBLocalRefSupported() && "ejb-local-ref".equals(localName)) {
            EJBLocalRefType ejb_local_ref = new EJBLocalRefType();
            parser.parse(ejb_local_ref);
            addEJBLocalRef(ejb_local_ref);
            return true;
        }
        if ("resource-ref".equals(localName)) {
            ResourceRefType resource_ref = new ResourceRefType();
            parser.parse(resource_ref);
            addResourceRef(resource_ref);
            return true;
        }
        if ("resource-env-ref".equals(localName)) {
            ResourceEnvRefType resource_env_ref = new ResourceEnvRefType();
            parser.parse(resource_env_ref);
            addResourceEnvRef(resource_env_ref);
            return true;
        }
        if ("message-destination-ref".equals(localName)) {
            MessageDestinationRefType message_destination_ref = new MessageDestinationRefType();
            parser.parse(message_destination_ref);
            addMessageDestinationRef(message_destination_ref);
            return true;
        }
        if (isPersistenceContextRefSupported() && "persistence-context-ref".equals(localName)) {
            PersistenceContextRefType persistence_context_ref = new PersistenceContextRefType();
            parser.parse(persistence_context_ref);
            addPersistenceContextRef(persistence_context_ref);
            return true;
        }
        if ("persistence-unit-ref".equals(localName)) {
            PersistenceUnitRefType persistence_unit_ref = new PersistenceUnitRefType();
            parser.parse(persistence_unit_ref);
            addPersistenceUnitRef(persistence_unit_ref);
            return true;
        }
        if ("data-source".equals(localName)) {
            DataSourceType data_source = new DataSourceType();
            parser.parse(data_source);
            addDataSource(data_source);
            return true;
        }
        if (parser.eePlatformVersion >= 70 && "jms-connection-factory".equals(localName)) {
            JMSConnectionFactoryType jms_connection_factory = new JMSConnectionFactoryType();
            parser.parse(jms_connection_factory);
            addJMSConnectionFactory(jms_connection_factory);
            return true;
        }
        if (parser.eePlatformVersion >= 70 && "jms-destination".equals(localName)) {
            JMSDestinationType jms_destination = new JMSDestinationType();
            parser.parse(jms_destination);
            addJMSDestination(jms_destination);
            return true;
        }
        if (parser.eePlatformVersion >= 70 && "mail-session".equals(localName)) {
            MailSessionType mail_session = new MailSessionType();
            parser.parse(mail_session);
            addMailSession(mail_session);
            return true;
        }
        if (parser.eePlatformVersion >= 70 && "connection-factory".equals(localName)) {
            ConnectionFactoryType connection_factory = new ConnectionFactoryType();
            parser.parse(connection_factory);
            addConnectionFactory(connection_factory);
            return true;
        }
        if (parser.eePlatformVersion >= 70 && "administered-object".equals(localName)) {
            AdministeredObjectType administered_object = new AdministeredObjectType();
            parser.parse(administered_object);
            addAdministeredObject(administered_object);
            return true;
        }
        return false;
    }

    private void addEnvEntry(EnvEntryType env_entry) {
        if (this.env_entry == null) {
            this.env_entry = new EnvEntryType.ListType();
        }
        this.env_entry.add(env_entry);
    }

    private void addEJBRef(EJBRefType ejb_ref) {
        if (this.ejb_ref == null) {
            this.ejb_ref = new EJBRefType.ListType();
        }
        this.ejb_ref.add(ejb_ref);
    }

    private void addEJBLocalRef(EJBLocalRefType ejb_local_ref) {
        if (this.ejb_local_ref == null) {
            this.ejb_local_ref = new EJBLocalRefType.ListType();
        }
        this.ejb_local_ref.add(ejb_local_ref);
    }

    private void addResourceRef(ResourceRefType resource_ref) {
        if (this.resource_ref == null) {
            this.resource_ref = new ResourceRefType.ListType();
        }
        this.resource_ref.add(resource_ref);
    }

    private void addResourceEnvRef(ResourceEnvRefType resource_env_ref) {
        if (this.resource_env_ref == null) {
            this.resource_env_ref = new ResourceEnvRefType.ListType();
        }
        this.resource_env_ref.add(resource_env_ref);
    }

    private void addMessageDestinationRef(MessageDestinationRefType message_destination_ref) {
        if (this.message_destination_ref == null) {
            this.message_destination_ref = new MessageDestinationRefType.ListType();
        }
        this.message_destination_ref.add(message_destination_ref);
    }

    private void addPersistenceContextRef(PersistenceContextRefType persistence_context_ref) {
        if (this.persistence_context_ref == null) {
            this.persistence_context_ref = new PersistenceContextRefType.ListType();
        }
        this.persistence_context_ref.add(persistence_context_ref);
    }

    private void addPersistenceUnitRef(PersistenceUnitRefType persistence_unit_ref) {
        if (this.persistence_unit_ref == null) {
            this.persistence_unit_ref = new PersistenceUnitRefType.ListType();
        }
        this.persistence_unit_ref.add(persistence_unit_ref);
    }

    private void addDataSource(DataSourceType data_source) {
        if (this.data_source == null) {
            this.data_source = new DataSourceType.ListType();
        }
        this.data_source.add(data_source);
    }

    private void addJMSConnectionFactory(JMSConnectionFactoryType jms_connection_factory) {
        if (this.jms_connection_factory == null) {
            this.jms_connection_factory = new JMSConnectionFactoryType.ListType();
        }
        this.jms_connection_factory.add(jms_connection_factory);
    }

    private void addJMSDestination(JMSDestinationType jms_destination) {
        if (this.jms_destination == null) {
            this.jms_destination = new JMSDestinationType.ListType();
        }
        this.jms_destination.add(jms_destination);
    }

    private void addMailSession(MailSessionType mail_session) {
        if (this.mail_session == null) {
            this.mail_session = new MailSessionType.ListType();
        }
        this.mail_session.add(mail_session);
    }

    private void addConnectionFactory(ConnectionFactoryType connection_factory) {
        if (this.connection_factory == null) {
            this.connection_factory = new ConnectionFactoryType.ListType();
        }
        this.connection_factory.add(connection_factory);
    }

    private void addAdministeredObject(AdministeredObjectType administered_object) {
        if (this.administered_object == null) {
            this.administered_object = new AdministeredObjectType.ListType();
        }
        this.administered_object.add(administered_object);
    }

    @Override
    public void describe(DDParser.Diagnostics diag) {
        diag.describeIfSet("env-entry", env_entry);
        diag.describeIfSet("ejb-ref", ejb_ref);
        diag.describeIfSet("ejb-local-ref", ejb_local_ref);
        super.describe(diag);
        diag.describeIfSet("resource-ref", resource_ref);
        diag.describeIfSet("resource-env-ref", resource_env_ref);
        diag.describeIfSet("message-destination-ref", message_destination_ref);
        diag.describeIfSet("persistence-context-ref", persistence_context_ref);
        diag.describeIfSet("persistence-unit-ref", persistence_unit_ref);
        diag.describeIfSet("data-source", data_source);
        diag.describeIfSet("jms-connection-factory", jms_connection_factory);
        diag.describeIfSet("jms-destination", jms_destination);
        diag.describeIfSet("mail-session", mail_session);
        diag.describeIfSet("connection-factory", connection_factory);
        diag.describeIfSet("administered-object", administered_object);
    }
}
