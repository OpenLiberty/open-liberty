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
package com.ibm.wsspi.persistence.internal.eclipselink;

import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceUnitInfo;

import org.eclipse.persistence.jpa.PersistenceProvider;

/**
 * An extension of the EclipseLink PersistenceProvider.
 */
public class PsPersistenceProvider extends PersistenceProvider {

     /**
      * This method exists to give us access to the protected method
      * createEntityManagerFactoryImpl(...)
      */
     public EntityManagerFactory createContainerEMF(PersistenceUnitInfo info, Map<?, ?> properties,
          boolean requiresConnection) {
          return super.createContainerEntityManagerFactoryImpl(info, properties, requiresConnection);
     }
}
