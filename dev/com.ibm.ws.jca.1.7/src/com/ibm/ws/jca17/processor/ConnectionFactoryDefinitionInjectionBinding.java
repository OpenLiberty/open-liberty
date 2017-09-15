/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca17.processor;

import java.lang.reflect.Member;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.resource.ConnectionFactoryDefinition;
import javax.resource.spi.TransactionSupport;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.javaee.dd.common.ConnectionFactory;
import com.ibm.ws.javaee.dd.common.Description;
import com.ibm.ws.javaee.dd.common.Property;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionConfigurationException;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.JNDIEnvironmentRefType;

/**
 *
 */
public class ConnectionFactoryDefinitionInjectionBinding extends InjectionBinding<ConnectionFactoryDefinition> {
    private static final TraceComponent tc = Tr.register(ConnectionFactoryDefinitionInjectionBinding.class,
                                                         "WAS.j2c",
                                                         "com.ibm.ws.jca.internal.resources.J2CAMessages");

    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_INTERFACE_NAME = "interfaceName";
    private static final String KEY_RESOURCE_ADAPTER = "resourceAdapter";
    private static final String KEY_MAX_POOL_SIZE = "maxPoolSize";
    private static final String KEY_MIN_POOL_SIZE = "minPoolSize";
    private static final String KEY_TRANSACTION_SUPPORT = "transactionSupport";

    private String description;
    private boolean XMLDescription;

    private String interfaceName;
    private boolean XMLInterfaceName;

    private String resourceAdapter;
    private boolean XMLResourceAdapter;

    private Map<String, String> properties;
    private final Set<String> XMLProperties = new HashSet<String>();

    private Integer transactionSupport;
    private boolean XMLTransactionSupport;

    private Integer maxPoolSize;
    private boolean XMLMaxPoolSize;

    private Integer minPoolSize;
    private boolean XMLMinPoolSize;

    private static final Map<Integer, String> TRANSACTION_SUPPORT_NAMES = new TreeMap<Integer, String>();

    static {
        TRANSACTION_SUPPORT_NAMES.put(ConnectionFactory.TRANSACTION_SUPPORT_UNSPECIFIED, "TRANSACTION_SUPPORT_UNSPECIFIED");
        TRANSACTION_SUPPORT_NAMES.put(ConnectionFactory.TRANSACTION_SUPPORT_NO_TRANSACTION, TransactionSupport.TransactionSupportLevel.NoTransaction.name());
        TRANSACTION_SUPPORT_NAMES.put(ConnectionFactory.TRANSACTION_SUPPORT_LOCAL_TRANSACTION, TransactionSupport.TransactionSupportLevel.LocalTransaction.name());
        TRANSACTION_SUPPORT_NAMES.put(ConnectionFactory.TRANSACTION_SUPPORT_XA_TRANSACTION, TransactionSupport.TransactionSupportLevel.XATransaction.name());

    }

    public ConnectionFactoryDefinitionInjectionBinding(String jndiName, ComponentNameSpaceConfiguration nameSpaceConfig) {
        super(null, nameSpaceConfig);
        setJndiName(jndiName);
    }

    @Override
    protected JNDIEnvironmentRefType getJNDIEnvironmentRefType() {
        return JNDIEnvironmentRefType.ConnectionFactory;
    }

