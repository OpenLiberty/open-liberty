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

// Bean with an invalid @EJB reference

package com.ibm.ws.ejbcontainer.remote.enventry.bad.ejb2;

import javax.ejb.Stateless;

import com.ibm.ws.ejbcontainer.remote.enventry.shared.Bad;

@Stateless
public class Bad2XmlBean implements Bad {
    // In the <injection-target> of the <env-entry>,
    // "ivEnvEntry_NoSuchEnumType" is the value of <injection-target-name>
    Class<?> ivEnvEntry_NoSuchEnumType;

    // Not expected to succeed
    @Override
    public int boing() {
        return 57;
    }
}