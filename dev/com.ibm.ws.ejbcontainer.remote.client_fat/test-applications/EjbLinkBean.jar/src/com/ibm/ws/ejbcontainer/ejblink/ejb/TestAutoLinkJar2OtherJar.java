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

import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.Stateless;

/**
 * Basic Stateless Bean implementation for testing AutoLink
 **/
@Stateless
@Remote(EjbLinkDriverRemote.class)
public class TestAutoLinkJar2OtherJar extends BasicEjbLinkTest implements EjbLinkDriverLocal {
    @EJB
    public AutoLinkLocal2OtherJar beanInOtherJarModuleTwice;

    @Override
    public String verifyAmbiguousEJBReferenceException() {
        return "Failed";
    }

    public TestAutoLinkJar2OtherJar() {
        // intentionally blank
    }
}
