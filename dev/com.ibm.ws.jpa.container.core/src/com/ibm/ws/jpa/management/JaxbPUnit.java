/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
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

/**
 * This interface is an abstraction of <persistence-unit> in persistence.xml. <p>
 * 
 * Get methods are provided for all attributes of <persistence-unit> for all
 * schema versions of persistence.xml (i.e. 1.0 and 2.0, etc.). This allows
 * the client of this interface to be coded (and compiled) independent of the
 * schema version. <p>
 * 
 * A different implementation of this interface will be provided for each schema
 * version of persistence.xml and will wrap the JAXB generated class that
 * represents a <persistence-unit>; generally Persistence.PersistenceUnit. <p>
 * 
 * Get methods on the generated JAXB class which return other JAXB generated
 * classes will instead return either a java primitive or javax.persistence
 * representation of that data; allowing the client of this interface to
 * be coded independent of the JAXB implementation. <p>
 **/
abstract class JaxbPUnit
{
    private static final TraceComponent tc = Tr.register(JaxbPUnit.class,
                                                         JPA_TRACE_GROUP,
                                                         JPA_RESOURCE_BUNDLE_NAME);

    /**
     * Gets the value of the description property.
     * 
     * @return value of the description property.
     */
    abstract String getDescription();

    /**
     * Gets the value of the provider property.
     * 
     * @return value of the provider property.
     */
    abstract String getProvider();

    /**
     * Gets the value of the jtaDataSource property.
     * 
     * @return value of the jtaDataSource property.
     */
    abstract String getJtaDataSource();

    /**
     * Gets the value of the nonJtaDataSource property.
     * 
     * @return value of the nonJtaDataSource property.
     */
    abstract String getNonJtaDataSource();

    /**
     * Gets the value of the mappingFile property. <p>
     * 
     * This accessor method returns a reference to the live list, not a
     * snapshot. Therefore any modification you make to the returned list will
     * be present inside the JAXB object. This is why there is not a
     * <CODE>set</CODE> method for the mappingFile property. <p>
     * 
     * For example, to add a new item, do as follows:
     * 
     * <pre>
     * getMappingFile().add(newItem);
     * </pre>
     * <p>
     * 
     * Objects of the following type(s) are allowed in the list {@link String }
     */
    abstract List<String> getMappingFile();

    /**
     * Gets the value of the jarFile property. <p>
     * 
     * This accessor method returns a reference to the live list, not a
     * snapshot. Therefore any modification you make to the returned list will
     * be present inside the JAXB object. This is why there is not a
     * <CODE>set</CODE> method for the jarFile property. <p>
     * 
     * For example, to add a new item, do as follows:
     * 
     * <pre>
     * getJarFile().add(newItem);
     * </pre>
     * 
     * Objects of the following type(s) are allowed in the list {@link String }
     */
    abstract List<String> getJarFile();

    /**
     * Gets the value of the clazz property. <p>
     * 
     * This accessor method returns a reference to the live list, not a
     * snapshot. Therefore any modification you make to the returned list will
     * be present inside the JAXB object. This is why there is not a
     * <CODE>set</CODE> method for the clazz property. <p>
     * 
     * For example, to add a new item, do as follows:
     * 
     * <pre>
     * getClazz().add(newItem);
     * </pre>
     * 
     * Objects of the following type(s) are allowed in the list {@link String }
     */
    abstract List<String> getClazz();

    /**
     * Gets the value of the excludeUnlistedClasses property.
     * 
     * @return value of the excludeUnlistedClasses property.
     */
    abstract boolean isExcludeUnlistedClasses();

    /**
     * Gets the value of the sharedCacheMode property.
     * 
     * @return value of the sharedCacheMode property.
     */
    SharedCacheMode getSharedCacheMode()
    {
        // Not available in 1.0 persistence schema
        // Per the spec, must return UNSPECIFIED if not in xml
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getSharedCacheMode : defaulted to UNSPECIFIED");

        return SharedCacheMode.UNSPECIFIED;
    }

    /**
     * Gets the value of the validationMode property.
     * 
     * @return value of the validationMode property.
     */
    ValidationMode getValidationMode()
    {
        // Not available in 1.0 persistence schema
        // Per the spec, must return null when not in xml
        return null;
    }

    /**
     * Gets the value of the properties property.
     * 
     * @return value of the properties property.
     */
    abstract Properties getProperties();

    /**
     * Gets the value of the name property.
     * 
     * @return value of the name property.
     */
    abstract String getName();

    /**
     * Gets the value of the transactionType property.
     * 
     * @return value of the transactionType property.
     */
    abstract PersistenceUnitTransactionType getTransactionType();

    @Override
    public String toString()
    {
        return (getClass().getSimpleName() + "(" + getName() + ")@" + Integer.toHexString(hashCode()));
    }
}
