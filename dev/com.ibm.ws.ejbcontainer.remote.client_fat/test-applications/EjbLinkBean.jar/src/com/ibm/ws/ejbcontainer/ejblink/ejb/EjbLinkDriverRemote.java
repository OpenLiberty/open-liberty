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
 * Remote interface with methods to verify ejb-link, beanName, and AutoLink
 * behavior.
 **/
public interface EjbLinkDriverRemote {
    /**
     * Verify EJB link occurred properly to ejb-jar modules.
     **/
    public String verifyStyle1OtherJarXML();

    public String verifyStyle2OtherJarXML();

    public String verifyStyle3OtherJarXML();

    public String verifyStyle1SameJarXML();

    public String verifyStyle2SameJarXML();

    public String verifyStyle3SameJarXML();

    public String verifyStyle1OtherJarAnn();

    public String verifyStyle2OtherJarAnn();

    public String verifyStyle3OtherJarAnn();

    public String verifyStyle1SameJarAnn();

    public String verifyStyle2SameJarAnn();

    public String verifyStyle3SameJarAnn();

    /**
     * Verify EJB link occurred properly to .war modules.
     **/
    public String verifyStyle1OtherWarXML();

    public String verifyStyle2OtherWarXML();

    public String verifyStyle3OtherWarXML();

    public String verifyStyle1SameWarXML();

    public String verifyStyle2SameWarXML();

    public String verifyStyle3SameWarXML();

    public String verifyStyle1OtherWarAnn();

    public String verifyStyle2OtherWarAnn();

    public String verifyStyle3OtherWarAnn();

    public String verifyStyle1SameWarAnn();

    public String verifyStyle2SameWarAnn();

    public String verifyStyle3SameWarAnn();

    public String verifyStyle1BeanInJarAndWar();

    /**
     * Verify AutoLink occurred properly when an interface is implemented
     * only once.
     **/
    public String verifyAutoLinkToCurrentModule();

    public String verifyAutoLinkToOtherJar();

    public String verifyAutoLinkToOtherWar();

    /**
     * Verify AutoLink occurred properly when an interface is implemented
     * in two separate modules. This results in AmbigousEJBReferenceException
     * when one of the implemented beans is not in the current module.
     **/
    public String verifyAutoLinkToJarAndOtherJar();

    public String verifyAutoLinkToJarAndWar();

    public String verifyAutoLinkToOtherJarAndWar();

    public String verifyAutoLinkToWarAndOtherWar();

    /**
     * Verify AutoLink results in AmbiguousEJBRefenceException when an interface
     * is implemented twice in the same module.
     **/
    public String verifyAmbiguousEJBReferenceException();

    /**
     * Provides a means to destroy a SLSB. Should throw unchecked exception
     */
    public void discardInstance();
}
