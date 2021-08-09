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

package com.ibm.ws.ejbcontainer.injection.ann.ejb;

import javax.ejb.EJBLocalObject;

/**
 * Compatibility EJBLocal interface for CompCatBean
 **/
public interface CatEJBLocal extends EJBLocalObject {
    /**
     * Returns "I am a cat."
     */
    public String whatAmI();

    /**
     * Returns "Give me milk and tuna."
     */
    public String careInst();

    /**
     * Returns "Just a ball of string."
     */
    public String favToy();

    /**
     * Returns "Cat: any of several carnivores of the family Felidae."
     */
    public String catDef();

    /**
     * Returns "Animal: any member of the kingdom Animalia."
     */
    public String animalDef(); // d452259

    /**
     * Clean up the bean if it is a SFSB
     */
    public void finish();
}
