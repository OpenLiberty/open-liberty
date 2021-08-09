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

package com.ibm.ws.ejbcontainer.ejblink.ejbwar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.ejb.EJB;
import javax.ejb.SessionContext;

import com.ibm.ws.ejbcontainer.ejblink.ejb.AutoLinkLocalJarWar;
import com.ibm.ws.ejbcontainer.ejblink.ejb.AutoLinkLocalOtherJar;
import com.ibm.ws.ejbcontainer.ejblink.ejb.AutoLinkLocalOtherWar;
import com.ibm.ws.ejbcontainer.ejblink.ejb.AutoLinkLocalWar;
import com.ibm.ws.ejbcontainer.ejblink.ejb.AutoLinkLocalWarOtherWar;
import com.ibm.ws.ejbcontainer.ejblink.ejb.EjbLinkLocal;
import com.ibm.ws.ejbcontainer.ejblink.ejb.EjbLinkRemote;

public class TestDriverFromWar {
    private static final String PASSED = "Passed";
    private static final String FAILED = "Failed";

    public Object bean;

    public EjbLinkLocal otherWarStyle1;

    private EjbLinkRemote otherWarStyle2;

    public EjbLinkLocal otherWarStyle3;

    public EjbLinkLocal sameWarStyle1;

    public EjbLinkLocal sameWarStyle2;

    public EjbLinkLocal sameWarStyle3;

    @EJB(beanName = "OtherWarBean")
    public EjbLinkLocal otherWarStyle1Ann;

    @EJB(beanName = "../EjbLinkInOtherWar.war#OtherWarBean")
    public EjbLinkLocal otherWarStyle2Ann;

    @EJB(beanName = "logicalOtherWar/OtherWarBean")
    public EjbLinkLocal otherWarStyle3Ann;

    @EJB(beanName = "SameWarBean")
    public EjbLinkLocal sameWarStyle1Ann;

    @EJB(beanName = "../EjbLinkInWar.war#SameWarBean")
    public EjbLinkLocal sameWarStyle2Ann;

    @EJB(beanName = "ejbinwar/SameWarBean")
    public EjbLinkLocal sameWarStyle3Ann;

    public EjbLinkLocal jarStyle1;

    @EJB(beanName = "OtherJarBean")
    public EjbLinkLocal jarStyle1Ann;

    public EjbLinkLocal jarStyle2;

    @EJB(beanName = "../EjbLinkOtherBean.jar#OtherJarBean")
    public EjbLinkLocal jarStyle2Ann;

    public EjbLinkLocal jarStyle3;

    @EJB(beanName = "logicalOther/OtherJarBean")
    public EjbLinkLocal jarStyle3Ann;

    @EJB
    public AutoLinkLocalWar beanInSameWar;

    @EJB
    public AutoLinkLocalOtherJar beanInOtherJar;

    @EJB
    public AutoLinkLocalOtherWar beanInOtherWar;

    @EJB
    public AutoLinkLocalJarWar beanInJarAndWar;

    @EJB
    public AutoLinkLocalWarOtherWar beanInWarAndOtherWar;

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

    public String verifyStyle1OtherWarXML() {
        if (otherWarStyle1 == null)
            return FAILED;

        assertEquals("otherWarStyle1 is com.ibm.ws.ejbcontainer.ejblink.ejbwaro.OtherWarBean",
                     otherWarStyle1.getBeanName(), "com.ibm.ws.ejbcontainer.ejblink.ejbwaro.OtherWarBean");

        return PASSED;
    }

    public String verifyStyle2OtherWarXML() {
        if (otherWarStyle2 == null)
            return FAILED;

        String envName = null;

        assertEquals("otherWarStyle2 is OtherWarBean", otherWarStyle2.getBeanName(),
                     "com.ibm.ws.ejbcontainer.ejblink.ejbwaro.OtherWarBean");

        envName = "ejb/OtherWarStyle2";
        bean = ivContext.lookup(envName);
        assertNotNull("lookup:" + envName, bean);

        EjbLinkRemote ctxOtherWar = (EjbLinkRemote) bean;
        bean = null;
        assertEquals("ctxOtherWar is OtherWarBean", ctxOtherWar.getBeanName(),
                     "com.ibm.ws.ejbcontainer.ejblink.ejbwaro.OtherWarBean");

        return PASSED;
    }

