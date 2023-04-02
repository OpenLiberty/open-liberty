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
package com.ibm.ws.jndi;

import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingException;

public class WSNameParser implements NameParser {

    private final Object root;

    public WSNameParser(Object symbolicRoot) {
        this.root = symbolicRoot; // extra de-reference to force an early NPE
    }

    @Override
    public Name parse(String s) throws NamingException {
        return new WSName(s);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof WSNameParser) {
            WSNameParser that = (WSNameParser) o;
            return this.root == that.root;
            // TODO do we need to do any other kind of comparison?
            // e.g. if this is a CosNaming context, we could check _is_equivalent
        }
        return false;
    }

    @Override
    public int hashCode() {
        return root.hashCode();
    }
}