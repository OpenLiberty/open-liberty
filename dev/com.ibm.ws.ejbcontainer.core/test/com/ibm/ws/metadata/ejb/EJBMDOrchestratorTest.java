/*******************************************************************************
 * Copyright (c) 2008, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.metadata.ejb;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Timeout;
import javax.ejb.Timer;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Assert;
import org.junit.Test;

import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.csi.EJBModuleMetaDataImpl;
import com.ibm.ws.javaee.dd.ejb.NamedMethod;
import com.ibm.ws.javaee.dd.ejb.Session;

public class EJBMDOrchestratorTest
{
    final Mockery mockery = new Mockery();

    private EJBMDOrchestrator createEJBMDOrchestrator()
    {
        return new TestEJBMDOrchestrator();
    }

    private BeanMetaData createBeanMetaData()
    {
        BeanMetaData bmd = new BeanMetaData(0);
        bmd.wccm = new TestWCCMMetaData();
        bmd._moduleMetaData = new EJBModuleMetaDataImpl(0, null);
        return bmd;
    }

    private NamedMethod createNamedMethod(final String name, Class<?>... params)
    {
        final List<String> paramNames = new ArrayList<String>();
        for (Class<?> param : params)
        {
            paramNames.add(param.getName());
        }

        return new NamedMethod()
        {
            @Override
            public String getMethodName()
            {
                return name;
            }

            @Override
            public List<String> getMethodParamList()
            {
                return paramNames;
            }
        };
    }

    /**
     * Ensure that processTimeoutMetaData can be called when ivTimeoutMethod is
     * already set.
     */
    @Test
    public void testProcessTimeoutMetaDataAgain()
                    throws Exception
    {
        BeanMetaData bmd = createBeanMetaData();
        bmd.enterpriseBeanClass = TestProcessTimeoutMetaDataAgain.class;
        bmd.ivTimeoutMethod = TestProcessTimeoutMetaDataAgain.class.getMethod("timeout");

        createEJBMDOrchestrator().processTimeoutMetaData(bmd);
    }

    public static class TestProcessTimeoutMetaDataAgain
    {
        @Timeout
        public void timeout() {}
    }

    /**
     * Ensure that the proper timeout method is found when a private method in
     * the bean class is specified in XML as a timeout method and it "overrides"
     * (but not really, since private methods don't override) the same method
     * signature in a superclass.
     */
    @Test
    public void testProcessTimeoutMetaDataXMLShadowPrivate()
                    throws Exception
    {
        final Session session = mockery.mock(Session.class);

        mockery.checking(new Expectations()
        {
            {
                allowing(session).getTimeoutMethod();
                will(returnValue(createNamedMethod("timeout", Timer.class)));
            }
        });

        BeanMetaData bmd = createBeanMetaData();
        bmd.wccm.enterpriseBean = session;
        bmd.enterpriseBeanClass = TestProcessTimeoutMetaDataXMLOverridePrivate.class;

        createEJBMDOrchestrator().processTimeoutMetaData(bmd);

        Assert.assertTrue(bmd.isTimedObject);
        Assert.assertEquals(TestProcessTimeoutMetaDataXMLOverridePrivate.class.getDeclaredMethod("timeout", Timer.class), bmd.ivTimeoutMethod);
    }

    public class TestProcessTimeoutMetaDataXMLOverridePrivate
                    extends TestProcessTimeoutMetaDataXMLOverridePrivateSuper
    {
        @SuppressWarnings("unused")
        private void timeout(Timer t)
        {
            // Nothing.
        }
    }

    public class TestProcessTimeoutMetaDataXMLOverridePrivateSuper
    {
        @SuppressWarnings("unused")
        private void timeout(Timer t)
        {
            // Nothing.
        }
    }
}
