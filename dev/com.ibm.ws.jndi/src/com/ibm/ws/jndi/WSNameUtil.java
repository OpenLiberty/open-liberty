/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jndi;

import javax.naming.InvalidNameException;
import javax.naming.Name;

public enum WSNameUtil {
    ;
    public static WSName normalize(Name name) throws InvalidNameException {
        if (name == null || name.isEmpty())
            return WSName.EMPTY_NAME;
        return name instanceof WSName ? (WSName) name : new WSName(name);
    }

    public static WSName normalize(String name) throws InvalidNameException {
        if (name == null || name.isEmpty())
            return WSName.EMPTY_NAME;
        return new WSName(name);
    }

    public static WSName copy(Name n) throws InvalidNameException {
        return (n instanceof WSName) ? (WSName) n.clone() : new WSName(n);
    }

    public static WSName copy(String s) throws InvalidNameException {
        return normalize(s);
    }

    public static WSName compose(Name prefix, Name suffix) throws InvalidNameException {
        return copy(prefix).addAll(suffix);
    }

    public static String compose(String prefix, String suffix) throws InvalidNameException {
        return copy(prefix).addAll(normalize(suffix)).toString();
    }
}
