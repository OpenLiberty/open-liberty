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

import javax.ejb.LocalBean;
import javax.ejb.Stateless;

/**
 * The @LocalBean annotation "cancels out" the "implements" in
 * regard to the business local interface. That is, there should
 * be only one business interface, and that should be SimpleMultiBizBean
 *
 */

@Stateless
@LocalBean
public class SimpleMultiBizBean implements SimpleMultiBiz {

    @Override
    public int ping() {

        // Caller knows to expect this hard-coded value
        return 57;

    }

}
