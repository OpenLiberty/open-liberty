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

import javax.ejb.Local;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;

/**
 * The presence of both the @Local and @LocalBean annotations causes
 * a choice of two local business interfaces to be offered, if installed
 * via the Admin Console.
 */
@Stateless
@Local
@LocalBean
public class SimpleLocalBean implements SimpleLocal {

    @Override
    public int ping() {

        // Caller knows to expect this hard-coded value
        return 55;

    }

}
