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

import java.util.ArrayList;
import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jpa.pxml30.Persistence;
import com.ibm.ws.jpa.pxml30.Persistence.PersistenceUnit;

/**
 * Provides a concrete implementation of the JaxbPersistence abstraction, representing the
 * <persistence> stanza in a 3.0 version persistence.xml.
 * <p>
 *
 * This implementation wraps the JAXB generated class that represent a <persistence> stanza in a 3.0
 * version persistence.xml.
 * <p>
 *
 * Get methods on the generated JAXB class which return other JAXB generated classes will instead
 * return either a java primitive or javax.persistence representation of that data or another
 * abstraction interface; allowing the client of this class to be coded independent of the JAXB
 * implementation.
 * <p>
 **/
public class JaxbPersistence30 extends JaxbPersistence {
    protected static final String SCHEMA_VERSION = "3.0";
    private static final String XSD_NAME = "persistence_3_0.xsd";

    private static final TraceComponent tc = Tr.register(JaxbPersistence30.class, JPA_TRACE_GROUP, JPA_RESOURCE_BUNDLE_NAME);

    /** The <persistence-unit>s found in this persistence.xml **/
    private List<JaxbPUnit> ivPUnits;

    public JaxbPersistence30(JPAPXml pxml) {
        super(pxml, "com.ibm.ws.jpa.pxml30", XSD_NAME);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.jpa.management.JaxbPersistence#setResult(java.lang.Object)
     */
    @Override
    void setResult(Object result) {
        Persistence p = (Persistence) result;

        // Obtain the list of <persistence-unit>s and provide an abstraction for them as well.
        List<PersistenceUnit> pUnits = p.getPersistenceUnit();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc,
                     "JaxbPersistence30.setResult : persistence units : " + ((pUnits == null) ? "null" : pUnits.size()));

        if (pUnits != null) {
            ivPUnits = new ArrayList<JaxbPUnit>(pUnits.size());
            for (PersistenceUnit pUnit : pUnits) {
                ivPUnits.add(new JaxbPUnit30(pUnit));
            }
        } else {
            // Insure an empty list is returned to avoid NPE
            ivPUnits = new ArrayList<JaxbPUnit>();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.jpa.management.JaxbPersistence#getPersistenceUnit()
     */
    @Override
    List<JaxbPUnit> getPersistenceUnit() {
        return ivPUnits;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.jpa.management.JaxbPersistence#getVersion()
     */
    @Override
    String getVersion() {
        return SCHEMA_VERSION;
    }

}
