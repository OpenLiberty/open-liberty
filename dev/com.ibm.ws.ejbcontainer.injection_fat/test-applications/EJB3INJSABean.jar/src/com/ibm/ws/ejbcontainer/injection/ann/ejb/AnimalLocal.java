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
 * Local interface for Animal
 **/
public interface AnimalLocal {
    /**
     * Returns "I am an animal."
     */
    public String whatAmI();

    /**
     * Returns "Give me food and water."
     */
    public String careInst();

    /**
     * Returns "Just a generic animal toy."
     */
    public String favToy();

    /**
     * Returns "Animal: any member of the kingdom Animalia."
     */
    public String animalDef(); // d452259

}
