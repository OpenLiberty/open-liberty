/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.management;

import static com.ibm.ws.jpa.management.JPAConstants.JPA_RESOURCE_BUNDLE_NAME;
import static com.ibm.ws.jpa.management.JPAConstants.JPA_TRACE_GROUP;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.util.List;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.PersistenceProperty;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.javaee.dd.common.PersistenceContextRef;
import com.ibm.ws.javaee.dd.common.Property;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionConfigurationException;
import com.ibm.wsspi.injectionengine.InjectionException;

/**
 * @PersistenceContext injection binding.
 **/
class JPAPCtxtInjectionBinding extends AbstractJPAInjectionBinding<PersistenceContext> {
    private static final TraceComponent tc = Tr.register(JPAPCtxtInjectionBinding.class,
                                                         JPA_TRACE_GROUP,
                                                         JPA_RESOURCE_BUNDLE_NAME);

    private final JPAPCtxtAttributeAccessor ivAttributeAccessor;

    private boolean ivPuFromXML;

    private Boolean ivExtendedType;
    private boolean ivTypeFromXML;

    private Boolean ivUnsynchronized;
    private boolean ivSynchronizationFromXML;

    private Properties ivProperties;

    /**
     * XML based constructor.
     *
     * @param pCtxtRef XML representation of persistence context metadata.
     * @param compNSConfig component name space configuration metadata.
     */
    // d662814
    public JPAPCtxtInjectionBinding(PersistenceContextRef pCtxtRef,
                                    ComponentNameSpaceConfiguration compNSConfig,
                                    JPAPCtxtAttributeAccessor attributeAccessor) throws InjectionException {
        super(newPersistenceContext(pCtxtRef.getName(), pCtxtRef.getPersistenceUnitName(), pCtxtRef.getTypeValue(),
                                    pCtxtRef.getProperties()), pCtxtRef.getName(), pCtxtRef.getPersistenceUnitName(), compNSConfig);
        ivAttributeAccessor = attributeAccessor;
        setInjectionClassType(EntityManager.class);

        String pUnitName = pCtxtRef.getPersistenceUnitName();
        if (pUnitName != null && pUnitName.length() > 0) {
            ivPuFromXML = true;
        }

        int type = pCtxtRef.getTypeValue();
        if (type != PersistenceContextRef.TYPE_UNSPECIFIED) {
            ivExtendedType = type == PersistenceContextRef.TYPE_EXTENDED;
            ivTypeFromXML = true;
        }

        int synchronization = pCtxtRef.getSynchronizationValue();
        if (synchronization != PersistenceContextRef.SYNCHRONIZATION_UNSPECIFIED) {
            ivUnsynchronized = synchronization == PersistenceContextRef.SYNCHRONIZATION_UNSYNCHRONIZED;
            ivSynchronizationFromXML = true;
        }

        ivProperties = getPersistenceProperties(pCtxtRef);
    }

    /**
     * Annotation based constructor.
     *
     * @param pCtxt persistence unit annotation.
     * @param compNSConfig component name space configuration metadata.
     */
    public JPAPCtxtInjectionBinding(PersistenceContext pCtxt,
                                    ComponentNameSpaceConfiguration compNSConfig,
                                    JPAPCtxtAttributeAccessor attributeAccessor) throws InjectionException {
        super(pCtxt, pCtxt.name(), pCtxt.unitName(), compNSConfig);
        ivAttributeAccessor = attributeAccessor;
        setInjectionClassType(EntityManager.class);

        ivExtendedType = pCtxt.type() == PersistenceContextType.EXTENDED;
        ivUnsynchronized = attributeAccessor.isUnsynchronized(pCtxt);
        ivProperties = getPersistenceProperties(pCtxt);
    }

    public boolean isExtendedType() {
        return ivExtendedType != null && ivExtendedType;
    }

    public boolean isUnsynchronized() {
        return ivUnsynchronized != null && ivUnsynchronized;
    }

    public Properties getProperties() {
        return ivProperties;
    }

