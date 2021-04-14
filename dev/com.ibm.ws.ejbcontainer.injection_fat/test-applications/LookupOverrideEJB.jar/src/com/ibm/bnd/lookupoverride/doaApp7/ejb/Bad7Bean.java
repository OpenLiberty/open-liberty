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

// Bean with a lookup annotation in conjunction with <ejb-link> in ejb-jar.xml.

package com.ibm.bnd.lookupoverride.doaApp7.ejb;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import com.ibm.bnd.lookupoverride.shared.Bad;
import com.ibm.bnd.lookupoverride.shared.TargetBean;

@Stateless
public class Bad7Bean implements Bad {

    @EJB(name = "SLT9", lookup = "ejblocal:SLT")
    TargetBean ivTarget7;

    // Not expected to succeed.
    @Override
    public int boing() {

        System.out.println("in com.ibm.bnd.lookupoverride.shared.Bad6Bean.boing(). ivTarget7 = <" + ivTarget7 + ">");
        System.out.println("in com.ibm.bnd.lookupoverride.shared.Bad6Bean.boing() returning 77");
        return 77;

    }

}
