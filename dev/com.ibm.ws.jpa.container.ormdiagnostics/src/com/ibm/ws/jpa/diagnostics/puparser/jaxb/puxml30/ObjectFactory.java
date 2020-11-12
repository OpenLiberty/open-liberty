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

package com.ibm.ws.jpa.diagnostics.puparser.jaxb.puxml30;

import javax.xml.bind.annotation.XmlRegistry;

@XmlRegistry
public class ObjectFactory {

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.ibm.ws.jpa.diagnostics.puparser.jaxb.puxml30
     *
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link Persistence }
     *
     */
    public Persistence createPersistence() {
        return new Persistence();
    }

    /**
     * Create an instance of {@link Persistence.PersistenceUnit }
     *
     */
    public Persistence.PersistenceUnit createPersistencePersistenceUnit() {
        return new Persistence.PersistenceUnit();
    }

    /**
     * Create an instance of {@link Persistence.PersistenceUnit.Properties }
     *
     */
    public Persistence.PersistenceUnit.Properties createPersistencePersistenceUnitProperties() {
        return new Persistence.PersistenceUnit.Properties();
    }

    /**
     * Create an instance of {@link Persistence.PersistenceUnit.Properties.Property }
     *
     */
    public Persistence.PersistenceUnit.Properties.Property createPersistencePersistenceUnitPropertiesProperty() {
        return new Persistence.PersistenceUnit.Properties.Property();
    }

}
