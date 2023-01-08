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

package com.ibm.ws.jpa.fvt.jarfile.testlogic;

import java.util.List;

import javax.persistence.Query;

import org.junit.Assert;

import com.ibm.ws.jpa.commonentities.jpa10.employee.Employee;
import com.ibm.ws.jpa.commonentities.jpa10.simple.ISimpleEntity10;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.testlogic.JPAEntityClassEnum;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class JarFileSupportTestLogic extends AbstractTestLogic {
    public static enum EntityClasses implements JPAEntityClassEnum {
        // Component residing entity types
        SimpleEntity10 {
            @Override
            public String getEntityClassName() {
                return "com.ibm.ws.jpa.commonentities.jpa10.simple.SimpleEntity10";
            }

            @Override
            public String getEntityName() {
                return "SimpleEntity10";
            }
        },
        XMLSimpleEntity10 {
            @Override
            public String getEntityClassName() {
                return "com.ibm.ws.jpa.commonentities.jpa10.simple.XMLSimpleEntity10";
            }

            @Override
            public String getEntityName() {
                return "XMLSimpleEntity10";
            }
        },

        // Utility jar residing entity types
        Employee {
            @Override
            public String getEntityClassName() {
                return "com.ibm.ws.jpa.commonentities.jpa10.employee.Employee";
            }

            @Override
            public String getEntityName() {
                return "Employee";
            }
        },
        Manager {
            @Override
            public String getEntityClassName() {
                return "com.ibm.ws.jpa.commonentities.jpa10.employee.Manager";
            }

            @Override
            public String getEntityName() {
                return "Manager";
            }
        },
        Department {
            @Override
            public String getEntityClassName() {
                return "com.ibm.ws.jpa.commonentities.jpa10.employee.Department";
            }

            @Override
            public String getEntityName() {
                return "Department";
            }
        };

        @Override
        public abstract String getEntityClassName();

        @Override
        public abstract String getEntityName();
    };

    /**
     * Test Logic: testEntitiesInComponentArchive001
     *
     * Performs basic CRUD operations with an entity within the managed component archive:
     * 1) Create a new instance of the entity class
     * 2) Persist the new entity to the database
     * 3) Verify the entity was saved to the database
     * 4) Update the entity
     * 5) Verify the entity update was saved to the database
     * 6) Delete the entity from the database
     * 7) Verify the entity remove was successful
     *
     * Expected Test Points on Pass: 12
     */
    public void testEntitiesInComponentArchive001(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                  Object managedComponentObject) throws Throwable {
        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testEntitiesInComponentArchive001: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Identify the target entity type verified by this test case.
        EntityClasses entityType = null;
        String entityTypeStr = (String) testExecCtx.getProperties().get("EntityType");
        if ("SimpleEntity10".equalsIgnoreCase(entityTypeStr)) {
            entityType = EntityClasses.SimpleEntity10;
        } else if ("XMLSimpleEntity10".equalsIgnoreCase(entityTypeStr)) {
            entityType = EntityClasses.XMLSimpleEntity10;
        } else {
            Assert.fail("Invalid entity type specified ('" + entityTypeStr + "').  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("JarFileSupportTestLogic.testEntitiesInComponentArchive001(): Begin");

            // Cleanup the database for executing the test
            System.out.println("Cleaning up database before executing test...");
//            cleanupDatabase(jpaResource.getEm(), jpaResource.getTj(),
//                            new EntityClasses[] { EntityClasses.SimpleEntity10, EntityClasses.XMLSimpleEntity10 }, log);
            System.out.println("Database cleanup complete.\n");

            // log.addInfo("Testing basic CRUD operations...");

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            // 1) Create a new instance of the entity class
            System.out.println("1) Create a new instance of " + entityType.getEntityClassName());

            ISimpleEntity10 newEntity = (ISimpleEntity10) constructNewEntityObject(entityType);
            System.out.println("Constructed new instance of " + newEntity.getClass());

            System.out.println("Set id=1, strData=\"Some String Data\"..");
            newEntity.setId(1);
            newEntity.setStrData("Some String Data");
            newEntity.setByteArrData(new byte[] { 0, 1, 2, 3, 4, 5 }); // Oracle fails if this is null.

            // 2) Persist the new entity to the database
            System.out.println("2) Persist new entity to the database");

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Persisting " + newEntity);
            jpaResource.getEm().persist(newEntity);

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // 3) Verify the entity was saved to the database
            System.out.println("3) Verify the entity was saved to the database");

            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            // Begin a new transaction, to ensure the entity returned by find is managed
            // by the persistence context in all environments, including CM-TS.
            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            // Perform the find operation
            System.out.println("Finding " + entityType.getEntityName() + "(id=1)...");
            ISimpleEntity10 findEntity = (ISimpleEntity10) jpaResource.getEm().find(resolveEntityClass(entityType), 1);
            System.out.println("Object returned by find: " + findEntity);

            // Verify that em.find() returned an object.
            Assert.assertNotNull("Assert that the find operation did not return null", findEntity);

            // Perform basic verifications
            Assert.assertTrue(
                              "Assert find did not return the original object",
                              newEntity != findEntity);
            Assert.assertTrue(
                              "Assert entity returned by find is managed by the persistence context.",
                              jpaResource.getEm().contains(findEntity));
            Assert.assertEquals(
                                "Assert that the entity's id is 1",
                                findEntity.getId(),
                                1);
            Assert.assertEquals(
                                "Assert that the entity's strData field is \"Some String Data\"",
                                findEntity.getStrData(),
                                "Some String Data");

            // 4) Update the entity
            System.out.println("4) Update the entity");
            System.out.println("Updating " + entityType.getEntityName() + "(id=1)'s strData property to \"New Data\"...");
            findEntity.setStrData("New Data");

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // 5) Verify the entity update was saved to the database
            System.out.println("5) Verify the entity update was saved to the database");

            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            // results.addInfo("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            // Perform the find operation.
            System.out.println("Finding " + entityType.getEntityName() + "(id=1)...");
            ISimpleEntity10 findUpdatedEntity = (ISimpleEntity10) jpaResource.getEm().find(resolveEntityClass(entityType), 1);
            System.out.println("Object returned by find: " + findUpdatedEntity);

            // Verify that em.find() returned an object.
            Assert.assertNotNull("Assert that the find operation did not return null", findUpdatedEntity);

            // Perform basic verifications
            Assert.assertTrue(
                              "Assert find did not return the original object",
                              newEntity != findUpdatedEntity);
            Assert.assertTrue(
                              "Assert find did not return the first object returned by find",
                              findEntity != findUpdatedEntity);
            Assert.assertTrue(
                              "Assert entity returned by find is managed by the persistence context.",
                              jpaResource.getEm().contains(findUpdatedEntity));
            Assert.assertEquals(
                                "Assert that the entity's id is 1",
                                findUpdatedEntity.getId(),
                                1);
            Assert.assertEquals(
                                "Assert that the entity's strData field is \"New Data\"",
                                findUpdatedEntity.getStrData(),
                                "New Data");

            // 6) Delete the entity from the database
            System.out.println("6) Delete the entity from the database");

            System.out.println("Removing " + entityType.getEntityName() + "(id=1)...");
            jpaResource.getEm().remove(findUpdatedEntity);

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // 7) Verify the entity remove was successful
            System.out.println("7) Verify the entity remove was successful");

            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            // Perform the find operation
            System.out.println("Finding " + entityType.getEntityName() + "(id=1)...");
            ISimpleEntity10 findRemovedEntity = (ISimpleEntity10) jpaResource.getEm().find(resolveEntityClass(entityType), 1);
            System.out.println("Object returned by find: " + findRemovedEntity);

            // Verify that the entity could not be found.
            Assert.assertNull("Assert that the find operation did return null", findRemovedEntity);
        } finally {
            System.out.println("JarFileSupportTestLogic.testEntitiesInComponentArchive001(): End");
        }
    }

    /**
     * Test Logic: testEntitiesInJarFile001
     *
     * Performs basic CRUD operations with an entity within the jar archive.
     *
     * Expected Test Points on Pass: 9
     */
    public void testEntitiesInJarFile001(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                         Object managedComponentObject) throws Throwable {
        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testEntitiesInJarFile001: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("JarFileSupportTestLogic.testEntitiesInJarFile001(): Begin");

            // Cleanup the database for executing the test
            System.out.println("Cleaning up database before executing test...");
//            cleanupDatabase(jpaResource.getEm(), jpaResource.getTj(), EntityClasses.values(), log);
            System.out.println("Database cleanup complete.\n");

            // log.addInfo("Testing basic CRUD operations...");

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            // Populate the database with employees and departments
            System.out.println("Populating the database with employees and departments...");
            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            EmployeeUtilities.populateEmployeeDatabase(jpaResource.getEm());

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            System.out.println("No Exceptions have been thrown during the creation process, indicating that " +
                               "the entity types Employee, Manager, and Department are recognized by the pctx.");

            // Verify entity creation by querying for all employees
            System.out.println("Verify entity creation by querying for all employees:");

            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            String queryStr = "SELECT e FROM Employee e";
            System.out.println("Invoking query \"" + queryStr + "\"...");
            Query query = jpaResource.getEm().createQuery(queryStr);
            List<Employee> resultList = query.getResultList();

            Assert.assertNotNull("Assert that query.getResultList did not return null.", resultList);
            Assert.assertEquals("Assert that the length of the result set is " + EmployeeUtilities.getEmployeeCount(),
                                EmployeeUtilities.getEmployeeCount(), resultList.size());

            // Verify that each employee is present in the list
            boolean employeePresent[] = new boolean[EmployeeUtilities.getEmployeeCount()];
            for (int i = 0; i < employeePresent.length; i++) {
                employeePresent[i] = false;
            }

            for (Employee emp : resultList) {
                int index = emp.getId() - 1;
                if (index < 0 || index >= employeePresent.length) {
                    Assert.fail("Employee ID would result in IndexOutOfBounds Exception: " + emp);
                    continue;
                }

                employeePresent[index] = true;
            }

            boolean allEmployeesPresent = true;
            for (int i = 0; i < employeePresent.length; i++) {
                boolean thisEmpIsPresent = employeePresent[i];
                allEmployeesPresent = allEmployeesPresent && thisEmpIsPresent;
                if (thisEmpIsPresent == false) {
                    String missingEmployeeName = EmployeeUtilities.getEmployeeLastName(i) + ", " +
                                                 EmployeeUtilities.getEmployeeFirstName(i);
                    Assert.fail("Employee id " + (i + 1) + " is missing (" + missingEmployeeName + ")");
                }
            }
            Assert.assertTrue("Assert that every expected Employee is accounted for.", allEmployeesPresent);

            // Fire an employee
            System.out.println("Now, search for and fire an unlucky employee...");
            int idOfEmployeeToFire = EmployeeUtilities.getEmployeeID(6);
            Employee employeeToFire = jpaResource.getEm().find(Employee.class, idOfEmployeeToFire);
            Assert.assertNotNull("Failed to find Employee with id " + idOfEmployeeToFire, employeeToFire);

            System.out.println("Firing " + employeeToFire + " ...");
            jpaResource.getEm().remove(employeeToFire);

            // Mutate another employee
            System.out.println("Mutate the persistent state of another entity...");
            int idOfEmployeeToMutate = EmployeeUtilities.getEmployeeID(0);
            Employee employeeToMutate = jpaResource.getEm().find(Employee.class, idOfEmployeeToMutate);
            Assert.assertNotNull("Failed to find Employee with id " + employeeToMutate, employeeToMutate);

            System.out.println("Mutating " + employeeToMutate + " ...");
            employeeToMutate.setPosition("Some president of the United States");

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            // Verify the fired and mutated employee.
            System.out.println("Verify the fired and mutated employee.");

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            Employee employeeThatShouldBeFired = jpaResource.getEm().find(Employee.class, idOfEmployeeToFire);
            Assert.assertNull("Assert that em.find() returned null for id=" + idOfEmployeeToFire, employeeThatShouldBeFired);

            Employee employeeThatShouldBeMutated = jpaResource.getEm().find(Employee.class, idOfEmployeeToMutate);
            Assert.assertNotNull("Assert that em.find() returned an entity for id=" + idOfEmployeeToMutate, employeeThatShouldBeMutated);
            Assert.assertEquals("Assert that the mutation took place.", "Some president of the United States", employeeThatShouldBeMutated.getPosition());

            System.out.println("Rolling back transaction...");
            jpaResource.getTj().rollbackTransaction();
        } finally {
            System.out.println("JarFileSupportTestLogic.testEntitiesInJarFile001(): End");
        }
    }
}
