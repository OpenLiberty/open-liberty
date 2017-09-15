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

import static com.ibm.ws.jpa.management.JPAConstants.JPA_OVERRIDE_EXCLUDE_UNLISTED_CLASSES;
import static com.ibm.ws.jpa.management.JPAConstants.JPA_RESOURCE_BUNDLE_NAME;
import static com.ibm.ws.jpa.management.JPAConstants.JPA_TRACE_GROUP;

import java.util.List;
import java.util.Properties;

import javax.persistence.spi.PersistenceUnitTransactionType;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.jpa.pxml10.Persistence.PersistenceUnit;

/**
 * Provides a concrete implementation of the JaxbPUnit abstraction,
 * representing the <persistence-unit> stanza in a 1.0 version
 * persistence.xml. <p>
 * 
 * This implementation wraps the JAXB generated class that represent a
 * <persistence-unit> stanza in a 1.0 version persistence.xml. <p>
 * 
 * Get methods on the generated JAXB class which return other JAXB generated
 * classes will instead return either a java primitive or javax.persistence
 * or standard jdk representation of that data; allowing the client of this
 * class to be coded independent of the JAXB implementation. <p>
 **/
class JaxbPUnit10 extends JaxbPUnit
{
    private static final String CLASS_NAME = JaxbPUnit10.class.getName();

    private static final TraceComponent tc = Tr.register
                    (JaxbPUnit10.class,
                     JPA_TRACE_GROUP,
                     JPA_RESOURCE_BUNDLE_NAME);

    private PersistenceUnit ivPUnit;

    /**
     * Constructs a JaxbPUnit10 instance with the JAXB PersistenceUnit instance
     * for which the created instance will provide an abstraction.
     **/
    JaxbPUnit10(PersistenceUnit pUnit)
    {
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
    public String getDescription()
    {
        return ivPUnit.getDescription();
    }

    /**
     * Gets the value of the provider property.
     * 
     * @return value of the provider property.
     */
    public String getProvider()
    {
        return ivPUnit.getProvider();
    }

    /**
     * Gets the value of the jtaDataSource property.
     * 
     * @return value of the jtaDataSource property.
     */
    public String getJtaDataSource()
    {
        return ivPUnit.getJtaDataSource();
    }

    /**
     * Gets the value of the nonJtaDataSource property.
     * 
     * @return value of the nonJtaDataSource property.
     */
    public String getNonJtaDataSource()
    {
        return ivPUnit.getNonJtaDataSource();
    }

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
    public List<String> getMappingFile()
    {
        return ivPUnit.getMappingFile();
    }

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
    public List<String> getJarFile()
    {
        return ivPUnit.getJarFile();
    }

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
    public List<String> getClazz()
    {
        return ivPUnit.getClazz();
    }

    /**
     * Gets the value of the excludeUnlistedClasses property. <p>
     * 
     * Note that the default value defined in the 1.0 persistence xsd is 'false',
     * however, that is for when the stanza is specified, but without a value.
     * If the stanza is not present in persistence.xml, then the default is
     * also 'false'. <p>
     * 
     * @return value of the excludeUnlistedClasses property.
     */
    public boolean isExcludeUnlistedClasses()
    {
        // The JAXB generated classes will return 'false' if the stanza was
        // specified, but without a value
        Boolean exclude = ivPUnit.isExcludeUnlistedClasses();

        // The version 1.0 persistence.xsd spelled out the default for
        // exclude-unlisted-classes is false.
        //
        // This means there are 4 choices:
        //   <exclude-unlisted-classes> not specified                   --> Not excluded
        //   <exclude-unlisted-classes>false</exclude-unlisted-classes> --> Not excluded
        //   <exclude-unlisted-classes>true</exclude-unlisted-classes>  --> Excluded
        //   <exclude-unlisted-classes/>                                --> Not excluded
        //
        // The last choice contradicts the sample's implied semantics.
        // Patrick Linskey confirms that this is a problem in the JPA spec and
        // the spec decided that the XSD is the final correct specification.
        // i.e. unlisted classes are excluded iff
        // <exclude-unlisted-classes>true</exclude-unlisted-classes> is used.

        if (JPA_OVERRIDE_EXCLUDE_UNLISTED_CLASSES)
        {
            // If "com.ibm.jpa.override.exclude.unlisted.classes" system property is set to
            // true, all variations of <exclude-unlisted-classes> specification in
            // persistence unit are treated as
            // <exclude-unlisted-classes>true</exclude-listed-classes>, which matches the
            // original intent as used in the JPA spec samples.
            //
            // This is NOT the default behavior.
            if (exclude != null)
            {
                exclude = Boolean.TRUE;

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "isExcludeUnlistedClasses : overriden to TRUE");
            }
        }

        // The default when not present at all is 'false'
        if (exclude == null)
        {
            exclude = Boolean.FALSE;

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "isExcludeUnlistedClasses : defaulted to FALSE");
        }