    public String verifyStyle3OtherWarXML() {
        if (otherWarStyle3 == null)
            return FAILED;

        assertEquals("otherWarStyle3 is com.ibm.ws.ejbcontainer.ejblink.ejbwaro.OtherWarBean",
                     otherWarStyle3.getBeanName(), "com.ibm.ws.ejbcontainer.ejblink.ejbwaro.OtherWarBean");

        return PASSED;
    }

    public String verifyStyle1SameWarXML() {
        if (sameWarStyle1 == null)
            return FAILED;

        assertEquals("sameWarStyle1 is com.ibm.ws.ejbcontainer.ejblink.ejbwar.SameWarBean",
                     sameWarStyle1.getBeanName(), "com.ibm.ws.ejbcontainer.ejblink.ejbwar.SameWarBean");

        return PASSED;
    }

    public String verifyStyle2SameWarXML() {
        if (sameWarStyle2 == null)
            return FAILED;

        assertEquals("sameWarStyle2 is com.ibm.ws.ejbcontainer.ejblink.ejbwar.SameWarBean",
                     sameWarStyle2.getBeanName(), "com.ibm.ws.ejbcontainer.ejblink.ejbwar.SameWarBean");

        return PASSED;
    }

    public String verifyStyle3SameWarXML() {
        if (sameWarStyle3 == null)
            return FAILED;

        assertEquals("sameWarStyle3 is com.ibm.ws.ejbcontainer.ejblink.ejbwar.SameWarBean",
                     sameWarStyle3.getBeanName(), "com.ibm.ws.ejbcontainer.ejblink.ejbwar.SameWarBean");

        return PASSED;
    }

    public String verifyStyle1OtherWarAnn() {
        if (otherWarStyle1Ann == null)
            return FAILED;

        assertEquals("otherWarStyle1Ann is com.ibm.ws.ejbcontainer.ejblink.ejbwaro.OtherWarBean",
                     otherWarStyle1Ann.getBeanName(), "com.ibm.ws.ejbcontainer.ejblink.ejbwaro.OtherWarBean");

        return PASSED;
    }

    public String verifyStyle2OtherWarAnn() {
        if (otherWarStyle2Ann == null)
            return FAILED;

        assertEquals("otherWarStyle2Ann is com.ibm.ws.ejbcontainer.ejblink.ejbwaro.OtherWarBean",
                     otherWarStyle2Ann.getBeanName(), "com.ibm.ws.ejbcontainer.ejblink.ejbwaro.OtherWarBean");

        return PASSED;
    }

    public String verifyStyle3OtherWarAnn() {
        if (otherWarStyle3Ann == null)
            return FAILED;

        assertEquals("otherWarStyle3Ann is com.ibm.ws.ejbcontainer.ejblink.ejbwaro.OtherWarBean",
                     otherWarStyle3Ann.getBeanName(), "com.ibm.ws.ejbcontainer.ejblink.ejbwaro.OtherWarBean");

        return PASSED;
    }

    public String verifyStyle1SameWarAnn() {
        if (sameWarStyle1Ann == null)
            return FAILED;

        assertEquals("sameWarStyle1Ann is com.ibm.ws.ejbcontainer.ejblink.ejbwar.SameWarBean",
                     sameWarStyle1Ann.getBeanName(), "com.ibm.ws.ejbcontainer.ejblink.ejbwar.SameWarBean");

        return PASSED;
    }

    public String verifyStyle2SameWarAnn() {
        if (sameWarStyle2Ann == null)
            return FAILED;

        assertEquals("sameWarStyle2Ann is com.ibm.ws.ejbcontainer.ejblink.ejbwar.SameWarBean",
                     sameWarStyle2Ann.getBeanName(), "com.ibm.ws.ejbcontainer.ejblink.ejbwar.SameWarBean");

        return PASSED;
    }

    public String verifyStyle3SameWarAnn() {
        if (sameWarStyle3Ann == null)
            return FAILED;

        assertEquals("sameWarStyle3Ann is com.ibm.ws.ejbcontainer.ejblink.ejbwar.SameWarBean",
                     sameWarStyle3Ann.getBeanName(), "com.ibm.ws.ejbcontainer.ejblink.ejbwar.SameWarBean");

        return PASSED;
    }

    public String verifyAutoLinkToCurrentModule() {
        if (beanInSameWar == null)
            return FAILED;

        assertEquals("beanInSameJar is com.ibm.ws.ejbcontainer.ejblink.ejbwar.TestBean",
                     beanInSameWar.getBeanName(), "com.ibm.ws.ejbcontainer.ejblink.ejbwar.TestBean");

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
        return FAILED;
    }

