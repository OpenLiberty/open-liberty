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

import javax.resource.AdministeredObjectDefinition;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.javaee.dd.common.AdministeredObject;
import com.ibm.ws.javaee.dd.common.Property;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionConfigurationException;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.JNDIEnvironmentRefType;

public class AdministeredObjectDefinitionInjectionBinding extends InjectionBinding<AdministeredObjectDefinition>
{
    private static final TraceComponent tc = Tr.register(AdministeredObjectDefinitionInjectionBinding.class,
                                                         "WAS.j2c",
                                                         "com.ibm.ws.jca.internal.resources.J2CAMessages");

    private static final String KEY_NAME = "name";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_RESOURCE_ADAPTER = "resourceAdapter";
    private static final String KEY_CLASS_NAME = "className";
    private static final String KEY_INTERFACE_NAME = "interfaceName";
    private static final String ADMIN_OBJECT_SERVICE_CREATE_CLASS = "com.ibm.ws.jca17.annotation.AdminObjectService";

    private String name;
    private boolean XMLName;

    private String description;
    private boolean XMLDescription;

    private String resourceAdapter;
    private boolean XMLResourceAdapter;

    private String className;
    private boolean XMLclassName;

    private String interfaceName;
    private boolean XMLInterfaceName;

    private Map<String, String> properties;
    private Set<String> XMLProperties;

    public AdministeredObjectDefinitionInjectionBinding(String jndiName, ComponentNameSpaceConfiguration nameSpaceConfig) {
        super(null, nameSpaceConfig);
        setJndiName(jndiName);
    }

    @Override
    protected JNDIEnvironmentRefType getJNDIEnvironmentRefType() {
        return JNDIEnvironmentRefType.AdministeredObject;
    }

    @Override
    public void merge(AdministeredObjectDefinition annotation, Class<?> instanceClass, Member member) throws InjectionException {

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "merge: name=" + getJndiName() + ", " + annotation);

        if (member != null)
        {
            // ConnectionFactoryDefinition is a class-level annotation only.
            throw new IllegalArgumentException(member.toString());
        }

        name = mergeAnnotationValue(name, XMLName, annotation.name(), KEY_NAME, "");
        description = mergeAnnotationValue(description, XMLDescription, annotation.description(), KEY_DESCRIPTION, "");
        resourceAdapter = mergeAnnotationValue(resourceAdapter, XMLResourceAdapter, annotation.resourceAdapter(), KEY_RESOURCE_ADAPTER, "");
        className = mergeAnnotationValue(className, XMLclassName, annotation.className(), KEY_CLASS_NAME, "");
        interfaceName = mergeAnnotationValue(interfaceName, XMLInterfaceName, annotation.interfaceName(), KEY_INTERFACE_NAME, "");
        properties = mergeAnnotationProperties(properties, XMLProperties, annotation.properties());

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "merge");
    }

    void resolve() throws InjectionException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "resolve");

        Map<String, Object> props = new HashMap<String, Object>();

        if (properties != null) {
            props.putAll(properties);
        }

        // Insert all remaining attributes.
        addOrRemoveProperty(props, KEY_NAME, name);
        addOrRemoveProperty(props, KEY_RESOURCE_ADAPTER, resourceAdapter);
        addOrRemoveProperty(props, KEY_CLASS_NAME, className);
        addOrRemoveProperty(props, KEY_DESCRIPTION, description);
        addOrRemoveProperty(props, KEY_INTERFACE_NAME, interfaceName);

        setObjects(null, createDefinitionReference(null, ADMIN_OBJECT_SERVICE_CREATE_CLASS, props));

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "resolve");
    }

    void mergeXML(AdministeredObject aod) throws InjectionConfigurationException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "mergeXML: name=" + getJndiName() + ", binding=" + ", " + aod);

        String nameValue = aod.getName();
        if (nameValue != null)
        {
            name = mergeXMLValue(name, nameValue, "name", KEY_NAME);
            XMLName = true;
        }

        String descriptionValue = aod.getName();
        if (descriptionValue != null)
        {
            description = mergeXMLValue(description, descriptionValue, "description", KEY_NAME);
            XMLDescription = true;
        }

        String resourceAdapterValue = aod.getResourceAdapter();
        if (resourceAdapterValue != null)
        {
            resourceAdapter = mergeXMLValue(resourceAdapter, resourceAdapterValue, "resource-adapter", KEY_RESOURCE_ADAPTER);
            XMLResourceAdapter = true;
        }

        String classNameValue = aod.getClassNameValue();
        if (classNameValue != null)
        {
            className = mergeXMLValue(className, classNameValue, "class-name", KEY_CLASS_NAME);
            XMLclassName = true;
        }

        String interfaceNameValue = aod.getInterfaceNameValue();
        if (interfaceNameValue != null)
        {
            interfaceName = mergeXMLValue(interfaceName, interfaceNameValue, "interface-name", KEY_INTERFACE_NAME);
            XMLInterfaceName = true;
        }

        List<Property> aodProps = aod.getProperties();
        mergeXMLProperties(aodProps);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "mergeXML");
    }

    private void mergeXMLProperties(List<Property> props)
                    throws InjectionConfigurationException
    {
        if (!props.isEmpty())
        {
            if (properties == null)
            {
                properties = new HashMap<String, String>();
                XMLProperties = new HashSet<String>();
            }

            for (Property prop : props)
            {
                String name = prop.getName();
                String newValue = prop.getValue();
                Object oldValue = properties.put(name, newValue);

                if (oldValue != null && !newValue.equals(oldValue))
                {
                    mergeError(oldValue, newValue, true, name + " property", true, name);
                    continue;
                }

                XMLProperties.add(name);
            }
        }
    }

    private <T> T mergeXMLValue(T oldValue,
                                T newValue,
                                String elementName,
                                String key)
                    throws InjectionConfigurationException
    {
        if (newValue == null)
        {
            return oldValue;
        }

        return newValue;
    }

    @Override
    public void mergeSaved(InjectionBinding<AdministeredObjectDefinition> injectionBinding) throws InjectionException
    {
        AdministeredObjectDefinitionInjectionBinding administeredObjectBinding = (AdministeredObjectDefinitionInjectionBinding) injectionBinding;

        mergeSavedValue(name, administeredObjectBinding.name, "name");
        mergeSavedValue(description, administeredObjectBinding.description, "description");
        mergeSavedValue(resourceAdapter, administeredObjectBinding.resourceAdapter, "resource-adapter");
        mergeSavedValue(className, administeredObjectBinding.className, "class-name");
        mergeSavedValue(interfaceName, administeredObjectBinding.interfaceName, "interface-name");
    }

    @Override
    public Class<?> getAnnotationType()
    {
        return AdministeredObjectDefinition.class;
    }

}