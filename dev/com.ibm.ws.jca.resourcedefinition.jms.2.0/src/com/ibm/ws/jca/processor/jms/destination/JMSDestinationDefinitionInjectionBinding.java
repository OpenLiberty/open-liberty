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
package com.ibm.ws.jca.processor.jms.destination;

import java.lang.reflect.Member;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jms.JMSDestinationDefinition;

import com.ibm.ws.javaee.dd.common.Description;
import com.ibm.ws.javaee.dd.common.JMSDestination;
import com.ibm.ws.javaee.dd.common.Property;
import com.ibm.ws.jca.processor.jms.util.JMSDestinationProperties;
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
public class JMSDestinationDefinitionInjectionBinding extends InjectionBinding<JMSDestinationDefinition> {

    private static final String KEY_NAME = "name";
    private static final String KEY_INTERFACE_NAME = "interfaceName";
    private static final String KEY_CLASS_NAME = "className";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_DESTINATION_NAME = "destinationName";
    private static final String KEY_RESOURCE_ADAPTER = "resourceAdapter";

    private static final String DEFAULT_DESTINATION_INTERFACE = "javax.jms.Destination";

    private String name;
    private boolean isXmlNameSet;

    private String interfaceName;
    private boolean isXmlInterfaceNameSet;

    private String className;
    private boolean isXmlclassNameSet;

    private String description;
    private boolean isXmlDescriptionSet;

    private String destinationName;
    private boolean isXmlDestinationNameSet;

    private String resourceAdapter;
    private boolean isXmlResourceAdapterSet;

    private Map<String, String> properties;
    private Set<String> xmlProperties;

