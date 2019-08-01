/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.oauth.core.test.base;

import java.security.Principal;

/*
 * Mock Pricipal class for use in OAuth20Component.processAuthorization()
 */
public class MockPrincipal implements Principal {

    String _name = null;

    public MockPrincipal(String name) {
        _name = name;
    }

    public MockPrincipal() {}

    @Override
    public boolean equals(Object another) {
        if ((another != null) && (another instanceof MockPrincipal)) {
            if ((((MockPrincipal) another).getName() != null) && (((MockPrincipal) another).getName().equals(this.getName()))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public int hashCode() {
        int value = 0;
        if (_name != null) {
            value = _name.hashCode();
        }
        return value;
    }

    @Override
    public String toString() {
        return ("MockPrincipal : " + _name);
    }

    public void setName(String name) {
        _name = name;
    }

}
