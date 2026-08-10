/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
import com.ibm.ws.jpa.pxml40.Persistence.PersistenceUnit;
import com.ibm.ws.jpa.pxml40.PersistenceUnitCachingType;
import com.ibm.ws.jpa.pxml40.PersistenceUnitDefaultToOneFetchType;
import com.ibm.ws.jpa.pxml40.PersistenceUnitValidationModeType;

/**
 * Concrete implementation of the JaxbPUnit abstraction representing a {@code <persistence-unit>}
 * stanza in a JPA 4.0 version persistence.xml.
 * <p>
 * Adds support for the {@code default-to-one-fetch-type} element introduced in JPA 4.0,
 * inheriting all other field handling from JaxbPUnit32.
 */
public class JaxbPUnit40 extends JaxbPUnit {
    private static final String CLASS_NAME = JaxbPUnit40.class.getName();
    private static final TraceComponent tc = Tr.register(JaxbPUnit40.class, JPA_TRACE_GROUP, JPA_RESOURCE_BUNDLE_NAME);

    private final PersistenceUnit ivPUnit;

    /**
     * Constructs a JaxbPUnit40 instance with the JAXB PersistenceUnit instance for which the
     * created instance will provide an abstraction.
     **/
    JaxbPUnit40(PersistenceUnit pUnit) {
        if (pUnit == null) {
            throw new IllegalArgumentException("null parameter");
        }
        ivPUnit = pUnit;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, toString());
    }

    @Override
    public String getDescription() {
        return ivPUnit.getDescription();
    }

    @Override
    public String getProvider() {
        return ivPUnit.getProvider();
    }

    @Override
    public List<String> getQualifier() {
        return ivPUnit.getQualifier();
    }

    @Override
    public String getScope() {
        return ivPUnit.getScope();
    }

    @Override
    public String getJtaDataSource() {
        return ivPUnit.getJtaDataSource();
    }

    @Override
    public String getNonJtaDataSource() {
        return ivPUnit.getNonJtaDataSource();
    }

    @Override
    public List<String> getMappingFile() {
        return ivPUnit.getMappingFile();
    }

    @Override
    public List<String> getJarFile() {
        return ivPUnit.getJarFile();
    }

    @Override
    public List<String> getClazz() {
        return ivPUnit.getClazz();
    }

    @Override
    public boolean isExcludeUnlistedClasses() {
        Boolean exclude = ivPUnit.isExcludeUnlistedClasses();
        if (exclude == null) {
            exclude = Boolean.FALSE;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "isExcludeUnlistedClasses : defaulted to FALSE");
        }
        return exclude.booleanValue();
    }

    @Override
    public SharedCacheMode getSharedCacheMode() {
        SharedCacheMode rtnMode = SharedCacheMode.UNSPECIFIED;
        PersistenceUnitCachingType jaxbMode = ivPUnit.getSharedCacheMode();
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

    @Override
    public ValidationMode getValidationMode() {
        ValidationMode rtnMode = null;
        PersistenceUnitValidationModeType jaxbMode = ivPUnit.getValidationMode();
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
     * Returns the default fetch type for to-one associations, as specified in the
     * {@code <default-to-one-fetch-type>} element of a JPA 4.0 persistence.xml.
     * Returns {@code null} if not specified; JPAPUnitInfo will then use EAGER as the default.
     */
    @Override
    public javax.persistence.FetchType getDefaultToOneFetchType() {
        PersistenceUnitDefaultToOneFetchType jaxbType = ivPUnit.getDefaultToOneFetchType();
        if (jaxbType == PersistenceUnitDefaultToOneFetchType.LAZY) {
            return javax.persistence.FetchType.LAZY;
        } else if (jaxbType == PersistenceUnitDefaultToOneFetchType.EAGER) {
            return javax.persistence.FetchType.EAGER;
        }
        // Not specified - return null so JPAPUnitInfo keeps its default (EAGER)
        return null;
    }

    @Override
    public Properties getProperties() {
        Properties rtnProperties = null;

        com.ibm.ws.jpa.pxml40.Persistence.PersistenceUnit.Properties puProperties = ivPUnit.getProperties();

        if (puProperties != null) {
            List<com.ibm.ws.jpa.pxml40.Persistence.PersistenceUnit.Properties.Property> propertyList = puProperties.getProperty();
            if (propertyList != null && !propertyList.isEmpty()) {
                rtnProperties = new Properties();
                for (com.ibm.ws.jpa.pxml40.Persistence.PersistenceUnit.Properties.Property puProperty : propertyList) {
                    try {
                        rtnProperties.setProperty(puProperty.getName(), puProperty.getValue());
                    } catch (Throwable ex) {
                        FFDCFilter.processException(ex, CLASS_NAME + ".getProperties", "188", this);
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

    @Override
    public String getName() {
        return ivPUnit.getName();
    }

    @Override
    public PersistenceUnitTransactionType getTransactionType() {
        PersistenceUnitTransactionType rtnType = null;
        com.ibm.ws.jpa.pxml40.PersistenceUnitTransactionType jaxbType = ivPUnit.getTransactionType();
        if (jaxbType == com.ibm.ws.jpa.pxml40.PersistenceUnitTransactionType.JTA) {
            rtnType = PersistenceUnitTransactionType.JTA;
        } else if (jaxbType == com.ibm.ws.jpa.pxml40.PersistenceUnitTransactionType.RESOURCE_LOCAL) {
            rtnType = PersistenceUnitTransactionType.RESOURCE_LOCAL;
        }
        return rtnType;
    }
}
