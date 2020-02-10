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

import javax.ejb.EJB;

import com.ibm.ws.ejbcontainer.ejblink.ejb.AutoLinkLocalJarOtherWar;
import com.ibm.ws.ejbcontainer.ejblink.ejb.BasicEjbLinkTest;

/**
 * Basic Stateless Bean implementation for testing AutoLink
 **/
public class TestAutoLinkOtherWarOtherJar extends BasicEjbLinkTest {
    public Object bean;

    @EJB
    public AutoLinkLocalJarOtherWar beanInJarAndOtherWar;

    public String verifyAutoLinkToOtherJarAndWar() {
        return "Failed";
    }

    public TestAutoLinkOtherWarOtherJar() {
        // intentionally blank
    }
}
