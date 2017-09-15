/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.persistence;

import java.io.Writer;

import javax.persistence.EntityManager;

/**
 * Interface used to :
 * <ul>
 * <li>create EntityManagers based off PersistenceServiceUnitConfig.</li>
 * <li>drive schema / table operations.</li>
 * 
 * </P> <b>Note:</b> When a user has finished using the unit, .close() must be invoked. Once a
 * PersistenceServiceUnit has been closed, all of its EntityManagers are considered to be closed.
 */
public interface PersistenceServiceUnit {
     /**
      * Returns a non-thread safe EntityManager. It is expected that the provided EntityManager is
      * short lived. An EntityManager should be created on a per request basis. When work is
      * complete, the EntityManager needs to be closed.
      * 
      * @return An EntityManager for this unit.
      */
     public EntityManager createEntityManager();

     /**
      * DataSource precedence : privileged, nonJta, jta
      * <p>
      * Only valid to be invoked via a client script. Cannot be invoked multiple times. TODO --
      * 154030
      */
     public void createTables();

     /**
      * DataSource precedence : privileged, nonJta, jta
      * <p>
      * Only valid to be invoked via a client script. Cannot be invoked multiple times. TODO --
      * 154030
      */
     public void dropAndCreateTables();

     /**
      * DataSource precedence : privileged, nonJta, jta
      * <p>
      * Only valid to be invoked via a client script. Cannot be invoked multiple times. TODO --
      * 154030
      */
     public void dropTables();

     /**
      * Generates DDL based off the PersistenceServiceUnitConfig that was used to create this unit
      * and writes it to out.
      * <p>
      * Only valid to be invoked via a client script. Cannot be invoked multiple times. TODO --
      * 154030
      * 
      * @param out
      *             a Writer where DDL will be written to.
      */
     public void generateDDL(Writer out);

     /**
      * Close the PersistenceServiceUnit. This method must be invoked to cleanup resources when the
      * consumer of this service is shutting down.
      */
     public void close();
     
     /**
      * Returns the termination token of SQL statements based on the Database platform.
      */
     public String getDatabaseTerminationToken();
}
