/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package com.ibm.ws.jpa.management;

import static com.ibm.ws.jpa.management.JPAConstants.JPA_RESOURCE_BUNDLE_NAME;
import static com.ibm.ws.jpa.management.JPAConstants.JPA_TRACE_GROUP;

import java.util.ArrayList;
import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jpa.pxml40.Persistence;
import com.ibm.ws.jpa.pxml40.Persistence.PersistenceUnit;

/**
 * Provides a concrete implementation of the JaxbPersistence abstraction, representing the
 * {@code <persistence>} stanza in a JPA 4.0 version persistence.xml.
 * <p>
 * This implementation wraps the JAXB generated class that represent a {@code <persistence>}
 * stanza in a 4.0 version persistence.xml.
 * <p>
 * Get methods on the generated JAXB class which return other JAXB generated classes will instead
 * return either a java primitive or javax.persistence representation of that data or another
 * abstraction interface; allowing the client of this class to be coded independent of the JAXB
 * implementation.
 * <p>
 **/
public class JaxbPersistence40 extends JaxbPersistence {
    protected static final String SCHEMA_VERSION = "4.0";
    private static final String XSD_NAME = "persistence_4_0.xsd";

    private static final TraceComponent tc = Tr.register(JaxbPersistence40.class, JPA_TRACE_GROUP, JPA_RESOURCE_BUNDLE_NAME);

    /** The {@code <persistence-unit>}s found in this persistence.xml **/
    private List<JaxbPUnit> ivPUnits;

    public JaxbPersistence40(JPAPXml pxml) {
        super(pxml, "com.ibm.ws.jpa.pxml40", XSD_NAME);
    }

    @Override
    void setResult(Object result) {
        Persistence p = (Persistence) result;

        // Obtain the list of <persistence-unit>s and provide an abstraction for them as well.
        List<PersistenceUnit> pUnits = p.getPersistenceUnit();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc,
                     "JaxbPersistence40.setResult : persistence units : " + ((pUnits == null) ? "null" : pUnits.size()));

        if (pUnits != null) {
            ivPUnits = new ArrayList<JaxbPUnit>(pUnits.size());
            for (PersistenceUnit pUnit : pUnits) {
                ivPUnits.add(new JaxbPUnit40(pUnit));
            }
        } else {
            // Ensure an empty list is returned to avoid NPE
            ivPUnits = new ArrayList<JaxbPUnit>();
        }
    }

    @Override
    List<JaxbPUnit> getPersistenceUnit() {
        return ivPUnits;
    }

    @Override
    String getVersion() {
        return SCHEMA_VERSION;
    }

}