    /**
     * @param jndiName
     * @param nameSpaceConfig
     */
    public JMSDestinationDefinitionInjectionBinding(String jndiName, ComponentNameSpaceConfiguration nameSpaceConfig) {
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
        return JNDIEnvironmentRefType.JMSDestination;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.injectionengine.InjectionBinding#merge(java.lang.annotation.Annotation, java.lang.Class, java.lang.reflect.Member)
     */
    @Override
    public void merge(JMSDestinationDefinition annotation, Class<?> instanceClass, Member member) throws InjectionException {

        if (member != null) {
            // JMSDestinationDefinition is a class-level annotation only.
            throw new IllegalArgumentException(member.toString());
        }

        name = mergeAnnotationValue(name, isXmlNameSet, annotation.name(), JMSDestinationProperties.NAME.getAnnotationKey(), "");
        interfaceName = mergeAnnotationValue(interfaceName, isXmlInterfaceNameSet, annotation.interfaceName(), JMSDestinationProperties.INTERFACE_NAME.getAnnotationKey(), "");
        className = mergeAnnotationValue(className, isXmlclassNameSet, annotation.className(), JMSDestinationProperties.CLASS_NAME.getAnnotationKey(), "");
        description = mergeAnnotationValue(description, isXmlDescriptionSet, annotation.description(), JMSDestinationProperties.DESCRIPTION.getAnnotationKey(), "");
        destinationName = mergeAnnotationValue(destinationName, isXmlDestinationNameSet, annotation.destinationName(),
                                               JMSDestinationProperties.DESTINATION_NAME.getAnnotationKey(), "");
        resourceAdapter = mergeAnnotationValue(resourceAdapter, isXmlResourceAdapterSet, annotation.resourceAdapter(),
                                               JMSDestinationProperties.RESOURCE_ADAPTER.getAnnotationKey(), "");
        properties = mergeAnnotationProperties(properties, xmlProperties, annotation.properties());

    }

    /**
     * @throws InjectionException
     */
    void resolve() throws InjectionException {

        Map<String, Object> props = new HashMap<String, Object>();

        if (properties != null) {
            props.putAll(properties);
        }

        addOrRemoveProperty(props, KEY_NAME, name);
        addOrRemoveProperty(props, KEY_INTERFACE_NAME, (interfaceName == null || interfaceName.isEmpty()) ? JMSResourceDefinitionConstants.JMS_QUEUE_INTERFACE : interfaceName);
        addOrRemoveProperty(props, KEY_CLASS_NAME, className);
        addOrRemoveProperty(props, KEY_RESOURCE_ADAPTER,
                            (resourceAdapter == null || resourceAdapter.isEmpty()) ? JMSResourceDefinitionConstants.DEFAULT_JMS_RESOURCE_ADAPTER : resourceAdapter);
        addOrRemoveProperty(props, KEY_DESTINATION_NAME, destinationName);
        addOrRemoveProperty(props, KEY_DESCRIPTION, description);

        setObjects(null, createDefinitionReference(null, DEFAULT_DESTINATION_INTERFACE, props));

    }

    /**
     * @param jmsDestination
     * @throws InjectionConfigurationException
     */
    void mergeXML(JMSDestination jmsDestination) throws InjectionConfigurationException {
        //TODO, check if there is real need to merge name since its a primary key kind.
        //If different name is given then it will be considered as different resource.
        String nameValue = jmsDestination.getName();
        if (nameValue != null) {
            name = mergeXMLValue(name, nameValue, JMSDestinationProperties.NAME, null);
            isXmlNameSet = true;
        }

        List<Description> descriptionList = jmsDestination.getDescriptions();
        if (descriptionList != null && !descriptionList.isEmpty()) {
            description = mergeXMLValue(description, descriptionList.get(0).getValue(), JMSDestinationProperties.DESCRIPTION, null);
            isXmlDescriptionSet = true;
        }

        String resourceAdapterValue = jmsDestination.getResourceAdapter();
        if (resourceAdapterValue != null) {
            resourceAdapter = mergeXMLValue(resourceAdapter, resourceAdapterValue, JMSDestinationProperties.RESOURCE_ADAPTER, null);
            isXmlResourceAdapterSet = true;
        }

        String classNameValue = jmsDestination.getClassNameValue();
        if (classNameValue != null) {
            className = mergeXMLValue(className, classNameValue, JMSDestinationProperties.CLASS_NAME, null);
            isXmlclassNameSet = true;
        }

        String interfaceNameValue = jmsDestination.getInterfaceNameValue();
        if (interfaceNameValue != null) {
            interfaceName = mergeXMLValue(interfaceName, interfaceNameValue, JMSDestinationProperties.INTERFACE_NAME, null);
            isXmlInterfaceNameSet = true;
        }

        String destinationNameValue = jmsDestination.getDestinationName();
        if (destinationNameValue != null) {
            destinationName = mergeXMLValue(destinationName, destinationNameValue, JMSDestinationProperties.DESTINATION_NAME, null);
            isXmlDestinationNameSet = true;
        }

        List<Property> aodProps = jmsDestination.getProperties();
        mergeXMLProperties(aodProps);

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
                                  JMSDestinationProperties props,
                                  Map<T, String> valueNames) throws InjectionConfigurationException {

        return mergeXMLValue(oldValue, newValue, props.getXmlKey(), props.getAnnotationKey(), valueNames);
    }

    /**
     * @param props
     * @throws InjectionConfigurationException
     */
    private void mergeXMLProperties(List<Property> props) throws InjectionConfigurationException {
        if (!props.isEmpty()) {
            if (properties == null) {
                properties = new HashMap<String, String>();
                xmlProperties = new HashSet<String>();
            }

            for (Property prop : props) {
                String name = prop.getName();
                String newValue = prop.getValue();
                Object oldValue = properties.put(name, newValue);

                if (oldValue != null && !newValue.equals(oldValue)) {
                    mergeError(oldValue, newValue, true, name + " property", true, name);
                    continue;
                }

                xmlProperties.add(name);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.injectionengine.InjectionBinding#mergeSaved(com.ibm.wsspi.injectionengine.InjectionBinding)
     */
    @Override
    public void mergeSaved(InjectionBinding<JMSDestinationDefinition> injectionBinding) throws InjectionException {
        JMSDestinationDefinitionInjectionBinding jmsDestDefObjectBinding = (JMSDestinationDefinitionInjectionBinding) injectionBinding;

        mergeSavedValue(name, jmsDestDefObjectBinding.name, JMSDestinationProperties.NAME.getXmlKey());
        mergeSavedValue(description, jmsDestDefObjectBinding.description, JMSDestinationProperties.DESCRIPTION.getXmlKey());
        mergeSavedValue(resourceAdapter, jmsDestDefObjectBinding.resourceAdapter, JMSDestinationProperties.RESOURCE_ADAPTER.getXmlKey());
        mergeSavedValue(className, jmsDestDefObjectBinding.className, JMSDestinationProperties.CLASS_NAME.getXmlKey());
        mergeSavedValue(interfaceName, jmsDestDefObjectBinding.interfaceName, JMSDestinationProperties.INTERFACE_NAME.getXmlKey());
        mergeSavedValue(destinationName, jmsDestDefObjectBinding.destinationName, JMSDestinationProperties.DESTINATION_NAME.getXmlKey());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.injectionengine.InjectionBinding#getAnnotationType()
     */
    @Override
    public Class<?> getAnnotationType() {
        return JMSDestinationDefinition.class;
    }
}