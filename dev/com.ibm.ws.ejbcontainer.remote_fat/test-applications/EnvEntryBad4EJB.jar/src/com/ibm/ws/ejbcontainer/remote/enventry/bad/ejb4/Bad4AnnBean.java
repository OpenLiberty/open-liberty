/*******************************************************************************
 * Copyright (c) 2010, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

// Bean with an env-entry XML injection target referencing an existing non-Enum, non-Class

package com.ibm.ws.ejbcontainer.remote.enventry.bad.ejb4;

import javax.ejb.Stateless;

import com.ibm.ws.ejbcontainer.remote.enventry.shared.Bad;

@Stateless
public class Bad4AnnBean implements Bad {
    // In the <injection-target> of the <env-entry>,
    // "ivEnvEntry_ExistingNonEnumNonClass" is the value of <injection-target-name>
    Class<?> ivEnvEntry_ExistingNonEnumNonClass;

    // Not expected to succeed
    @Override
    public int boing() {
        return 58;
    }
}