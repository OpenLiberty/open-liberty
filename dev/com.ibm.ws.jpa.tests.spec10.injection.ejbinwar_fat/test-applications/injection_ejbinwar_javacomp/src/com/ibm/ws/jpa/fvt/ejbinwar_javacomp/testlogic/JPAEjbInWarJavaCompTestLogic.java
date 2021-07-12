/**
 *
 */
package com.ibm.ws.jpa.fvt.ejbinwar_javacomp.testlogic;

import org.junit.Assert;

import com.ibm.ws.jpa.fvt.injection.entities.ejb.EJBEntityA;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

/**
 *
 */
public class JPAEjbInWarJavaCompTestLogic extends AbstractTestLogic {
    public void testCRUDOperations(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                   Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testInjectionTarget: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        try {
            System.out.println("Creating new instance of EJBEntityA (id=1) ...");
            EJBEntityA newEnt = new EJBEntityA();
            newEnt.setId(1);
            newEnt.setStrData("Some String");

            jpaResource.getTj().beginTransaction();
            jpaResource.getEm().joinTransaction();
            jpaResource.getEm().persist(newEnt);
            jpaResource.getTj().commitTransaction();

            jpaResource.getEm().clear();
            EJBEntityA findEnt = jpaResource.getEm().find(EJBEntityA.class, 1);
            Assert.assertNotNull(findEnt);

            System.out.println("Successfully saved EJBEntityA (id=1) to database.  Means java:comp/env DataSource worked.");

        } finally {
            System.out.println(testName + ": End");
        }

    }
}
