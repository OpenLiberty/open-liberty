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

import javax.ejb.Local;
import javax.ejb.Remove;
import javax.ejb.Stateful;

/**
 * Stateful Bean implementation for testing EJB injection.
 **/
@Stateful(name = "Dog")
@Local(DogLocal.class)
public class DogBean extends Animal {
    @Override
    public String whatAmI() {
        return "I am a dog.";
    }

    @Override
    public String careInst() {
        return "Give me water and Puppy Chow.";
    }

    @Override
    public String favToy() {
        return "Just a bone.";
    }

    public String dogDef() // d452259
    {
        return "Dog: any carnivore of the dogfamily Canidae.";
    }

    /** Remove method **/
    @Remove
    public void finish() {
        // Intentionally blank
    }

    // Provided for compatibility with SLSB
    public void discardInstance() {
        finish();
    }

    public DogBean() {
        // Intentionally blank
    }

}
