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
package com.ibm.wsspi.injectionengine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.ibm.ws.javaee.dd.common.JNDIEnvironmentRef;
import com.ibm.ws.javaee.dd.common.JNDIEnvironmentRefs;

/**
 * Enumeration of all the types of items under {@link JNDIEnvironmentRefs}.
 */
public enum JNDIEnvironmentRefType {
    EnvEntry(com.ibm.ws.javaee.dd.common.EnvEntry.class, "env-entry", "env-entry-name", "Resource", "env-entry") {
        @Override
        public List<? extends JNDIEnvironmentRef> getRefs(JNDIEnvironmentRefs refs) {
            return refs.getEnvEntries();
        }
    },

    EJBRef(com.ibm.ws.javaee.dd.common.EJBRef.class, "ejb-ref", "ejb-ref-name", "EJB", "ejb-ref") {
        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public List<? extends JNDIEnvironmentRef> getRefs(JNDIEnvironmentRefs refs) {
            List ejbRefs = refs.getEJBRefs();
            List ejbLocalRefs = refs.getEJBLocalRefs();

            if (ejbRefs.isEmpty()) {
                return ejbLocalRefs;
            }
            if (ejbLocalRefs.isEmpty()) {
                return ejbRefs;
            }

            List allRefs = new ArrayList(ejbRefs.size() + ejbLocalRefs.size());
            allRefs.addAll(ejbRefs);
            allRefs.addAll(ejbLocalRefs);
            return allRefs;
        }
    },

    ServiceRef(com.ibm.ws.javaee.dd.common.wsclient.ServiceRef.class, "service-ref", "service-ref-name", "WebServiceRef", null) {
        @Override
        public List<? extends JNDIEnvironmentRef> getRefs(JNDIEnvironmentRefs refs) {
            return refs.getServiceRefs();
        }
    },

    ResourceRef(com.ibm.ws.javaee.dd.common.ResourceRef.class, "resource-ref", "res-ref-name", "Resource", "resource-ref") {
        @Override
        public List<? extends JNDIEnvironmentRef> getRefs(JNDIEnvironmentRefs refs) {
            return refs.getResourceRefs();
        }
    },

    ResourceEnvRef(com.ibm.ws.javaee.dd.common.ResourceEnvRef.class, "resource-env-ref", "resource-env-ref-name", "Resource", "resource-env-ref") {
        @Override
        public List<? extends JNDIEnvironmentRef> getRefs(JNDIEnvironmentRefs refs) {
            return refs.getResourceEnvRefs();
        }
    },

    MessageDestinationRef(com.ibm.ws.javaee.dd.common.MessageDestinationRef.class, "message-destination-ref", "message-destination-ref-name", "Resource", "message-destination-ref") {
        @Override
        public List<? extends JNDIEnvironmentRef> getRefs(JNDIEnvironmentRefs refs) {
            return refs.getMessageDestinationRefs();
        }
    },

    PersistenceContextRef(com.ibm.ws.javaee.dd.common.PersistenceContextRef.class, "persistence-context-ref", "persistence-context-ref-name", "PersistenceContextRef", null) {
        @Override
        public List<? extends JNDIEnvironmentRef> getRefs(JNDIEnvironmentRefs refs) {
            return refs.getPersistenceContextRefs();
        }
    },

    PersistenceUnitRef(com.ibm.ws.javaee.dd.common.PersistenceUnitRef.class, "persistence-unit-ref", "persistence-unit-ref-name", "PersistenceUnitRef", null) {
        @Override
        public List<? extends JNDIEnvironmentRef> getRefs(JNDIEnvironmentRefs refs) {
            return refs.getPersistenceUnitRefs();
        }
    },

    DataSource(com.ibm.ws.javaee.dd.common.DataSource.class, "data-source", "name", "DataSourceDefinition", "data-source") {
        @Override
        public List<? extends JNDIEnvironmentRef> getRefs(JNDIEnvironmentRefs refs) {
            return refs.getDataSources();
        }
    },

    JMSConnectionFactory(com.ibm.ws.javaee.dd.common.JMSConnectionFactory.class, "jms-connection-factory", "name", "JMSConnectionFactoryDefinition", null) {
        @Override
        public List<? extends JNDIEnvironmentRef> getRefs(JNDIEnvironmentRefs refs) {
            return refs.getJMSConnectionFactories();
        }
    },

    JMSDestination(com.ibm.ws.javaee.dd.common.JMSDestination.class, "jms-destination", "name", "JMSDestinationDefinition", null) {
        @Override
        public List<? extends JNDIEnvironmentRef> getRefs(JNDIEnvironmentRefs refs) {
            return refs.getJMSDestinations();
        }
    },

    MailSession(com.ibm.ws.javaee.dd.common.MailSession.class, "mail-session", "name", "MailSessionDefinition", null) {
        @Override
        public List<? extends JNDIEnvironmentRef> getRefs(JNDIEnvironmentRefs refs) {
            return refs.getMailSessions();
        }
    },

    ConnectionFactory(com.ibm.ws.javaee.dd.common.ConnectionFactory.class, "connection-factory", "name", "ConnectionFactoryDefinition", null) {
        @Override
        public List<? extends JNDIEnvironmentRef> getRefs(JNDIEnvironmentRefs refs) {
            return refs.getConnectionFactories();
        }
    },