    /**
     * Extract the fields from the PersistenceContextRef, and verify they match
     * the values in the current binding object and/or annotation exactly.
     *
     * @param pCtxtRef reference with same name to merge
     * @throws InjectionException if the fields of the two references are not
     *             compatible.
     */
    // d658856
    public void merge(PersistenceContextRef pCtxtRef) throws InjectionException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "merge : " + pCtxtRef);

        String thisUnitName = getPuName();
        String mergedUnitName = pCtxtRef.getPersistenceUnitName();
        int mergedType = pCtxtRef.getTypeValue();
        Boolean mergedExtendedType = null;
        if (mergedType != PersistenceContextRef.TYPE_UNSPECIFIED) {
            mergedExtendedType = mergedType == PersistenceContextRef.TYPE_EXTENDED;
        }
        int mergedSync = pCtxtRef.getSynchronizationValue();
        Boolean mergedUnsynchronized = null;
        if (mergedSync != PersistenceContextRef.SYNCHRONIZATION_UNSPECIFIED) {
            mergedUnsynchronized = mergedSync == PersistenceContextRef.SYNCHRONIZATION_UNSYNCHRONIZED;
        }
        Properties mergedProperties = getPersistenceProperties(pCtxtRef);

        if (isTraceOn && tc.isDebugEnabled()) {
            Tr.debug(tc, "new=" + getJndiName() + ":" + mergedUnitName + ":" +
                         mergedExtendedType + ":" + mergedUnsynchronized + ":" + mergedProperties);
            Tr.debug(tc, "cur=" + getJndiName() + ":" + thisUnitName + ":" +
                         ivExtendedType + ":" + ivProperties);
        }

        // Merge the persistence unit name. Either one must be not set or they
        // must match exactly.
        if (thisUnitName == null || thisUnitName.equals("")) {
            if (mergedUnitName != null) {
                setPuName(mergedUnitName);
                ivPuFromXML = true; // d662814
            }
        } else if (mergedUnitName != null && !mergedUnitName.equals("")) {
            if (!thisUnitName.equals(mergedUnitName)) {
                Tr.error(tc, "CONFLICTING_XML_VALUES_CWWJP0041E",
                         ivNameSpaceConfig.getModuleName(),
                         ivNameSpaceConfig.getApplicationName(),
                         "persistence-unit-name",
                         "persistence-context-ref",
                         "persistence-context-ref-name",
                         getJndiName(),
                         thisUnitName,
                         mergedUnitName);
                String exMsg = "CWWJP0041E: The " + ivNameSpaceConfig.getModuleName() +
                               " module of the " + ivNameSpaceConfig.getApplicationName() +
                               " application has conflicting configuration data in the XML" +
                               " deployment descriptor. Conflicting " + "persistence-unit-name" +
                               " element values exist for multiple " + "persistence-context-ref" +
                               " elements with the same " + "persistence-context-ref-name" +
                               " element value : " + getJndiName() + ". The conflicting " +
                               "persistence-unit-name" + " element values are " + thisUnitName +
                               " and " + mergedUnitName + ".";
                throw new InjectionConfigurationException(exMsg);
            }
        }

        // Merge the persistence context type. Either one must be not set or they
        // must match exactly.
        if (ivExtendedType == null) {
            if (mergedExtendedType != null) {
                ivExtendedType = mergedExtendedType;
                ivTypeFromXML = true; // d662814
            }
        } else if (mergedExtendedType != null) {
            if (ivExtendedType.booleanValue() != mergedExtendedType.booleanValue()) {
                Tr.error(tc, "CONFLICTING_XML_VALUES_CWWJP0041E",
                         ivNameSpaceConfig.getModuleName(),
                         ivNameSpaceConfig.getApplicationName(),
                         "persistence-context-type",
                         "persistence-context-ref",
                         "persistence-context-ref-name",
                         getJndiName(),
                         getTypeName(ivExtendedType),
                         getTypeName(mergedExtendedType));
                String exMsg = "CWWJP0041E: The " + ivNameSpaceConfig.getModuleName() +
                               " module of the " + ivNameSpaceConfig.getApplicationName() +
                               " application has conflicting configuration data in the XML" +
                               " deployment descriptor. Conflicting " + "persistence-context-type" +
                               " element values exist for multiple " + "persistence-context-ref" +
                               " elements with the same " + "persistence-context-ref-name" +
                               " element value : " + getJndiName() + ". The conflicting " +
                               "persistence-context-type" + " element values are " +
                               getTypeName(ivExtendedType) +
                               " and " + getTypeName(mergedExtendedType) + ".";
                throw new InjectionConfigurationException(exMsg);
            }
        }

        if (ivUnsynchronized == null) {
            if (mergedUnsynchronized != null) {
                ivUnsynchronized = mergedUnsynchronized;
                ivSynchronizationFromXML = true;
            }
        } else if (mergedUnsynchronized != null) {
            if (ivUnsynchronized.booleanValue() != mergedUnsynchronized.booleanValue()) {
                Tr.error(tc, "CONFLICTING_XML_VALUES_CWWJP0041E",
                         ivNameSpaceConfig.getModuleName(),
                         ivNameSpaceConfig.getApplicationName(),
                         "persistence-context-synchronization",
                         "persistence-context-ref",
                         "persistence-context-ref-name",
                         getJndiName(),
                         getSynchronizationName(ivUnsynchronized),
                         getSynchronizationName(mergedUnsynchronized));
                String exMsg = "CWWJP0041E: The " + ivNameSpaceConfig.getModuleName() +
                               " module of the " + ivNameSpaceConfig.getApplicationName() +
                               " application has conflicting configuration data in the XML" +
                               " deployment descriptor. Conflicting " + "persistence-context-synchronization" +
                               " element values exist for multiple " + "persistence-context-ref" +
                               " elements with the same " + "persistence-context-ref-name" +
                               " element value : " + getJndiName() + ". The conflicting " +
                               "persistence-context-type" + " element values are " +
                               getSynchronizationName(ivUnsynchronized) +
                               " and " + getSynchronizationName(mergedUnsynchronized) + ".";
                throw new InjectionConfigurationException(exMsg);
            }
        }

        // Merge the persistence context properties. Either one must be not set or they
        // must match exactly.
        if (ivProperties.isEmpty()) {
            ivProperties = mergedProperties;
        } else if (!mergedProperties.isEmpty()) {
            if (!ivProperties.equals(mergedProperties)) {
                Tr.error(tc, "CONFLICTING_XML_VALUES_CWWJP0041E",
                         ivNameSpaceConfig.getModuleName(),
                         ivNameSpaceConfig.getApplicationName(),
                         "persistence-property",
                         "persistence-context-ref",
                         "persistence-context-ref-name",
                         getJndiName(),
                         ivProperties,
                         mergedProperties);
                String exMsg = "CWWJP0041E: The " + ivNameSpaceConfig.getModuleName() +
                               " module of the " + ivNameSpaceConfig.getApplicationName() +
                               " application has conflicting configuration data in the XML" +
                               " deployment descriptor. Conflicting " + "persistence-property" +
                               " element values exist for multiple " + "persistence-context-ref" +
                               " elements with the same " + "persistence-context-ref-name" +
                               " element value : " + getJndiName() + ". The conflicting " +
                               "persistence-property" + " element values are " +
                               ivProperties + " and " + mergedProperties + ".";
                throw new InjectionConfigurationException(exMsg);
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "merge : " + this);
    }

    /**
     * Merges the configuration information of an annotation with a binding
     * object created from previously processed XML or annotations. <p>
     *
     * The may occur when there is an XML override of an annotation, or
     * there are multiple annotations defined with the same name (i.e.
     * a multiple target injection scenario).
     *
     * This method will implement/enforce the deployment descriptor override
     * rules defined in the EJB Specification:
     * <ul>
     * <li> The relevant deployment descriptor entry is located based on the JNDI name
     * used with the annotation (either defaulted or provided explicitly).
     * <li> The persistence-unit-name overrides the unitName element of the annotation. The
     * Application Assembler or Deployer should exercise caution in changing this
     * value, if specified, as doing so is likely to break the application.
     * <li> The persistence-context-type, if specified, overrides the type element of
     * the annotation. In general, the Application Assembler or Deployer should
     * never change the value of this element, as doing so is likely to break
     * the application.
     * <li> Any persistence-property elements are added to those specified by the
     * PersistenceContext annotation. If the name of a specified property is the
     * same as one specified by the PersistenceContext annotation, the value
     * specified in the annotation is overridden.
     * <li> The injection target, if specified, must name exactly the annotated field
     * or property method.
     * </ul>
     *
     * @param annotation the PersistenceContext annotation to be merged
     * @param member the Field or Method associated with the annotation;
     *            null if a class level annotation.
     **/
    // d432816
    @Override
    public void merge(PersistenceContext annotation, Class<?> instanceClass, Member member) throws InjectionException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "merge : " + annotation + ", " + instanceClass + ", " + member);

        String thisUnitName = getPuName();

        // Only need to check values if not overridden by XML.             d662814
        if (!ivPuFromXML) {
            String mergedUnitName = annotation.unitName();
            if (mergedUnitName != null && mergedUnitName.length() > 0) {
                if (!isComplete() && (thisUnitName == null || thisUnitName.length() == 0)) {
                    setPuName(mergedUnitName);
                } else if (!mergedUnitName.equals(thisUnitName)) {
                    // Error - conflicting persistence unit specified         d662814
                    Tr.error(tc, "CONFLICTING_ANNOTATION_VALUES_CWWJP0042E",
                             ivNameSpaceConfig.getDisplayName(),
                             ivNameSpaceConfig.getModuleName(),
                             ivNameSpaceConfig.getApplicationName(),
                             "unitName",
                             "@PersistenceContext",
                             "name",
                             getJndiName(),
                             thisUnitName,
                             mergedUnitName);
                    String exMsg = "The " + ivNameSpaceConfig.getDisplayName() +
                                   " bean in the " + ivNameSpaceConfig.getModuleName() +
                                   " module of the " + ivNameSpaceConfig.getApplicationName() +
                                   " application has conflicting configuration data" +
                                   " in source code annotations. Conflicting " +
                                   "unitName" + " attribute values exist for multiple " +
                                   "@PersistenceContext" + " annotations with the same " +
                                   "name" + " attribute value : " + getJndiName() +
                                   ". The conflicting " + "unitName" +
                                   " attribute values are " + thisUnitName +
                                   " and " + mergedUnitName + ".";
                    throw new InjectionConfigurationException(exMsg);
                }
            }
        }

        // Only need to check values if not overridden by XML.             d662814
        if (!ivTypeFromXML) {
            // If not set from XML, then just use the value from the annotation,
            // note that even if not set in XML, the 'this' value will be the
            // default of 'Transaction'. Also, if multiple annotations are present,
            // then use 'Extended' if any one of them has specified it.
            Boolean mergedExtendedType = annotation.type() == PersistenceContextType.EXTENDED;
            if (!isComplete() && ivExtendedType == null) {
                ivExtendedType = mergedExtendedType;
            }
        }

        if (!ivSynchronizationFromXML) {
            // If not set from XML, then just use the value from the annotation.
            boolean mergedUnsynchronized = ivAttributeAccessor.isUnsynchronized(annotation);
            if (!isComplete() && ivUnsynchronized == null) {
                ivUnsynchronized = mergedUnsynchronized;
            } else if (ivUnsynchronized == null || ivUnsynchronized.booleanValue() != mergedUnsynchronized) {
                // Error - conflicting persistence unit specified         d662814
                Tr.error(tc, "CONFLICTING_ANNOTATION_VALUES_CWWJP0042E",
                         ivNameSpaceConfig.getDisplayName(),
                         ivNameSpaceConfig.getModuleName(),
                         ivNameSpaceConfig.getApplicationName(),
                         "synchronization",
                         "@PersistenceContext",
                         "name",
                         getJndiName(),
                         getSynchronizationName(ivUnsynchronized),
                         getSynchronizationName(mergedUnsynchronized));
                String exMsg = "The " + ivNameSpaceConfig.getDisplayName() +
                               " bean in the " + ivNameSpaceConfig.getModuleName() +
                               " module of the " + ivNameSpaceConfig.getApplicationName() +
                               " application has conflicting configuration data" +
                               " in source code annotations. Conflicting " +
                               "synchronization" + " attribute values exist for multiple " +
                               "@PersistenceContext" + " annotations with the same " +
                               "name" + " attribute value : " + getJndiName() +
                               ". The conflicting " + "synchronization" +
                               " attribute values are " + getSynchronizationName(ivUnsynchronized) +
                               " and " + getSynchronizationName(mergedUnsynchronized) + ".";
                throw new InjectionConfigurationException(exMsg);
            }
        }

        // Merge annotation properties to DD, DD properties override annotation
        // properties and make sure a PersistenceProperty[] object is returned.
        Properties mergedProperties = getPersistenceProperties(annotation);
        if (!isComplete() && !mergedProperties.isEmpty()) {
            mergedProperties.putAll(ivProperties);
            ivProperties = mergedProperties;
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "merge", this);
    }

    @Override
    public void mergeSaved(InjectionBinding<PersistenceContext> injectionBinding) // d681743
                    throws InjectionException {
        JPAPCtxtInjectionBinding pCtxtBinding = (JPAPCtxtInjectionBinding) injectionBinding;

        mergeSavedValue(getPuName(), pCtxtBinding.getPuName(), "persistence-unit-name");
        mergeSavedValue(getTypeName(ivExtendedType), getTypeName(pCtxtBinding.ivExtendedType), "persistence-context-type");
        mergeSavedValue(getSynchronizationName(ivUnsynchronized), getSynchronizationName(pCtxtBinding.ivUnsynchronized), "persistence-context-synchronization");
    }

    private static String getTypeName(Boolean extended) {
        return extended != null && extended ? "EXTENDED" : "TRANSACTION";
    }

    private String getSynchronizationName(Boolean unsynchronized) {
        return unsynchronized != null && unsynchronized ? "UNSYNCHRONIZED" : "SYNCHRONIZED";
    }

    private static Properties getPersistenceProperties(PersistenceContext pCtxt) {
        Properties properties = new Properties();
        for (PersistenceProperty property : pCtxt.properties()) {
            properties.put(property.name(), property.value());
        }

        return properties;
    }

    private static Properties getPersistenceProperties(PersistenceContextRef pCtxtRef) {
        Properties properties = new Properties();
        for (Property property : pCtxtRef.getProperties()) {
            properties.put(property.getName(), property.getValue());
        }
        return properties;
    }

    /**
     * This transient PersistenceContext annotation class has no default value.
     * i.e. null is a valid value for some fields.
     */
    private static PersistenceContext newPersistenceContext(final String fJndiName, final String fUnitName, final int fCtxType,
                                                            final List<Property> fCtxProperties) {
        return new PersistenceContext() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return PersistenceContext.class;
            }

            @Override
            public String name() {
                return fJndiName;
            }

            @Override
            public PersistenceProperty[] properties() {
                //TODO not ideal doing this conversion processing here
                PersistenceProperty[] props = new PersistenceProperty[fCtxProperties.size()];
                int i = 0;
                for (Property property : fCtxProperties) {
                    final String name = property.getName();
                    final String value = property.getValue();
                    PersistenceProperty prop = new PersistenceProperty() {

                        @Override
                        public Class<? extends Annotation> annotationType() {
                            return PersistenceProperty.class;
                        }

                        @Override
                        public String name() {
                            return name;
                        }

                        @Override
                        public String value() {
                            return value;
                        }

                    };
                    props[i++] = prop;
                }
                return props;
            }

            @Override
            public PersistenceContextType type() {
                if (fCtxType == PersistenceContextRef.TYPE_TRANSACTION) {
                    return PersistenceContextType.TRANSACTION;
                } else {
                    return PersistenceContextType.EXTENDED;
                }
            }

            @Override
            public String unitName() {
                return fUnitName;
            }

            @Override
            public String toString() {
                return "JPA.PersistenceContext(name=" + fJndiName +
                       ", unitName=" + fUnitName + ")";
            }
        };
    }
}
