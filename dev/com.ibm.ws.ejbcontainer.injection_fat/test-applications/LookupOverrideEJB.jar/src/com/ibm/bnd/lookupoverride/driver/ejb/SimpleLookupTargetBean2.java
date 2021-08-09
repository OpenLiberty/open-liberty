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

package com.ibm.bnd.lookupoverride.driver.ejb;

import javax.ejb.Stateless;

@Stateless
public class SimpleLookupTargetBean2 implements SimpleLookupTarget {

    @Override
    public int pong() {

        // Caller knows to expect this hard-coded value
        return 65;

    }

}
