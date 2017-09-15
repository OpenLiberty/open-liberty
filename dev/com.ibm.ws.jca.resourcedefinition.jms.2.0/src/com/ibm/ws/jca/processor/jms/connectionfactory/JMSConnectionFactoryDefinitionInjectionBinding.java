/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.processor.jms.connectionfactory;

import java.lang.reflect.Member;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jms.JMSConnectionFactoryDefinition;

import com.ibm.ws.javaee.dd.common.Description;
import com.ibm.ws.javaee.dd.common.JMSConnectionFactory;
import com.ibm.ws.javaee.dd.common.Property;
import com.ibm.ws.jca.processor.jms.util.JMSConnectionFactoryProperties;
import com.ibm.ws.jca.processor.jms.util.JMSResourceDefinitionConstants;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionConfigurationException;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.JNDIEnvironmentRefType;

/**
 * Represents Reference 'Binding' information, controlling what is to be bound
 * into the java:comp/env name space context.
 */
public class JMSConnectionFactoryDefinitionInjectionBinding extends InjectionBinding<JMSConnectionFactoryDefinition> {

    private String interfaceName;
    private boolean isXmlInterfaceNameSet;

    private String className;
    private boolean isXmlClassNameSet;

    private String resourceAdapter;
    private boolean isXmlResourceAdapterSet;

    private String user;
    private boolean isXMLUserSet;

    private String password;
    private boolean isXmlPasswordSet;

    private String clientId;
    private boolean isXmlClientIdSet;

    private Map<String, String> properties;
    private final Set<String> xmlProperties = new HashSet<String>();

    private Boolean transactional;
    private boolean isXmlTransactionalSet;

    private String description;
    private boolean isXmlDescriptionSet;

    private Integer maxPoolSize;
    private boolean isXmlMaxPoolSizeSet;

    private Integer minPoolSize;
    private boolean isXmlMinPoolSizeSet;

