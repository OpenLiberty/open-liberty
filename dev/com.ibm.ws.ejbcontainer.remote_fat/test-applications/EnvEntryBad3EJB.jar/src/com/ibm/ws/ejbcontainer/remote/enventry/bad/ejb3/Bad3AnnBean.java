/*******************************************************************************
 * Copyright (c) 2010, 2018 IBM Corporation and others.
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

// Bean with an invalid @EJB reference

package com.ibm.ws.ejbcontainer.remote.enventry.bad.ejb3;

import javax.ejb.Stateless;

import com.ibm.ws.ejbcontainer.remote.enventry.shared.Bad;

@Stateless
public class Bad3AnnBean implements Bad {
    // In the <injection-target> of the <env-entry>,
    // "ivEnvEntry_NoSuchEnumValue" is the value of <injection-target-name>
    Class<?> ivEnvEntry_NoSuchEnumValue;

    // Not expected to succeed
    @Override
    public int boing() {
        return 58;
    }
}