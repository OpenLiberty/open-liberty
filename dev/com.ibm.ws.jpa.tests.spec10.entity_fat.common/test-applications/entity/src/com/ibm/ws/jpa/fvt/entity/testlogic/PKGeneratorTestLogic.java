/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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

package com.ibm.ws.jpa.fvt.entity.testlogic;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.junit.Assert;

import com.ibm.ws.jpa.fvt.entity.entities.IPKGeneratorEntity;
import com.ibm.ws.jpa.fvt.entity.testlogic.enums.PKGeneratorEntityEnum;
import com.ibm.ws.testtooling.jpaprovider.JPAPersistenceProvider;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class PKGeneratorTestLogic extends AbstractTestLogic {
    /**
     * 60 Points
     */
    public void testPKGenerator001(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                   Object managedComponentObject) throws NamingException, SQLException {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("PKGeneratorTestLogic.testPKGenerator001(): Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // These triggers for OpenJPA interfere with EclipseLink, which runs a separate query to get the sequence number, so remove them
        if (jpaResource.getEm().getProperties().containsKey("eclipselink.target-server")) {
            DataSource ds = (DataSource) new InitialContext().lookup("jdbc/JPA_NJTA_DS");
            Connection con = ds.getConnection();
            try {
                // The triggers are created only for Oracle, so we can skip for other database types
                if (con.getMetaData().getDatabaseProductName().toUpperCase().indexOf("ORACLE") >= 0) {
                    Statement stmt = con.createStatement();
                    stmt.executeUpdate("DROP TRIGGER PKGenIdentityEntity_id_TRG");
                    stmt.executeUpdate("DROP TRIGGER XMLPKGenIdentityEntity_id_TRG");
                    stmt.close();
                }
            } catch (SQLException x) {
                // expected if trigger doesn't exist
            } finally {
                con.close();
            }
        }

        // Fetch target entity type from test parameters
        String entityName = (String) testExecCtx.getProperties().get("EntityName");
        PKGeneratorEntityEnum targetEntityType = PKGeneratorEntityEnum.resolveEntityByName(entityName);
        if (targetEntityType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity type specified ('" + entityName + "').  Cannot execute the test.");
            return;
        }

        JPAPersistenceProvider provider = JPAPersistenceProvider.resolveJPAPersistenceProvider(jpaResource);
        if (JPAPersistenceProvider.HIBERNATE.equals(provider)) {
            // TODO: Hibernate fails with "SEQUENCE 'APP.HIBERNATE_SEQUENCE' does not exist" exception, even though it does exist.
            return;
        }

        // Execute Test Case
        try {
            System.out.println("PKGeneratorTestLogic.testPKGenerator001(): Begin");

            int ARR_LEN = 20;
            IPKGeneratorEntity[] entityArray = new IPKGeneratorEntity[ARR_LEN];

            System.out.println("About to create " + ARR_LEN + " unmanaged instances of IPKGeneratorEntity...");
            System.out.println("Primary key values for the new entities will not be known until transaction commit or persistence context flush.");

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            for (int index = 0; index < ARR_LEN; index++) {
                System.out.println("Creating new object instance of " + targetEntityType.getEntityName() + "...");
                entityArray[index] = (IPKGeneratorEntity) constructNewEntityObject(targetEntityType);
                entityArray[index].setIntVal((int) (Math.random() * 100000)); // Random number from 0 to 100,000
            }

            System.out.println("Generating random order which the entities will be persisted/flushed...");
            ArrayList<Integer> indexPoolList = new ArrayList<Integer>();
            for (int index = 0; index < ARR_LEN; index++) {
                indexPoolList.add(new Integer(index));
            }

            System.out.println("Persisting/flushing entities in the order that was generated...");
            for (int index = 0; index < ARR_LEN; index++) {
                int randomListIndex = (int) (indexPoolList.size() * Math.random());

                if (randomListIndex >= indexPoolList.size()) {
                    // Protect against Index Out of Bounds problems
                    randomListIndex = indexPoolList.size() - 1;
                }

                Integer randomIndex = indexPoolList.remove(randomListIndex);
                System.out.println("Persisting IPKGeneratorEntity with intVal=" + entityArray[randomIndex].getIntVal());
                jpaResource.getEm().persist(entityArray[randomIndex]);
                jpaResource.getEm().flush();

                System.out.println("Persisted and flushed IPKGeneratorEntity with id=" + entityArray[randomIndex].getId() +
                                   " and intVal = " + entityArray[randomIndex].getIntVal());
            }

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("Verifying that the data was saved to the database correctly.");

            IPKGeneratorEntity[] entityArray_find = new IPKGeneratorEntity[ARR_LEN];

            // ARR_LEN * 3 Points
            for (int index = 0; index < ARR_LEN; index++) {
                int targetId = entityArray[index].getId();
                System.out.println("Searching for IPKGeneratorEntity with id=" + targetId);

                entityArray_find[index] = (IPKGeneratorEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), targetId);

                // Perform validations on the entity returned by find.
                Assert.assertNotNull("Assert the find operation for IPKGeneratorEntity with id=" + targetId + " did not return a null value.", entityArray_find[index]);
                Assert.assertNotSame("Assert the entity returned by the find operation for IPKGeneratorEntity with id=" + targetId +
                                     "is not the same entity object as the original.", entityArray[index], entityArray_find[index]);
                Assert.assertEquals("Assert the value of intVal associated with the entity returned by the find operation for " +
                                    "IPKGeneratorEntity with id=" + targetId + " is not the same as the original.", entityArray[index].getIntVal(),
                                    entityArray_find[index].getIntVal());
            }

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            for (int index = 0; index < ARR_LEN; index++) {
                int targetId = entityArray[index].getId();
                System.out.println("Searching for IPKGeneratorEntity with id=" + targetId);

                System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + targetId + ")...");
                IPKGeneratorEntity find_remove_entity = (IPKGeneratorEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), targetId);
                System.out.println("Object returned by find: " + find_remove_entity);

                Assert.assertNotNull("Assert that the find operation did not return null", find_remove_entity);

                System.out.println("Removing entity...");
                jpaResource.getEm().remove(find_remove_entity);
            }

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("Ending test.");
        } catch (AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        } finally {
            System.out.println("PKGeneratorTestLogic.testPKGenerator001(): End");
        }
    }
}
