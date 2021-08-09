/*******************************************************************************
 * Copyright (c) 2006, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.container.osgi.jndi;

import java.io.Serializable;
import java.util.Map;
import java.util.Properties;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.jpa.JPAPuId;

/*
 * Naming Info object used for JPA @PersistenceUnit and @PersistenceContext
 * JNDI Reference binding.
 */
public class JPAJndiLookupInfo implements Serializable {
    private static final long serialVersionUID = 4608505288422385364L;

    // Persistence unit identity.
    private final JPAPuId ivPuId;

    // JavaEE unique identifier for the component providing this binding/reference.
    private final J2EEName ivJ2eeName; // d510184

    // PersistenceUnit or PersistenceContext reference name.
    private final String ivRefName; // d510184

    // set to true for @PersistenceUnit binding.
    private final boolean ivIsFactory;

    // set to true if extend-scoped persistence context type in declared @PersistenceContext.
    private final boolean ivIsExtendedContextType;

    // Properties defined in @PersistenceContext annotation and/or ejb-jar.xml or null.
    private final Properties ivProperties;

    // Set true if the Unsynchronized transaction synchronization type is declared in @PersistenceContext
    private final boolean ivIsUnsynchronized;

    // is @PersistenceUnit injected to a SFSB
    private final boolean ivIsSFSB; // d416151.3.1

    /**
     * Constructor uses for @PersistenceUnit binding in Client Container.
     * 
     * @param puId PersistenceUnit identifier.
     * @param isSFSB
     *            true if this PersistenceUnit binding is for a Stateful Session bean.
     */
    public JPAJndiLookupInfo(JPAPuId puId,
                             boolean isSFSB) // d416151.3.1
    {
        this(puId, null, null, true, false, isSFSB, null, false);
    }

    /**
     * Constructor uses for @PersistenceUnit binding.
     * 
     * @param puId PersistenceUnit identifier.
     * @param j2eeName
     *            JavaEE unique identifier for the component, identifying the
     *            java:comp/env context used.
     * @param refName
     *            Name of the PersistenceUnit reference.
     * @param isSFSB
     *            true if this PersistenceUnit binding is for a Stateful Session bean.
     */
    public JPAJndiLookupInfo(JPAPuId puId,
                             J2EEName j2eeName,
                             String refName,
                             boolean isSFSB) // d416151.3.1
    {
        this(puId, j2eeName, refName, true, false, isSFSB, null, false);
    }

    /**
     * Constructor uses for @PersistenceContext binding.
     * 
     * @param puId PersistenceUnit identifier.
     * @param j2eeName
     *            JavaEE unique identifier for the component, identifying the
     *            java:comp/env context used.
     * @param refName
     *            Name of the PersistenceContext reference.
     * @param isExtendedContextType
     *            Extend-scoped persistence context.
     * @param properties
     *            PersistenceContext properties declared in annotation and/or xml.
     */
    public JPAJndiLookupInfo(JPAPuId puId,
                             J2EEName j2eeName,
                             String refName,
                             boolean isExtendedContextType,
                             boolean isSFSB,
                             Properties properties,
                             boolean isUnsynchronized) {
        this(puId, j2eeName, refName, false, isExtendedContextType, isSFSB, properties, isUnsynchronized);
    }

    /**
     * Common lookup info object construction for PersistenceUnit and PersistenceContext.
     */
    private JPAJndiLookupInfo(JPAPuId puId,
                              J2EEName j2eeName,
                              String refName,
                              boolean isFactory,
                              boolean isExtendedContextType,
                              boolean isSFSB, // d658638
                              Properties properties,
                              boolean isUnsynchronized) {
        ivPuId = puId;
        ivJ2eeName = j2eeName; // d510184
        ivRefName = refName; // d510184
        ivIsFactory = isFactory;
        ivIsExtendedContextType = isExtendedContextType;
        ivIsSFSB = isSFSB;
        ivProperties = properties;
        ivIsUnsynchronized = isUnsynchronized;
    }

    /**
     * Getter for PuId.
     */
    public JPAPuId getPuId() {
        return ivPuId;
    }

    /**
     * Getter for J2EEName.
     **/
    // d510184
    public J2EEName getJ2EEName() {
        return ivJ2eeName;
    }

    /**
     * Getter for reference name.
     **/
    // d510184
    public String getReferenceName() {
        return ivRefName;
    }

    /**
     * Returns true if this represents EntityManagerFactory.
     */
    public boolean isFactory() {
        return ivIsFactory;
    }

    /**
     * Returns true if this represents a extend-scoped EntityManager.
     */
    public boolean isExtendedContextType() {
        return ivIsExtendedContextType;
    }

    /**
     * Returns a Map of all the properties declared in PersistenceContext or null if none is found.
     */
    public Map<?, ?> getPersistenceProperties() {
        return ivProperties;
    }

    // d416151.3.1 Begins
    /**
     * Returns true if this info is from injecting a EntityManagerFactory to a SFSB.
     */
    public boolean isSFSB() {
        return ivIsSFSB;
    }

    public boolean isUnsynchronized() {
        return ivIsUnsynchronized;
    }

    // d416151.3.1 Ends

    /**
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return ivPuId + ", " + ivJ2eeName + "#" + ivRefName
               + ", isFactory=" + ivIsFactory
               + ", isSFSB=" + ivIsSFSB // d416151.3.1
               + ", "
               + (ivIsFactory ? "" : "PersistenceContextType="
                                     + (ivIsExtendedContextType ? "Extended" : "Transaction"))
               + ", "
               + (ivIsFactory ? "" : "TxSynchronization="
                                     + (ivIsUnsynchronized ? "Unsynchronized" : "Synchronized"))
               + ", properties=" + (ivProperties == null ? "[]" : ivProperties);
    }
}
