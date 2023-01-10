/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.injection.ann.ejb;

/**
 * Local interface for DogBean
 **/
public interface DogLocal extends AnimalLocal {
    /**
     * Returns "I am a dog."
     */
    @Override
    public String whatAmI();

    /**
     * Returns "Give me water and Puppy Chow."
     */
    @Override
    public String careInst();

    /**
     * Returns "Just a bone."
     */
    @Override
    public String favToy();

    /**
     * Returns "Dog: any carnivore of the dogfamily Canidae."
     */
    public String dogDef(); // d452259

    /**
     * Clean up the bean if it is a SFSB
     */
    public void finish();
}
