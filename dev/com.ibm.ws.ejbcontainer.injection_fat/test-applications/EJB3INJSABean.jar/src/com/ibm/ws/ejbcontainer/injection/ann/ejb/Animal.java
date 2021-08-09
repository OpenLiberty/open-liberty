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

/**
 * Java class that will be extended by various animals such as dog and cat.
 **/
public class Animal implements AnimalLocal {
    @Override
    public String whatAmI() {
        return "I am an animal.";
    }

    @Override
    public String careInst() {
        return "Give me food and water.";
    }

    @Override
    public String favToy() {
        return "Just a generic animal toy.";
    }

    @Override
    public String animalDef() { // d452259
        return "Animal: any member of the kingdom Animalia.";
    }
}
