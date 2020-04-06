/*******************************************************************************
 * Copyright (c) 2006, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.ejblink.ejb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.annotation.security.PermitAll;
import javax.ejb.EJB;
import javax.ejb.SessionContext;

/**
 * Basic Stateless Bean implementation for testing EJB Injection via XML
 **/
@PermitAll
public class TestDriverFromJar {
    private static final String PASSED = "Passed";
    private static final String FAILED = "Failed";

    public Object bean;

    public EjbLinkLocal otherJarStyle1;

    private EjbLinkRemote otherJarStyle2;

    public EjbLinkLocal otherJarStyle3;

    public EjbLinkLocal sameJarStyle1;

    public EjbLinkLocal sameJarStyle2;

    public EjbLinkLocal sameJarStyle3;

    @EJB(beanName = "OtherJarBean")
    public EjbLinkLocal otherJarStyle1Ann;

    @EJB(beanName = "../EjbLinkOtherBean.jar#OtherJarBean")
    public EjbLinkLocal otherJarStyle2Ann;

    @EJB(beanName = "logicalOther/OtherJarBean")
    public EjbLinkLocal otherJarStyle3Ann;

    @EJB(beanName = "SameJarBean")
    public EjbLinkLocal sameJarStyle1Ann;

    @EJB(beanName = "../EjbLinkBean.jar#SameJarBean")
    public EjbLinkLocal sameJarStyle2Ann;

    @EJB(beanName = "EjbLinkBean/SameJarBean")
    public EjbLinkLocal sameJarStyle3Ann;

    public EjbLinkLocal warStyle1;

    @EJB(beanName = "OtherWarBean")
    public EjbLinkLocal warStyle1Ann;

    public EjbLinkLocal warStyle2;

    @EJB(beanName = "../EjbLinkInOtherWar.war#OtherWarBean")
    public EjbLinkLocal warStyle2Ann;

    public EjbLinkLocal warStyle3;

    @EJB(beanName = "logicalOtherWar/OtherWarBean")
    public EjbLinkLocal warStyle3Ann;

    @EJB
    public AutoLinkLocalJar beanInSameJar;

    @EJB
    public AutoLinkLocalOtherJar beanInOtherJar;

    @EJB
    public AutoLinkLocalOtherWar beanInOtherWar;

    @EJB
    public AutoLinkLocalJarJar beanInJarAndOtherJar;

    @EJB
    public AutoLinkLocalJarWar beanInJarAndWar;

    private SessionContext ivContext;

//   private void assertEquals(String message, Object obj1, Object obj2) {
//	   if (!obj1.equals(obj2)) {
//		   throw new RuntimeException("Objects not equal");
//	   }
//   }
//
//   private void assertNotNull(String message, Object obj) {
//	   if (obj == null) {
//		   throw new RuntimeException("Object was null");
//	   }
//   }

    //@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public String verifyStyle1OtherJarXML() {
        if (otherJarStyle1 == null)
            return FAILED;

        assertEquals("otherJarStyle1 is com.ibm.ws.ejbcontainer.ejblink.ejbo.OtherJarBean",
                     otherJarStyle1.getBeanName(), "com.ibm.ws.ejbcontainer.ejblink.ejbo.OtherJarBean");