        return exclude.booleanValue();
    }

    /**
     * Gets the value of the properties property.
     * 
     * @return value of the properties property.
     */
    public Properties getProperties()
    {
        Properties rtnProperties = null;

        // Convert this Properties from the class defined in JAXB
        // (com.ibm.ws.jpa.pxml10.Persistence.PersistenceUnit.Properties)
        // to standard JDK classes (java.util.Properties).

        com.ibm.ws.jpa.pxml10.Persistence.PersistenceUnit.Properties puProperties = ivPUnit.getProperties();

        if (puProperties != null)
        {
            List<com.ibm.ws.jpa.pxml10.Persistence.PersistenceUnit.Properties.Property> propertyList = puProperties.getProperty();
            if (propertyList != null && !propertyList.isEmpty())
            {
                rtnProperties = new Properties();
                for (com.ibm.ws.jpa.pxml10.Persistence.PersistenceUnit.Properties.Property puProperty : propertyList)
                {
                    // It is possible that a syntax error will exist in the persistence.xml
                    // where the property or value is null.  Neither is acceptable for
                    // a Hashtable and will result in an exception.
                    try
                    {
                        rtnProperties.setProperty(puProperty.getName(), puProperty.getValue());
                    } catch (Throwable ex)
                    {
                        FFDCFilter.processException(ex, CLASS_NAME + ".getProperties",
                                                    "219", this);
                        Tr.error(tc, "PROPERTY_SYNTAX_ERROR_IN_PERSISTENCE_XML_CWWJP0039E",
                                 ivPUnit.getName(),
                                 puProperty.getName(),
                                 puProperty.getValue(),
                                 ex);
                        String exMsg = "A severe error occurred while processing the properties " +
                                       "within the persistence.xml of Persistence Unit: " +
                                       ivPUnit.getName() + " (Property = " + puProperty.getName() +
                                       ", Value = " + puProperty.getValue() + ").";
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
    public String getName()
    {
        return ivPUnit.getName();
    }

    /**
     * Gets the value of the transactionType property.
     * 
     * @return value of the transactionType property.
     */
    public PersistenceUnitTransactionType getTransactionType()
    {
        // Convert this TransactionType from the class defined in JAXB
        // (com.ibm.ws.jpa.pxml10.PersistenceUnitTransactionType) to JPA
        // (javax.persistence.spi.PersistenceUnitTransactionType).

        PersistenceUnitTransactionType rtnType = null;
        com.ibm.ws.jpa.pxml10.PersistenceUnitTransactionType jaxbType = null;

        jaxbType = ivPUnit.getTransactionType();
        if (jaxbType == com.ibm.ws.jpa.pxml10.PersistenceUnitTransactionType.JTA)
        {
            rtnType = PersistenceUnitTransactionType.JTA;
        }
        else if (jaxbType == com.ibm.ws.jpa.pxml10.PersistenceUnitTransactionType.RESOURCE_LOCAL)
        {
            rtnType = PersistenceUnitTransactionType.RESOURCE_LOCAL;
        }
        return rtnType;
    }

}
