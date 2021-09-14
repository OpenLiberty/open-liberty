/*******************************************************************************
 * Copyright (c) 2015,2021
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javamail.internal.injection;

import java.lang.reflect.Member;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.mail.MailSessionDefinition;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.javaee.dd.common.Description;
import com.ibm.ws.javaee.dd.common.MailSession;
import com.ibm.ws.javaee.dd.common.Property;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionConfigurationException;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;

/**
 *
 */
public class MailSessionDefinitionInjectionBinding extends InjectionBinding<MailSessionDefinition> {

    private static final TraceComponent tc = Tr.register(MailSessionDefinitionInjectionBinding.class);

    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_USER = "user";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_STORE_PROTOCOL = "storeProtocol";
    private static final String KEY_STORE_PROTOCOL_CLASS_NAME = "storeProtocolClassName";
    private static final String KEY_TRANSPORT_PROTOCOL = "transportProtocol";
    private static final String KEY_TRANSPORT_PROTOCOL_CLASS_NAME = "transportProtocolClassName";
    private static final String KEY_HOST = "host";
    private static final String KEY_FROM = "from";
    private static final String KEY_PROPERTY = "property";

    private String description;
    private boolean XMLDescription;

    private String user;
    private boolean XMLUser;

    private SerializableProtectedString password;
    private boolean XMLPassword;

    private Map<String, String> properties;
    private final Set<String> XMLProperties = new HashSet<String>();

    private String storeProtocol;
    private boolean XMLStoreProtocol;

    private String storeProtocolClassName;
    private boolean XMLStoreProtocolClassName;

    private String transportProtocol;
    private boolean XMLTransportProtocol;

    private String transportProtocolClassName;
    private boolean XMLTransportProtocolClassName;

    private String host;
    private boolean XMLHost;

    private String from;
    private boolean XMLFrom;

    /**
     * @param annotation
     * @param nameSpaceConfig
     */
    public MailSessionDefinitionInjectionBinding(String jndiName, ComponentNameSpaceConfiguration nameSpaceConfig) {

        super(null, nameSpaceConfig);
        setJndiName(jndiName);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.injectionengine.InjectionBinding#merge(java.lang.annotation.Annotation, java.lang.Class, java.lang.reflect.Member)
     */
    @Override
    public void merge(@Sensitive MailSessionDefinition annotation, Class<?> instanceClass, Member member) throws InjectionException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "merge: name=" + getJndiName() + ", " + super.toStringSecure(annotation));

        if (member != null) {
            // MailSessionDefinition is a class-level annotation only.
            throw new IllegalArgumentException(member.toString());
        }