        return PASSED;
    }

    public String verifyStyle2OtherJarXML() {
        if (otherJarStyle2 == null)
            return FAILED;

        String envName = null;

        assertEquals("otherJarStyle2 is OtherJarBean", otherJarStyle2.getBeanName(),
                     "com.ibm.ws.ejbcontainer.ejblink.ejbo.OtherJarBean");

        envName = "ejb/OtherJarStyle2";
        bean = ivContext.lookup(envName);
        assertNotNull("lookup:" + envName, bean);

        EjbLinkRemote ctxOtherJar = (EjbLinkRemote) bean;
        bean = null;
        assertEquals("ctxOtherJar is OtherJarBean", ctxOtherJar.getBeanName(),
                     "com.ibm.ws.ejbcontainer.ejblink.ejbo.OtherJarBean");

        return PASSED;
    }

    public String verifyStyle3OtherJarXML() {
        if (otherJarStyle3 == null)
            return FAILED;

        assertEquals("otherJarStyle3 is com.ibm.ws.ejbcontainer.ejblink.ejbo.OtherJarBean",
                     otherJarStyle3.getBeanName(), "com.ibm.ws.ejbcontainer.ejblink.ejbo.OtherJarBean");

        return PASSED;
    }

    public String verifyStyle1SameJarXML() {
        if (sameJarStyle1 == null)
            return FAILED;

        assertEquals("sameJarStyle1 is com.ibm.ws.ejbcontainer.ejblink.ejb.SameJarBean",
                     sameJarStyle1.getBeanName(), "com.ibm.ws.ejbcontainer.ejblink.ejb.SameJarBean");

        return PASSED;
    }

    public String verifyStyle2SameJarXML() {
        if (sameJarStyle2 == null)
            return FAILED;

        assertEquals("sameJarStyle2 is com.ibm.ws.ejbcontainer.ejblink.ejb.SameJarBean",
                     sameJarStyle2.getBeanName(), "com.ibm.ws.ejbcontainer.ejblink.ejb.SameJarBean");

        return PASSED;
    }

    public String verifyStyle3SameJarXML() {
        if (sameJarStyle3 == null)
            return FAILED;

        assertEquals("sameJarStyle3 is com.ibm.ws.ejbcontainer.ejblink.ejb.SameJarBean",
                     sameJarStyle3.getBeanName(), "com.ibm.ws.ejbcontainer.ejblink.ejb.SameJarBean");

        return PASSED;
    }

    public String verifyStyle1OtherJarAnn() {
        if (otherJarStyle1Ann == null)
            return FAILED;

        assertEquals("otherJarStyle1Ann is com.ibm.ws.ejbcontainer.ejblink.ejbo.OtherJarBean",
                     otherJarStyle1Ann.getBeanName(), "com.ibm.ws.ejbcontainer.ejblink.ejbo.OtherJarBean");

        return PASSED;
    }

    public String verifyStyle2OtherJarAnn() {
        if (otherJarStyle2Ann == null)
            return FAILED;

        assertEquals("otherJarStyle2Ann is com.ibm.ws.ejbcontainer.ejblink.ejbo.OtherJarBean",
                     otherJarStyle2Ann.getBeanName(), "com.ibm.ws.ejbcontainer.ejblink.ejbo.OtherJarBean");

        return PASSED;
    }

    public String verifyStyle3OtherJarAnn() {
        if (otherJarStyle3Ann == null)
            return FAILED;

        assertEquals("otherJarStyle3Ann is com.ibm.ws.ejbcontainer.ejblink.ejbo.OtherJarBean",
                     otherJarStyle3Ann.getBeanName(), "com.ibm.ws.ejbcontainer.ejblink.ejbo.OtherJarBean");

        return PASSED;
    }

    public String verifyStyle1SameJarAnn() {
        if (sameJarStyle1Ann == null)
            return FAILED;

        assertEquals("sameJarStyle1Ann is com.ibm.ws.ejbcontainer.ejblink.ejb.SameJarBean",
                     sameJarStyle1Ann.getBeanName(), "com.ibm.ws.ejbcontainer.ejblink.ejb.SameJarBean");

        return PASSED;
    }

    public String verifyStyle2SameJarAnn() {
        if (sameJarStyle2Ann == null)
            return FAILED;

        assertEquals("sameJarStyle2Ann is com.ibm.ws.ejbcontainer.ejblink.ejb.SameJarBean",
                     sameJarStyle2Ann.getBeanName(), "com.ibm.ws.ejbcontainer.ejblink.ejb.SameJarBean");

        return PASSED;
    }

    public String verifyStyle3SameJarAnn() {
        if (sameJarStyle3Ann == null)
            return FAILED;

        assertEquals("sameJarStyle3Ann is com.ibm.ws.ejbcontainer.ejblink.ejb.SameJarBean",
                     sameJarStyle3Ann.getBeanName(), "com.ibm.ws.ejbcontainer.ejblink.ejb.SameJarBean");

        return PASSED;
    }

    public String verifyStyle1OtherWarAnn() {
        if (warStyle1Ann == null)
            return FAILED;

        assertEquals("warStyle1Ann is com.ibm.ws.ejbcontainer.ejblink.ejbwaro.OtherWarBean",
                     warStyle1Ann.getBeanName(), "com.ibm.ws.ejbcontainer.ejblink.ejbwaro.OtherWarBean");

        return PASSED;
    }

    public String verifyStyle1OtherWarXML() {
        if (warStyle1 == null)
            return FAILED;

        assertEquals("warStyle1 is com.ibm.ws.ejbcontainer.ejblink.ejbwaro.OtherWarBean",
                     warStyle1.getBeanName(), "com.ibm.ws.ejbcontainer.ejblink.ejbwaro.OtherWarBean");

        return PASSED;
    }

    public String verifyStyle1SameWarAnn() {
        return null;
    }

    public String verifyStyle1SameWarXML() {
        return null;
    }

    public String verifyStyle2OtherWarAnn() {
        if (warStyle2Ann == null)
            return FAILED;

        assertEquals("warStyle2Ann is com.ibm.ws.ejbcontainer.ejblink.ejbwaro.OtherWarBean",
                     warStyle2Ann.getBeanName(), "com.ibm.ws.ejbcontainer.ejblink.ejbwaro.OtherWarBean");

        return PASSED;
    }

    public String verifyStyle2OtherWarXML() {
        if (warStyle2 == null)
            return FAILED;

        assertEquals("warStyle2 is com.ibm.ws.ejbcontainer.ejblink.ejbwaro.OtherWarBean",
                     warStyle2.getBeanName(), "com.ibm.ws.ejbcontainer.ejblink.ejbwaro.OtherWarBean");

        return PASSED;
    }

    public String verifyStyle2SameWarAnn() {
        return null;
    }

    public String verifyStyle2SameWarXML() {
        return null;
    }

    public String verifyStyle3OtherWarAnn() {
        if (warStyle3Ann == null)
            return FAILED;

        assertEquals("warStyle3Ann is com.ibm.ws.ejbcontainer.ejblink.ejbwaro.OtherWarBean",
                     warStyle3Ann.getBeanName(), "com.ibm.ws.ejbcontainer.ejblink.ejbwaro.OtherWarBean");

        return PASSED;
    }

    public String verifyStyle3OtherWarXML() {
        if (warStyle3 == null)
            return FAILED;

        assertEquals("warStyle3 is com.ibm.ws.ejbcontainer.ejblink.ejbwaro.OtherWarBean",
                     warStyle3.getBeanName(), "com.ibm.ws.ejbcontainer.ejblink.ejbwaro.OtherWarBean");

        return PASSED;
    }

    public String verifyStyle3SameWarAnn() {
        return null;
    }

    public String verifyStyle3SameWarXML() {
        return null;
    }

    public String verifyStyle1BeanInJarAndWar() {
        return null;
    }

    public String verifyAutoLinkToCurrentModule() {
        if (beanInSameJar == null)
            return FAILED;

        assertEquals("beanInSameJar is com.ibm.ws.ejbcontainer.ejblink.ejb.TestBean",
                     beanInSameJar.getBeanName(), "com.ibm.ws.ejbcontainer.ejblink.ejb.TestBean");

        return PASSED;
    }

    public String verifyAutoLinkToOtherJar() {
        if (beanInOtherJar == null)
            return FAILED;

        assertEquals("beanInOtherJar is com.ibm.ws.ejbcontainer.ejblink.ejbo.TestBean",
                     beanInOtherJar.getBeanName(), "com.ibm.ws.ejbcontainer.ejblink.ejbo.TestBean");

        return PASSED;
    }

    public String verifyAutoLinkToOtherWar() {
        if (beanInOtherWar == null)
            return FAILED;

        assertEquals("beanInOtherWar is com.ibm.ws.ejbcontainer.ejblink.ejbwaro.TestBean",
                     beanInOtherWar.getBeanName(), "com.ibm.ws.ejbcontainer.ejblink.ejbwaro.TestBean");

        return PASSED;
    }

    public String verifyAutoLinkToJarAndOtherJar() {
        if (beanInJarAndOtherJar == null)
            return FAILED;

        assertEquals("beanInJarAndOtherJar is com.ibm.ws.ejbcontainer.ejblink.ejb.JarJarBean",
                     beanInJarAndOtherJar.getBeanName(), "com.ibm.ws.ejbcontainer.ejblink.ejb.JarJarBean");

        return PASSED;
    }

    public String verifyAutoLinkToJarAndWar() {
        if (beanInJarAndWar == null)
            return FAILED;

        assertEquals("beanInJarAndWar is com.ibm.ws.ejbcontainer.ejblink.ejb.JarWarBean",
                     beanInJarAndWar.getBeanName(), "com.ibm.ws.ejbcontainer.ejblink.ejb.JarWarBean");

        return PASSED;
    }

    public String verifyAutoLinkToOtherJarAndWar() {
        return null;
    }

    public String verifyAutoLinkToWarAndOtherWar() {
        return null;
    }

    public String verifyAmbiguousEJBReferenceException() {
        return null;
    }

    /* Added this method for SLSB to provide a way for a client to remove a bean from the cache */
    public void discardInstance() {
        throw new javax.ejb.EJBException("discardInstance");
    }

    public TestDriverFromJar() {
        // intentionally blank
    }
}
