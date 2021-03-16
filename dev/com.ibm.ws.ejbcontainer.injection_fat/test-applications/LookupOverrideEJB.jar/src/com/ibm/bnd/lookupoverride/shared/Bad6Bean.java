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

// Bean with an field not reachable at runtime.
// This will cause injection to fail, and the CWNEN0047W message to be issued.

package com.ibm.bnd.lookupoverride.shared;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import com.ibm.bnd.lookupoverride.hidden.MissingClass;

@Stateless
public class Bad6Bean implements Bad {

    @EJB(lookup = "ejblocal:com.ibm.bnd.lookupoverride.shared.TargetBean")
    TargetBean ivTarget6;

    MissingClass ivMissingField;

    // Expected to succeed, regardless of MissingClass not found
    @Override
    public int boing() {

        System.out.println("in com.ibm.bnd.lookupoverride.shared.Bad6Bean.boing(). ivTarget6 = <" + ivTarget6 + ">");
        System.out.println("in com.ibm.bnd.lookupoverride.shared.Bad6Bean.boing() returning 66");
        return 66;

    }

}
