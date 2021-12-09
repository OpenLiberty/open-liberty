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

import javax.ejb.Stateless;

@Stateless
public class Bad3Bean implements Bad {

    // ejb-jar has an invalid combination of references, specifying 
    // both a <lookup-name> and an <ejb-link> on the same <ejb-local-ref>

    // Not expected to succeed
    @Override
    public int boing() {

        return 58;

    }

}
