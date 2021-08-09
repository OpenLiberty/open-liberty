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

package com.ibm.bnd.lookupoverride.doaApp1.ejb;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import com.ibm.bnd.lookupoverride.shared.Bad;
import com.ibm.bnd.lookupoverride.shared.TargetBean;

@Stateless
public class Bad1Bean implements Bad {

    // Invalid reference, since it specifies both lookup and beanName
    @EJB(lookup = "ejblocal:com.ibm.bnd.lookupoverride.shared.TargetBean", beanName = "fooBean")
    TargetBean ivTarget;

    // Not expected to succeed
    @Override
    public int boing() {
        return 56;
    }
}