    AdministeredObject(com.ibm.ws.javaee.dd.common.AdministeredObject.class, "administered-object", "name", "AdministeredObjectDefinition", null) {
        @Override
        public List<? extends JNDIEnvironmentRef> getRefs(JNDIEnvironmentRefs refs) {
            return refs.getAdministeredObjects();
        }
    };

    public static final List<JNDIEnvironmentRefType> VALUES = Arrays.asList(values());

    /**
     * Update {@code allRefs} with the objects in {@code refs}. If the map
     * already has objects for a type, the new objects are appended to the list.
     *
     * @param allRefs the map of objects to update
     * @param refs the source of new objects
     */
    public static void addAllRefs(Map<JNDIEnvironmentRefType, List<? extends JNDIEnvironmentRef>> allRefs, JNDIEnvironmentRefs refs) {
        for (JNDIEnvironmentRefType refType : VALUES) {
            refType.addAllRefs(allRefs, refType.getRefs(refs));
        }
    }

    /**
     * Update {@code allRefs} with the objects in {@code compNSConfig}. If the map
     * already has objects for a type, the new objects are appended to the list.
     *
     * @param allRefs the map of objects to update
     * @param compNSConfig the source of new objects
     */
    public static void addAllRefs(Map<JNDIEnvironmentRefType, List<? extends JNDIEnvironmentRef>> allRefs, ComponentNameSpaceConfiguration compNSConfig) {
        for (JNDIEnvironmentRefType refType : VALUES) {
            refType.addAllRefs(allRefs, compNSConfig.getJNDIEnvironmentRefs(refType.getType()));
        }
    }

    /**
     * Update {@code compNSConfig} with the objects in {@code allRefs}. If the
     * configuration already has objects from the type, they will be replaced
     * with the objects in the map.
     *
     * @param allRefs the map of objects
     * @param refs the configuration to update
     */
    @SuppressWarnings("unchecked")
    public static void setAllRefs(ComponentNameSpaceConfiguration compNSConfig, Map<JNDIEnvironmentRefType, List<? extends JNDIEnvironmentRef>> allRefs) {
        for (JNDIEnvironmentRefType refType : VALUES) {
            @SuppressWarnings("rawtypes")
            List refs = allRefs.get(refType);
            compNSConfig.setJNDIEnvironmentRefs(refType.getType(), refs);
        }
    }

    private final Class<? extends JNDIEnvironmentRef> ivType;
    private final String ivXMLElementName;
    private final String ivNameXMLElementName;
    private final String ivAnnotationShortName;
    private final String ivBindingElementName;

    JNDIEnvironmentRefType(Class<? extends JNDIEnvironmentRef> type,
                           String xmlElementName,
                           String nameXMLElementName,
                           String annotationShortName,
                           String bindingElementName) {
        ivType = type;
        ivXMLElementName = xmlElementName;
        ivNameXMLElementName = nameXMLElementName;
        ivAnnotationShortName = annotationShortName;
        ivBindingElementName = bindingElementName;
    }

    public Class<? extends JNDIEnvironmentRef> getType() {
        return ivType;
    }

    public String getXMLElementName() {
        return ivXMLElementName;
    }

    public String getNameXMLElementName() {
        return ivNameXMLElementName;
    }

    public String getAnnotationShortName() {
        return ivAnnotationShortName;
    }

    public String getNameAnnotationElementName() {
        return "name";
    }

    public String getBindingElementName() {
        return ivBindingElementName;
    }

    public String getBindingAttributeName() {
        return ivBindingElementName == null ? null : "binding-name";
    }

    public abstract List<? extends JNDIEnvironmentRef> getRefs(JNDIEnvironmentRefs refs);

    /**
     * Add {@code ref} to {@code allRefs}.
     *
     * @param allRefs the map of objects to update
     * @param ref the ref to add
     * @throws ClassCastException if the new object is of the wrong type
     */
    @SuppressWarnings("unchecked")
    public void addRef(Map<JNDIEnvironmentRefType, List<? extends JNDIEnvironmentRef>> allRefs, JNDIEnvironmentRef ref) {
        getType().cast(ref);

        @SuppressWarnings("rawtypes")
        List oldRefs = allRefs.get(this);
        if (oldRefs == null) {
            @SuppressWarnings("rawtypes")
            List newRefs = new ArrayList<JNDIEnvironmentRef>();
            newRefs.add(ref);
            allRefs.put(this, newRefs);
        } else {
            oldRefs.add(ref);
        }
    }

    /**
     * Add {@code refs} to {@code allRefs}.
     *
     * @param allRefs the map of objects to update
     * @param ref the new objects to add
     * @throws ClassCastException if a new object is of the wrong type
     */
    @SuppressWarnings("unchecked")
    public void addAllRefs(Map<JNDIEnvironmentRefType, List<? extends JNDIEnvironmentRef>> allRefs, List<? extends JNDIEnvironmentRef> refs) {
        if (refs != null && !refs.isEmpty()) {
            // Ensure everything being added has the right type.
            for (JNDIEnvironmentRef ref : refs) {
                getType().cast(ref);
            }

            @SuppressWarnings("rawtypes")
            List oldRefs = allRefs.get(this);
            if (oldRefs == null) {
                // Copy the list to avoid mutating this source list if
                // addRef/addAllRefs is subsequently called.
                allRefs.put(this, new ArrayList<JNDIEnvironmentRef>(refs));
            } else {
                oldRefs.addAll(refs);
            }
        }
    }
}
