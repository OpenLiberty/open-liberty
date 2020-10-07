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

/**
 * Basic Stateless Bean implementation for testing AutoLink to save from implementing
 * all these methods for AutoLink tests that result in AmbiguousEJBReferenceException
 **/
public class BasicEjbLinkTest {
    public String verifyStyle1OtherJarAnn() {
        return null;
    }

    public String verifyStyle1OtherJarXML() {
        return null;
    }

    public String verifyStyle1SameJarAnn() {
        return null;
    }

    public String verifyStyle1SameJarXML() {
        return null;
    }

    public String verifyStyle2OtherJarAnn() {
        return null;
    }

    public String verifyStyle2OtherJarXML() {
        return null;
    }

    public String verifyStyle2SameJarAnn() {
        return null;
    }

    public String verifyStyle2SameJarXML() {
        return null;
    }

    public String verifyStyle3OtherJarAnn() {
        return null;
    }

    public String verifyStyle3OtherJarXML() {
        return null;
    }

    public String verifyStyle3SameJarAnn() {
        return null;
    }

    public String verifyStyle3SameJarXML() {
        return null;
    }

    public String verifyStyle1OtherWarAnn() {
        return null;
    }

    public String verifyStyle1OtherWarXML() {
        return null;
    }

    public String verifyStyle1SameWarAnn() {
        return null;
    }

    public String verifyStyle1SameWarXML() {
        return null;
    }

    public String verifyStyle2OtherWarAnn() {
        return null;
    }

    public String verifyStyle2OtherWarXML() {
        return null;
    }

    public String verifyStyle2SameWarAnn() {
        return null;
    }

    public String verifyStyle2SameWarXML() {
        return null;
    }

    public String verifyStyle3OtherWarAnn() {
        return null;
    }

    public String verifyStyle3OtherWarXML() {
        return null;
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
        return null;
    }

    public String verifyAutoLinkToOtherJar() {
        return null;
    }

    public String verifyAutoLinkToOtherWar() {
        return null;
    }

    public String verifyAutoLinkToJarAndOtherJar() {
        return null;
    }

    public String verifyAutoLinkToJarAndWar() {
        return null;
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

    public BasicEjbLinkTest() {
        // intentionally blank
    }
}
