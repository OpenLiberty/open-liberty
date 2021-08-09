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
@Stateful(name = "Cat")
@Local(CatLocal.class)
public class CatBean extends Animal {
    @Override
    public String whatAmI() {
        return "I am a cat.";
    }

    @Override
    public String careInst() {
        return "Give me milk and tuna.";
    }

    @Override
    public String favToy() {
        return "Just a ball of string.";
    }

    public String catDef() // d452259
    {
        return "Cat: any of several carnivores of the family Felidae.";
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

    public CatBean() {
        // Intentionally blank
    }
}