    public String verifyAutoLinkToJarAndWar() {
        if (beanInJarAndWar == null)
            return FAILED;

        assertEquals("beanInJarAndWar is com.ibm.ws.ejbcontainer.ejblink.ejbwar.JarWarBean",
                     beanInJarAndWar.getBeanName(), "com.ibm.ws.ejbcontainer.ejblink.ejbwar.JarWarBean");

        return PASSED;
    }

    public String verifyAutoLinkToOtherJarAndWar() {
        return null;
    }

    public String verifyAutoLinkToWarAndOtherWar() {
        if (beanInWarAndOtherWar == null)
            return FAILED;

        assertEquals("beanInWarAndOtherWar is com.ibm.ws.ejbcontainer.ejblink.ejbwar.WarWarBean",
                     beanInWarAndOtherWar.getBeanName(), "com.ibm.ws.ejbcontainer.ejblink.ejbwar.WarWarBean");

        return PASSED;
    }

    public String verifyStyle1OtherJarAnn() {
        if (jarStyle1Ann == null)
            return FAILED;

        assertEquals("jarStyle1Ann is com.ibm.ws.ejbcontainer.ejblink.ejbo.OtherJarBean",
                     jarStyle1Ann.getBeanName(), "com.ibm.ws.ejbcontainer.ejblink.ejbo.OtherJarBean");

        return PASSED;
    }

    public String verifyStyle1OtherJarXML() {
        if (jarStyle1 == null)
            return FAILED;

        assertEquals("jarStyle1 is com.ibm.ws.ejbcontainer.ejblink.ejbo.OtherJarBean",
                     jarStyle1.getBeanName(), "com.ibm.ws.ejbcontainer.ejblink.ejbo.OtherJarBean");

        return PASSED;
    }

    public String verifyStyle1SameJarAnn() {
        return null;
    }

    public String verifyStyle1SameJarXML() {
        return null;
    }

    public String verifyStyle2OtherJarAnn() {
        if (jarStyle2Ann == null)
            return FAILED;

        assertEquals("jarStyle2Ann is com.ibm.ws.ejbcontainer.ejblink.ejbo.OtherJarBean",
                     jarStyle2Ann.getBeanName(), "com.ibm.ws.ejbcontainer.ejblink.ejbo.OtherJarBean");

        return PASSED;
    }

    public String verifyStyle2OtherJarXML() {
        if (jarStyle2 == null)
            return FAILED;

        assertEquals("jarStyle2 is com.ibm.ws.ejbcontainer.ejblink.ejbo.OtherJarBean",
                     jarStyle2.getBeanName(), "com.ibm.ws.ejbcontainer.ejblink.ejbo.OtherJarBean");

        return PASSED;
    }

    public String verifyStyle2SameJarAnn() {
        return null;
    }

    public String verifyStyle2SameJarXML() {
        return null;
    }

    public String verifyStyle3OtherJarAnn() {
        if (jarStyle3Ann == null)
            return FAILED;

        assertEquals("jarStyle3Ann is com.ibm.ws.ejbcontainer.ejblink.ejbo.OtherJarBean",
                     jarStyle3Ann.getBeanName(), "com.ibm.ws.ejbcontainer.ejblink.ejbo.OtherJarBean");

        return PASSED;
    }

    public String verifyStyle3OtherJarXML() {
        if (jarStyle3 == null)
            return FAILED;

        assertEquals("jarStyle3 is com.ibm.ws.ejbcontainer.ejblink.ejbo.OtherJarBean",
                     jarStyle3.getBeanName(), "com.ibm.ws.ejbcontainer.ejblink.ejbo.OtherJarBean");

        return PASSED;
    }

    public String verifyStyle3SameJarAnn() {
        return null;
    }

    public String verifyStyle3SameJarXML() {
        return null;
    }

    public String verifyStyle1BeanInJarAndWar() {
        return null;
    }

    public String verifyAmbiguousEJBReferenceException() {
        return null;
    }

    /* Added this method for SLSB to provide a way for a client to remove a bean from the cache */
    public void discardInstance() {
        throw new javax.ejb.EJBException("discardInstance");
    }

    public TestDriverFromWar() {
        // intentionally blank
    }
}