        description = mergeAnnotationValue(description, XMLDescription, annotation.description(), KEY_DESCRIPTION, "");
        user = mergeAnnotationValue(user, XMLUser, annotation.user(), KEY_USER, "");
        if (password != null)
            password = (SerializableProtectedString) mergeAnnotationValue(password.getChars(), XMLPassword, annotation.password().toCharArray(), KEY_PASSWORD, "");
        host = mergeAnnotationValue(host, XMLHost, annotation.host(), KEY_HOST, "");
        from = mergeAnnotationValue(from, XMLFrom, annotation.from(), KEY_FROM, "");
        properties = mergeAnnotationProperties(properties, XMLProperties, annotation.properties());
        storeProtocol = mergeAnnotationValue(storeProtocol, XMLStoreProtocol, annotation.storeProtocol(), KEY_STORE_PROTOCOL, "");
        storeProtocolClassName = mergeAnnotationValue(storeProtocolClassName, XMLStoreProtocolClassName, annotation.storeProtocol(), KEY_STORE_PROTOCOL_CLASS_NAME, "");
        transportProtocol = mergeAnnotationValue(transportProtocol, XMLTransportProtocol, annotation.transportProtocol(), KEY_TRANSPORT_PROTOCOL, "");
        transportProtocolClassName = mergeAnnotationValue(transportProtocolClassName, XMLTransportProtocolClassName, annotation.storeProtocol(), KEY_TRANSPORT_PROTOCOL_CLASS_NAME,
                                                          "");

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "merge");
    }

    void resolve() throws InjectionException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "resolve");

        Map<String, Object> props = new HashMap<String, Object>();

        if (properties != null) {
            int i = 0;
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                String key = entry.getKey();
                props.put(KEY_PROPERTY + "." + i + ".name", key);
                props.put(KEY_PROPERTY + "." + i + ".value", entry.getValue());
                i++;
            }
        }

        // Insert all remaining attributes.
        addOrRemoveProperty(props, KEY_DESCRIPTION, description);
        addOrRemoveProperty(props, KEY_FROM, from);
        addOrRemoveProperty(props, KEY_HOST, host);
        addOrRemoveProperty(props, KEY_PASSWORD, password);
        addOrRemoveProperty(props, KEY_STORE_PROTOCOL, storeProtocol);
        addOrRemoveProperty(props, KEY_STORE_PROTOCOL_CLASS_NAME, storeProtocolClassName);
        addOrRemoveProperty(props, KEY_TRANSPORT_PROTOCOL, transportProtocol);
        addOrRemoveProperty(props, KEY_TRANSPORT_PROTOCOL_CLASS_NAME, transportProtocolClassName);
        addOrRemoveProperty(props, KEY_USER, user);

        setObjects(null, createDefinitionReference(null, javax.mail.Session.class.getName(), props));

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "resolve");
    }

    void mergeXML(MailSession mailSession) throws InjectionConfigurationException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "mergeXML: name=" + getJndiName() + ", " + mailSession.toString());

        List<Description> descriptionList = mailSession.getDescriptions();

        if (descriptionList != null) {
            Iterator<Description> iter = descriptionList.iterator();
            StringBuilder descSB = new StringBuilder();
            while (iter.hasNext()) {
                descSB.append(iter.next().getValue());
                descSB.append(" ");
            }
            description = mergeXMLValue(description, descSB.toString().trim(), "description", KEY_DESCRIPTION, null);
            XMLDescription = true;
        }

        String fromValue = mailSession.getFrom();
        if (fromValue != null) {
            from = mergeXMLValue(from, fromValue, "from", KEY_FROM, null);
            XMLFrom = true;
        }

        String hostValue = mailSession.getHost();
        if (hostValue != null) {
            host = mergeXMLValue(host, hostValue, "host", KEY_HOST, null);
            XMLHost = true;
        }

        String userValue = mailSession.getUser();
        if (userValue != null) {
            user = mergeXMLValue(user, userValue, "user", KEY_USER, null);
            XMLUser = true;
        }

        String passwordValue = mailSession.getPassword();
        if (passwordValue != null && password != null) {
            password = (SerializableProtectedString) mergeXMLValue(password.getChars(), passwordValue, "password", KEY_PASSWORD, null);
            XMLPassword = true;
        }

        String storeProtocolValue = mailSession.getStoreProtocol();
        if (storeProtocolValue != null) {
            storeProtocol = mergeXMLValue(storeProtocol, storeProtocolValue, "store-protocol", KEY_STORE_PROTOCOL, null);
            XMLStoreProtocol = true;
        }

        String storeProtocolClassNameValue = mailSession.getStoreProtocolClassName();
        if (storeProtocolClassNameValue != null) {
            storeProtocolClassName = mergeXMLValue(storeProtocolClassName, storeProtocolClassNameValue, "store-protocol-class-name",
                                                   KEY_STORE_PROTOCOL_CLASS_NAME, null);
            XMLStoreProtocolClassName = true;
        }

        String transportProtocolValue = mailSession.getTransportProtocol();
        if (transportProtocolValue != null) {
            transportProtocol = mergeXMLValue(transportProtocol, transportProtocolValue, "transport-protocol", KEY_TRANSPORT_PROTOCOL, null);
            XMLTransportProtocol = true;
        }

        String transportProtocolClassNameValue = mailSession.getTransportProtocolClassName();
        if (transportProtocolClassNameValue != null) {
            transportProtocolClassName = mergeXMLValue(transportProtocolClassName, transportProtocolClassNameValue, "transport-protocol-class-name",
                                                       KEY_TRANSPORT_PROTOCOL_CLASS_NAME, null);
            XMLTransportProtocolClassName = true;
        }

        List<Property> props = mailSession.getProperties();
        properties = mergeXMLProperties(properties, XMLProperties, props);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "mergeXML");
    }

    @Override
    public void mergeSaved(InjectionBinding<MailSessionDefinition> injectionBinding) throws InjectionException {
        MailSessionDefinitionInjectionBinding mailSessionBinding = (MailSessionDefinitionInjectionBinding) injectionBinding;

        mergeSavedValue(description, mailSessionBinding.description, "description");
        mergeSavedValue(user, mailSessionBinding.user, "user");
        if (password != null)
            mergeSavedValue(password, mailSessionBinding.password, "password");
        mergeSavedValue(storeProtocol, mailSessionBinding.storeProtocol, "store-protocol");
        mergeSavedValue(storeProtocolClassName, mailSessionBinding.storeProtocolClassName, "store-protocol-class-name");
        mergeSavedValue(transportProtocol, mailSessionBinding.transportProtocol, "transport-protocol");
        mergeSavedValue(transportProtocolClassName, mailSessionBinding.transportProtocolClassName, "transport-protocol-class-name");
        mergeSavedValue(host, mailSessionBinding.host, "host");
        mergeSavedValue(from, mailSessionBinding.from, "from");
        mergeSavedValue(properties, mailSessionBinding.properties, "properties");
    }

    @Override
    public Class<?> getAnnotationType() {
        return MailSessionDefinition.class;
    }

}
