/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authentication.principals;

import java.io.Serializable;
import java.security.Identity;

/**
 * Used by EJB container API for getCallerIdentity
 */
@SuppressWarnings("deprecation")
public class WSIdentity extends Identity implements Serializable {

    /**  */
    private static final long serialVersionUID = 6151616852790963219L;

    public WSIdentity(String name) {
        super(name);
    }
}
