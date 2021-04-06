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

package com.ibm.bnd.lookupoverride.shared;

import javax.ejb.Stateless;

//By not implementing an interface, SimpleNIVbean is a Business Interface.
//(Alternatively, the @LocalBean annotation could have been added, keeping the "implements")
@Stateless
public class SimpleNIVbean {

    public int ping() {

        // Caller knows to expect this hard-coded value
        return 56;

    }

}
