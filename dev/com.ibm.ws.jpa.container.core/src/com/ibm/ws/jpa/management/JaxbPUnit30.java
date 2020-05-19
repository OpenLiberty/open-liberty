/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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

import java.util.List;
import java.util.Properties;

import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.PersistenceUnitTransactionType;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.jpa.pxml30.Persistence.PersistenceUnit;
import com.ibm.ws.jpa.pxml30.PersistenceUnitCachingType;
import com.ibm.ws.jpa.pxml30.PersistenceUnitValidationModeType;

public class JaxbPUnit30 extends JaxbPUnit {
    private static final String CLASS_NAME = JaxbPUnit30.class.getName();
    private static final TraceComponent tc = Tr.register(JaxbPUnit30.class, JPA_TRACE_GROUP, JPA_RESOURCE_BUNDLE_NAME);

    private final PersistenceUnit ivPUnit;

    /**
     * Constructs a JaxbPUnit30 instance with the JAXB PersistenceUnit instance for which the
     * created instance will provide an abstraction.
     **/
    JaxbPUnit30(PersistenceUnit pUnit) {
        if (pUnit == null) {
            throw new IllegalArgumentException("null parameter");
        }
        ivPUnit = pUnit;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, toString());
    }

    /**
     * Gets the value of the description property.
     *
     * @return value of the description property.
     */
    @Override
    public String getDescription() {
        return ivPUnit.getDescription();
    }

    /**
     * Gets the value of the provider property.
     *
     * @return value of the provider property.
     */
    @Override
    public String getProvider() {
        return ivPUnit.getProvider();
    }

    /**
     * Gets the value of the jtaDataSource property.
     *
     * @return value of the jtaDataSource property.
     */
    @Override
    public String getJtaDataSource() {
        return ivPUnit.getJtaDataSource();
    }

    /**
     * Gets the value of the nonJtaDataSource property.
     *
     * @return value of the nonJtaDataSource property.
     */
    @Override
    public String getNonJtaDataSource() {
        return ivPUnit.getNonJtaDataSource();
    }

    /**
     * Gets the value of the mappingFile property.
     * <p>
     *
     * This accessor method returns a reference to the live list, not a snapshot. Therefore any
     * modification you make to the returned list will be present inside the JAXB object. This is
     * why there is not a <CODE>set</CODE> method for the mappingFile property.
     * <p>
     *
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getMappingFile().add(newItem);
     * </pre>
     *
     * <p>
     *
     * Objects of the following type(s) are allowed in the list {@link String }
     */
    @Override
    public List<String> getMappingFile() {
        return ivPUnit.getMappingFile();
    }

    /**
     * Gets the value of the jarFile property.
     * <p>
     *
     * This accessor method returns a reference to the live list, not a snapshot. Therefore any
     * modification you make to the returned list will be present inside the JAXB object. This is
     * why there is not a <CODE>set</CODE> method for the jarFile property.
     * <p>
     *
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getJarFile().add(newItem);
     * </pre>
     *
     * Objects of the following type(s) are allowed in the list {@link String }
     */
    @Override
    public List<String> getJarFile() {
        return ivPUnit.getJarFile();
    }

    /**
     * Gets the value of the clazz property.
     * <p>
     *
     * This accessor method returns a reference to the live list, not a snapshot. Therefore any
     * modification you make to the returned list will be present inside the JAXB object. This is
     * why there is not a <CODE>set</CODE> method for the clazz property.
     * <p>
     *
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getClazz().add(newItem);
     * </pre>
     *
     * Objects of the following type(s) are allowed in the list {@link String }
     */
    @Override
    public List<String> getClazz() {
        return ivPUnit.getClazz();
    }

    /**
     * Gets the value of the excludeUnlistedClasses property.
     * <p>
     *
     * Note that the default value defined in the 2.2 persistence xsd is 'true', however, that is
     * for when the stanza is specified, but without a value. If the stanza is not present in
     * persistence.xml, then the default is 'false'.
     * <p>
     *
     * @return value of the excludeUnlistedClasses property.
     */
    @Override
    public boolean isExcludeUnlistedClasses() {
        // The JAXB generated classes will return 'true' if the stanza was
        // specified, but without a value
        Boolean exclude = ivPUnit.isExcludeUnlistedClasses();

        // The version 2.2 persistence.xsd spelled out the default for
        // exclude-unlisted-classes is true (changed from version 1.0).
        //
        // This means there are 4 choices:
        // <exclude-unlisted-classes> not specified --> Not excluded
        // <exclude-unlisted-classes>false</exclude-unlisted-classes> --> Not excluded
        // <exclude-unlisted-classes>true</exclude-unlisted-classes> --> Excluded
        // <exclude-unlisted-classes/> --> Excluded
        //
        // The last choice matches the sample's implied semantics.

        // The default when not present at all is 'false'
        if (exclude == null) {
            exclude = Boolean.FALSE;

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "isExcludeUnlistedClasses : defaulted to FALSE");
        }

        return exclude.booleanValue();
    }

    /**
     * Gets the value of the sharedCacheMode property.
     *
     * @return value of the sharedCacheMode property.
     */
    @Override
    public SharedCacheMode getSharedCacheMode() {
        // Convert this SharedCacheMode from the class defined
        // in JAXB (com.ibm.ws.jpa.pxml30.PersistenceUnitCachingType)
        // to JPA (javax.persistence.SharedCacheMode).

        // Per the spec, must return UNSPECIFIED if not in xml
        SharedCacheMode rtnMode = SharedCacheMode.UNSPECIFIED;
        PersistenceUnitCachingType jaxbMode = null;

        jaxbMode = ivPUnit.getSharedCacheMode();
        if (jaxbMode == PersistenceUnitCachingType.ALL) {
            rtnMode = SharedCacheMode.ALL;
        } else if (jaxbMode == PersistenceUnitCachingType.NONE) {
            rtnMode = SharedCacheMode.NONE;
        } else if (jaxbMode == PersistenceUnitCachingType.ENABLE_SELECTIVE) {
            rtnMode = SharedCacheMode.ENABLE_SELECTIVE;
        } else if (jaxbMode == PersistenceUnitCachingType.DISABLE_SELECTIVE) {
            rtnMode = SharedCacheMode.DISABLE_SELECTIVE;
        }

        return rtnMode;
    }

    /**
     * Gets the value of the validationMode property.
     *
     * @return value of the validationMode property.
     */
    @Override
    public ValidationMode getValidationMode() {
        // Convert this ValidationMode from the class defined
        // in JAXB (com.ibm.ws.jpa.pxml30.PersistenceUnitValidationModeType)
        // to JPA (javax.persistence.ValidationMode).

        ValidationMode rtnMode = null;
        PersistenceUnitValidationModeType jaxbMode = null;

        jaxbMode = ivPUnit.getValidationMode();
        if (jaxbMode == PersistenceUnitValidationModeType.AUTO) {
            rtnMode = ValidationMode.AUTO;
        } else if (jaxbMode == PersistenceUnitValidationModeType.CALLBACK) {
            rtnMode = ValidationMode.CALLBACK;
        } else if (jaxbMode == PersistenceUnitValidationModeType.NONE) {
            rtnMode = ValidationMode.NONE;
        }

        return rtnMode;
    }

    /**
     * Gets the value of the properties property.
     *
     * @return value of the properties property.
     */
    @Override
    public Properties getProperties() {
        Properties rtnProperties = null;

        // Convert this Properties from the class defined in JAXB
        // (com.ibm.ws.jpa.pxml30.Persistence.PersistenceUnit.Properties)
        // to standard JDK classes (java.util.Properties).

        com.ibm.ws.jpa.pxml30.Persistence.PersistenceUnit.Properties puProperties = ivPUnit.getProperties();

        if (puProperties != null) {
            List<com.ibm.ws.jpa.pxml30.Persistence.PersistenceUnit.Properties.Property> propertyList = puProperties.getProperty();
            if (propertyList != null && !propertyList.isEmpty()) {
                rtnProperties = new Properties();
                for (com.ibm.ws.jpa.pxml30.Persistence.PersistenceUnit.Properties.Property puProperty : propertyList) {
                    // It is possible that a syntax error will exist in the persistence.xml
                    // where the property or value is null. Neither is acceptable for
                    // a Hashtable and will result in an exception.
                    try {
                        rtnProperties.setProperty(puProperty.getName(), puProperty.getValue());
                    } catch (Throwable ex) {
                        FFDCFilter.processException(ex, CLASS_NAME + ".getProperties", "219", this);
                        Tr.error(tc, "PROPERTY_SYNTAX_ERROR_IN_PERSISTENCE_XML_CWWJP0039E",
                                 ivPUnit.getName(), puProperty.getName(), puProperty.getValue(), ex);
                        String exMsg = "A severe error occurred while processing the properties "
                                       + "within the persistence.xml of Persistence Unit: " + ivPUnit.getName()
                                       + " (Property = " + puProperty.getName() + ", Value = " + puProperty.getValue()
                                       + ").";
                        throw new RuntimeException(exMsg, ex);
                    }
                }
            }
        }
        return rtnProperties;
    }

    /**
     * Gets the value of the name property.
     *
     * @return value of the name property.
     */
    @Override
    public String getName() {
        return ivPUnit.getName();
    }

    /**
     * Gets the value of the transactionType property.
     *
     * @return value of the transactionType property.
     */
    @Override
    public PersistenceUnitTransactionType getTransactionType() {
        // Convert this TransactionType from the class defined in JAXB
        // (com.ibm.ws.jpa.pxml30.PersistenceUnitTransactionType) to JPA
        // (javax.persistence.spi.PersistenceUnitTransactionType).

        PersistenceUnitTransactionType rtnType = null;
        com.ibm.ws.jpa.pxml30.PersistenceUnitTransactionType jaxbType = null;

        jaxbType = ivPUnit.getTransactionType();
        if (jaxbType == com.ibm.ws.jpa.pxml30.PersistenceUnitTransactionType.JTA) {
            rtnType = PersistenceUnitTransactionType.JTA;
        } else if (jaxbType == com.ibm.ws.jpa.pxml30.PersistenceUnitTransactionType.RESOURCE_LOCAL) {
            rtnType = PersistenceUnitTransactionType.RESOURCE_LOCAL;
        }
        return rtnType;
    }
}
