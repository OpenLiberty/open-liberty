/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.testtooling.vehicle.ejb;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.transaction.UserTransaction;

import org.junit.Assert;

import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.vehicle.JEEExecutionContextHelper;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

/**
 * This simple test vehicle EJB can be implemented as either a Stateless or Stateful session bean.
 *
 * You can choose to either override this class and annotate new new class as an EJB, or define a new EJB
 * using this class in the deployment descriptor.
 *
 * Either way, a UserTransaction will be injected into the EJB, hence this class is suitable only
 * for Bean Managed Transaction configured EJBs. You will probably get errors starting the bean
 * if you attempt to define the bean as a CMT in whichever approach you choose to define the EJB.
 *
 * If you define a SFSB, remember to define the release() method as the bean's Release method.
 *
 */
public class BMTEJBTestVehicle implements EJBTestVehicle {
    @Resource
    protected UserTransaction tx;

    @Resource
    protected EJBContext ejbCtx;

    protected String beanName = "";

    @PostConstruct
    protected void postConstruct() {
        try {
            beanName = (String) ejbCtx.lookup("beanName");
            System.out.println("Bean self identifying with identity \"" + beanName + "\".");
        } catch (Throwable t) {
            // Swallow.  Defining a bean name is not required by the test framework.
        }
    }

    @PreDestroy
    protected void preDestroy() {

    }

    @Override
    public String getEnvDefinedBeanName() {
        return beanName;
    }

    @Override
    public void executeTestLogic(TestExecutionContext ctx) {
        TestExecutionResources testExecResources = null;

        try {
            String beanName = getEnvDefinedBeanName();
            if (beanName != null && !beanName.trim().isEmpty()) {
                ctx.getProperties().put("managed.resource.name", beanName);
            }

            JEEExecutionContextHelper.printBeginTestInfo(ctx);

            // Create resources needed by the test
            testExecResources = JEEExecutionContextHelper.processTestExecutionResources(ctx, this, tx);

            // Execute the test
            JEEExecutionContextHelper.executeTestLogic(ctx, testExecResources, this);
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            Assert.fail("BMTEJBTestVehicle Caught Exception: " + t);
        } finally {
            // Cleanup Resources
            try {
                JEEExecutionContextHelper.destroyExecutionResources(testExecResources);
            } catch (Throwable t) {
                Assert.fail("BMTEJBTestVehicle Cleanup Caught Exception: " + t);
            }

            JEEExecutionContextHelper.printEndTestInfo(ctx);
        }
    }

    @Override
    public void release() {

    }

}