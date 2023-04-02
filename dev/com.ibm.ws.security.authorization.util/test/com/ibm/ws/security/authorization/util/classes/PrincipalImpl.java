/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authorization.util.classes;

import java.security.Principal;

public class PrincipalImpl implements Principal {

    private final String name;

    public PrincipalImpl(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

}