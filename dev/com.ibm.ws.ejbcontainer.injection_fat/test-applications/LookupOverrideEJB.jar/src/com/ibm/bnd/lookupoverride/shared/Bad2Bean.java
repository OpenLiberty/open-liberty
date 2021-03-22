/*******************************************************************************
 * Copyright (c) 2010, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

// Bean with an invalid @EJB reference

package com.ibm.bnd.lookupoverride.shared;

import javax.ejb.EJB;
import javax.ejb.Stateless;

@Stateless
public class Bad2Bean implements Bad {

    // Invalid combination of references, specifying lookup on
    // one field and and beanName on another field of the same name

    @EJB(name = "bad2combo", lookup = "ejblocal:com.ibm.bnd.lookupoverride.shared.TargetBean")
    TargetBean ivTarget1;

    @EJB(name = "bad2combo", beanName = "fooBean")
    TargetBean ivTarget2;

    // Not expected to succeed
    @Override
    public int boing() {

        return 57;

    }

}
