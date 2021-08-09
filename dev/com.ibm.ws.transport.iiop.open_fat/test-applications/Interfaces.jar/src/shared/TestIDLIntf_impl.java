/*
 * =============================================================================
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * =============================================================================
 */

package shared;

public class TestIDLIntf_impl extends TestIDLIntfPOA {
    private String s;

    @Override
    public String s() {
        return s;
    }

    @Override
    public void s(String newS) {
        s = newS;
    }

}
