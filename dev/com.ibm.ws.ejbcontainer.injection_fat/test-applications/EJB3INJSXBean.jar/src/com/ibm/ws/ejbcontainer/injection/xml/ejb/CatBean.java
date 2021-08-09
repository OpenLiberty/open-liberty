/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.injection.xml.ejb;

/**
 * Basic Stateless bean with interface that has a parent for subclass injection testing.
 **/
// @Stateless
// @Local( Cat.class )
public class CatBean {
    /**
     * Returns the name of the Animal.
     **/
    public String getName() {
        return "Cat";
    }

    public String chaseMouse(String mouseName) {
        return ".>.>.>.>.>" + mouseName + "! Ouch!";
    }
}
