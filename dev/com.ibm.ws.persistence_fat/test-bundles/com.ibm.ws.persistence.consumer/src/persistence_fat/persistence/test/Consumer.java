/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package persistence_fat.persistence.test;

import java.io.Serializable;
import java.io.Writer;

/**
 * An interface that is shared with an application to interact with this service.
 */
public interface Consumer {
    public enum DATABASE {
        DB2, INFORMIX, ORACLE, ORACLE12, SQLSERVER, SYBASE, DERBY, JAVADB
    };

    public String test();

    void createTables();

    public long getRandomPersonId();

    public boolean personExists(long personId);

    /**
     * Persists a new person, and returns the id;
     * 
     * @param newTran
     *            -- If true, any existing tran will be suspended and a new one will be created
     *            for the persist operation. Otherwise the existing tran will be used
     * @return
     */
    public long persistPerson(boolean newTran, int numCars);

    public long persistPerson(boolean newTran, int numCars, String data);

    public long persistPerson(boolean newTran, int numCars, String data, Serializable serializable);

    /**
     * Return {firstName, lastName}
     */
    public String[] getPersonName(long personId);

    /**
     * @param personId
     * @throws RuntimeException
     *             if Person.cars is eagerly loaded.
     * @return
     */
    public int getNumCars(long personId);

    public Integer queryPersonDataParameter(String queryStr);

    public Integer queryPersonDataLiteral(String queryStr);

    public void updateUnicodeConfig(Boolean allowUnicode);

    public void clearTempConfig();

    public Serializable getPersonSerializableData(long personId);

    /**
     * Generate the DDL statements to the given writer.
     * 
     * @param out
     *            a Writer where DDL will be written to.
     */
    public void generateDDL(Writer out, DATABASE db);

    public String getDatabaseTerminationToken();
}