    @Override
    public void merge(ConnectionFactoryDefinition annotation, Class<?> instanceClass, Member member) throws InjectionException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "merge: name=" + getJndiName() + ", " + annotation);

        if (member != null) {
            // ConnectionFactoryDefinition is a class-level annotation only.
            throw new IllegalArgumentException(member.toString());
        }

        description = mergeAnnotationValue(description, XMLDescription, annotation.description(), KEY_DESCRIPTION, "");
        interfaceName = mergeAnnotationValue(interfaceName, XMLInterfaceName, annotation.interfaceName(), KEY_INTERFACE_NAME, "");
        resourceAdapter = mergeAnnotationValue(resourceAdapter, XMLResourceAdapter, annotation.resourceAdapter(), KEY_RESOURCE_ADAPTER, "");
        transactionSupport = mergeAnnotationInteger(transactionSupport, XMLTransactionSupport, annotation.transactionSupport().ordinal(), KEY_TRANSACTION_SUPPORT, -1, null);
        properties = mergeAnnotationProperties(properties, XMLProperties, annotation.properties());
        maxPoolSize = mergeAnnotationInteger(maxPoolSize, XMLMaxPoolSize, annotation.maxPoolSize(), KEY_MAX_POOL_SIZE, -1, null);
        minPoolSize = mergeAnnotationInteger(minPoolSize, XMLMinPoolSize, annotation.minPoolSize(), KEY_MIN_POOL_SIZE, -1, null);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "merge");
    }

    void resolve() throws InjectionException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "resolve");

        Map<String, Object> props = new HashMap<String, Object>();

        if (properties != null) {
            props.putAll(properties);
        }

        // Insert all remaining attributes.
        addOrRemoveProperty(props, KEY_DESCRIPTION, description);
        addOrRemoveProperty(props, KEY_INTERFACE_NAME, interfaceName);
        addOrRemoveProperty(props, KEY_RESOURCE_ADAPTER, resourceAdapter);
        addOrRemoveProperty(props, KEY_TRANSACTION_SUPPORT, TRANSACTION_SUPPORT_NAMES.get(transactionSupport));
        addOrRemoveProperty(props, KEY_MIN_POOL_SIZE, minPoolSize);
        addOrRemoveProperty(props, KEY_MAX_POOL_SIZE, maxPoolSize);

        setObjects(null, createDefinitionReference(null, javax.resource.cci.ConnectionFactory.class.getName(), props));

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "resolve");
    }

    void mergeXML(ConnectionFactory cfd) throws InjectionConfigurationException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "mergeXML: name=" + getJndiName() + ", " + cfd);

        List<Description> descriptionList = cfd.getDescriptions();

        if (description != null) {
            description = mergeXMLValue(description, descriptionList.toString(), "description", KEY_DESCRIPTION, null);
            XMLDescription = true;
        }

        String interfaceNameValue = cfd.getInterfaceNameValue();
        if (interfaceNameValue != null) {
            interfaceName = mergeXMLValue(interfaceName, interfaceNameValue, "interface-name", KEY_INTERFACE_NAME, null);
            XMLInterfaceName = true;
        }

        String resourceAdapterValue = cfd.getResourceAdapter();
        if (resourceAdapterValue != null) {
            resourceAdapter = mergeXMLValue(resourceAdapter, resourceAdapterValue, "resource-adapter", KEY_RESOURCE_ADAPTER, null);
            XMLResourceAdapter = true;
        }

        if (cfd.isSetMaxPoolSize()) {
            maxPoolSize = mergeXMLValue(maxPoolSize, cfd.getMaxPoolSize(), "max-pool-size", KEY_MAX_POOL_SIZE, null);
            XMLMaxPoolSize = true;
        }

        if (cfd.isSetMinPoolSize()) {
            minPoolSize = mergeXMLValue(minPoolSize, cfd.getMinPoolSize(), "min-pool-size", KEY_MIN_POOL_SIZE, null);
            XMLMinPoolSize = true;
        }

        int transactionSupportInt = cfd.getTransactionSupportValue();
        if (transactionSupportInt != ConnectionFactory.TRANSACTION_SUPPORT_UNSPECIFIED) {
            transactionSupport = mergeXMLValue(transactionSupport, transactionSupportInt, "transaction-support", KEY_TRANSACTION_SUPPORT, TRANSACTION_SUPPORT_NAMES);
            XMLTransactionSupport = true;
        }

        List<Property> cfdProps = cfd.getProperties();
        properties = mergeXMLProperties(properties, XMLProperties, cfdProps);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "mergeXML");
    }

    @Override
    public void mergeSaved(InjectionBinding<ConnectionFactoryDefinition> injectionBinding) throws InjectionException {
        ConnectionFactoryDefinitionInjectionBinding connectionFactoryBinding = (ConnectionFactoryDefinitionInjectionBinding) injectionBinding;

        mergeSavedValue(description, connectionFactoryBinding.description, "description");
        mergeSavedValue(interfaceName, connectionFactoryBinding.interfaceName, "interface-name");
        mergeSavedValue(resourceAdapter, connectionFactoryBinding.resourceAdapter, "resource-adapter");
        mergeSavedValue(maxPoolSize, connectionFactoryBinding.maxPoolSize, "max-pool-size");
        mergeSavedValue(minPoolSize, connectionFactoryBinding.minPoolSize, "min-pool-size");
        mergeSavedValue(transactionSupport, connectionFactoryBinding.transactionSupport, "transaction-support");
        mergeSavedValue(properties, connectionFactoryBinding.properties, "properties");

    }

    @Override
    public Class<?> getAnnotationType() {
        return ConnectionFactoryDefinition.class;
    }

}