    /**
     * @param jndiName
     * @param nameSpaceConfig
     */
    public JMSConnectionFactoryDefinitionInjectionBinding(String jndiName, ComponentNameSpaceConfiguration nameSpaceConfig) {
        super(null, nameSpaceConfig);
        setJndiName(jndiName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.injectionengine.InjectionBinding#getJNDIEnvironmentRefType()
     */
    @Override
    protected JNDIEnvironmentRefType getJNDIEnvironmentRefType() {
        return JNDIEnvironmentRefType.JMSConnectionFactory;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.injectionengine.InjectionBinding#merge(java.lang.annotation.Annotation, java.lang.Class, java.lang.reflect.Member)
     */
    @Override
    public void merge(JMSConnectionFactoryDefinition annotation, Class<?> instanceClass, Member member) throws InjectionException {

        if (member != null) {
            // ConnectionFactoryDefinition is a class-level annotation only.
            throw new IllegalArgumentException(member.toString());
        }

        interfaceName = mergeAnnotationValue(interfaceName, isXmlInterfaceNameSet, annotation.interfaceName(), JMSConnectionFactoryProperties.INTERFACE_NAME.getAnnotationKey(),
                                             JMSResourceDefinitionConstants.JMS_CONNECTION_FACTORY_INTERFACE);
        className = mergeAnnotationValue(className, isXmlClassNameSet, annotation.className(), JMSConnectionFactoryProperties.CLASS_NAME.getAnnotationKey(), "");
        resourceAdapter = mergeAnnotationValue(resourceAdapter, isXmlResourceAdapterSet, annotation.resourceAdapter(),
                                               JMSConnectionFactoryProperties.RESOURCE_ADAPTER.getAnnotationKey(),
                                               JMSResourceDefinitionConstants.DEFAULT_JMS_RESOURCE_ADAPTER);
        user = mergeAnnotationValue(user, isXMLUserSet, annotation.user(), JMSConnectionFactoryProperties.USER.getAnnotationKey(), "");
        password = mergeAnnotationValue(password, isXmlPasswordSet, annotation.password(), JMSConnectionFactoryProperties.PASSWORD.getAnnotationKey(), "");
        clientId = mergeAnnotationValue(clientId, isXmlClientIdSet, annotation.clientId(), JMSConnectionFactoryProperties.CLIENT_ID.getAnnotationKey(), "");
        properties = mergeAnnotationProperties(properties, xmlProperties, annotation.properties());
        transactional = mergeAnnotationBoolean(transactional, isXmlTransactionalSet, annotation.transactional(), JMSConnectionFactoryProperties.TRANSACTIONAL.getAnnotationKey(),
                                               JMSResourceDefinitionConstants.DEFAULT_TRANSACTIONAL_VALUE);
        description = mergeAnnotationValue(description, isXmlDescriptionSet, annotation.description(), JMSConnectionFactoryProperties.DESCRIPTION.getAnnotationKey(), ""); // d662109
        maxPoolSize = mergeAnnotationInteger(maxPoolSize, isXmlMaxPoolSizeSet, annotation.maxPoolSize(), JMSConnectionFactoryProperties.MAX_POOL_SIZE.getAnnotationKey(), -1, null);
        minPoolSize = mergeAnnotationInteger(minPoolSize, isXmlMinPoolSizeSet, annotation.minPoolSize(), JMSConnectionFactoryProperties.MIN_POOL_SIZE.getAnnotationKey(), -1, null);

    }

    /**
     * @throws InjectionException
     */
    void resolve() throws InjectionException {

        Map<String, Object> props = new HashMap<String, Object>();

        if (properties != null) {
            props.putAll(properties);
        }

        // Insert all remaining attributes.
        addOrRemoveProperty(props, JMSConnectionFactoryProperties.INTERFACE_NAME.getAnnotationKey(),
                            (interfaceName == null || interfaceName.isEmpty()) ? JMSResourceDefinitionConstants.JMS_CONNECTION_FACTORY_INTERFACE : interfaceName);
        addOrRemoveProperty(props, JMSConnectionFactoryProperties.CLASS_NAME.getAnnotationKey(), className);
        addOrRemoveProperty(props, JMSConnectionFactoryProperties.RESOURCE_ADAPTER.getAnnotationKey(),
                            (resourceAdapter == null || resourceAdapter.isEmpty()) ? JMSResourceDefinitionConstants.DEFAULT_JMS_RESOURCE_ADAPTER : resourceAdapter);
        addOrRemoveProperty(props, JMSConnectionFactoryProperties.USER.getAnnotationKey(), user);
        addOrRemoveProperty(props, JMSConnectionFactoryProperties.PASSWORD.getAnnotationKey(), password);
        addOrRemoveProperty(props, JMSConnectionFactoryProperties.CLIENT_ID.getAnnotationKey(), clientId);
        addOrRemoveProperty(props, JMSConnectionFactoryProperties.TRANSACTIONAL.getAnnotationKey(), transactional);
        addOrRemoveProperty(props, JMSConnectionFactoryProperties.DESCRIPTION.getAnnotationKey(), description);
        addOrRemoveProperty(props, JMSConnectionFactoryProperties.MAX_POOL_SIZE.getAnnotationKey(), maxPoolSize);
        addOrRemoveProperty(props, JMSConnectionFactoryProperties.MIN_POOL_SIZE.getAnnotationKey(), minPoolSize);

        setObjects(null, createDefinitionReference(null, JMSResourceDefinitionConstants.JMS_CONNECTION_FACTORY_INTERFACE, props));

    }

    /**
     * @param oldValue
     * @param newValue
     * @param type
     * @param valueNames
     * @return
     * @throws InjectionConfigurationException
     */
    protected <T> T mergeXMLValue(T oldValue,
                                  T newValue,
                                  JMSConnectionFactoryProperties type,
                                  Map<T, String> valueNames) throws InjectionConfigurationException {

        return mergeXMLValue(oldValue, newValue, type.getXmlKey(), type.getAnnotationKey(), valueNames);
    }

    /**
     * @param jmsConnectionFactory
     * @throws InjectionConfigurationException
     */
    void mergeXML(JMSConnectionFactory jmsConnectionFactory) throws InjectionConfigurationException {
        List<Description> descriptionList = jmsConnectionFactory.getDescriptions();

        if (descriptionList != null && !descriptionList.isEmpty()) {
            description = mergeXMLValue(description, descriptionList.get(0).getValue(), JMSConnectionFactoryProperties.DESCRIPTION, null);
            isXmlDescriptionSet = true;
        }

        String interfaceNameValue = jmsConnectionFactory.getInterfaceNameValue();
        if (interfaceNameValue != null) {
            interfaceName = mergeXMLValue(interfaceName, interfaceNameValue, JMSConnectionFactoryProperties.INTERFACE_NAME, null);
            isXmlInterfaceNameSet = true;
        }

        String resourceAdapterValue = jmsConnectionFactory.getResourceAdapter();
        if (resourceAdapterValue != null) {
            resourceAdapter = mergeXMLValue(resourceAdapter, resourceAdapterValue, JMSConnectionFactoryProperties.RESOURCE_ADAPTER, null);
            isXmlResourceAdapterSet = true;
        }

        if (jmsConnectionFactory.isSetMaxPoolSize()) {
            maxPoolSize = mergeXMLValue(maxPoolSize, jmsConnectionFactory.getMaxPoolSize(), JMSConnectionFactoryProperties.MAX_POOL_SIZE, null);
            isXmlMaxPoolSizeSet = true;
        }

        if (jmsConnectionFactory.isSetMinPoolSize()) {
            minPoolSize = mergeXMLValue(minPoolSize, jmsConnectionFactory.getMinPoolSize(), JMSConnectionFactoryProperties.MIN_POOL_SIZE, null);
            isXmlMinPoolSizeSet = true;
        }

        if (jmsConnectionFactory.isSetTransactional()) {

            transactional = mergeXMLValue(transactional, jmsConnectionFactory.isTransactional(), JMSConnectionFactoryProperties.TRANSACTIONAL, null);
            isXmlTransactionalSet = true;
        }

        if (jmsConnectionFactory.getUser() != null) {
            user = mergeXMLValue(user, jmsConnectionFactory.getUser(), JMSConnectionFactoryProperties.USER, null);
            isXMLUserSet = true;
        }

        if (jmsConnectionFactory.getPassword() != null) {
            password = mergeXMLValue(password, jmsConnectionFactory.getPassword(), JMSConnectionFactoryProperties.PASSWORD, null);
            isXmlPasswordSet = true;
        }

        if (jmsConnectionFactory.getClientId() != null) {
            clientId = mergeXMLValue(clientId, jmsConnectionFactory.getClientId(), JMSConnectionFactoryProperties.CLIENT_ID, null);
            isXmlClientIdSet = true;
        }

        List<Property> cfdProps = jmsConnectionFactory.getProperties();
        properties = mergeXMLProperties(properties, xmlProperties, cfdProps);

    }

    @Override
    public void mergeSaved(InjectionBinding<JMSConnectionFactoryDefinition> injectionBinding) throws InjectionException {
        JMSConnectionFactoryDefinitionInjectionBinding jmsConnectionFactoryBinding = (JMSConnectionFactoryDefinitionInjectionBinding) injectionBinding;

        mergeSavedValue(description, jmsConnectionFactoryBinding.description, JMSConnectionFactoryProperties.DESCRIPTION.getXmlKey());
        mergeSavedValue(interfaceName, jmsConnectionFactoryBinding.interfaceName, JMSConnectionFactoryProperties.INTERFACE_NAME.getXmlKey());
        mergeSavedValue(className, jmsConnectionFactoryBinding.className, JMSConnectionFactoryProperties.CLASS_NAME.getXmlKey());
        mergeSavedValue(user, jmsConnectionFactoryBinding.user, JMSConnectionFactoryProperties.USER.getXmlKey());
        mergeSavedValue(password, jmsConnectionFactoryBinding.password, JMSConnectionFactoryProperties.PASSWORD.getXmlKey());
        mergeSavedValue(clientId, jmsConnectionFactoryBinding.clientId, JMSConnectionFactoryProperties.CLIENT_ID.getXmlKey());
        mergeSavedValue(resourceAdapter, jmsConnectionFactoryBinding.resourceAdapter, JMSConnectionFactoryProperties.RESOURCE_ADAPTER.getXmlKey());
        mergeSavedValue(maxPoolSize, jmsConnectionFactoryBinding.maxPoolSize, JMSConnectionFactoryProperties.MAX_POOL_SIZE.getXmlKey());
        mergeSavedValue(minPoolSize, jmsConnectionFactoryBinding.minPoolSize, JMSConnectionFactoryProperties.MIN_POOL_SIZE.getXmlKey());
        mergeSavedValue(transactional, jmsConnectionFactoryBinding.transactional, JMSConnectionFactoryProperties.TRANSACTIONAL.getXmlKey());
        mergeSavedValue(properties, jmsConnectionFactoryBinding.properties, JMSConnectionFactoryProperties.PROPERTIES.getXmlKey());

    }

    @Override
    public Class<?> getAnnotationType() {
        return JMSConnectionFactoryDefinition.class;
    }
}
