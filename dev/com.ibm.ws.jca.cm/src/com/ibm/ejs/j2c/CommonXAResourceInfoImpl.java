/*******************************************************************************
 * Copyright (c) 2001, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.j2c;

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * CommonXAResourceInfoImpl is copied from J2CXAResourceInfo.
 * It contains the elements that are common to both the server and the
 * embeddable EJB container.
 */
public abstract class CommonXAResourceInfoImpl implements CommonXAResourceInfo {
    private static final long serialVersionUID = 6061555722882338966L;

    private static final String NL;

    static {
        NL = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty("line.separator");
            }
        });
    }

    CommonXAResourceInfoImpl() {}

    /**
     * To string. Returns a string representation of J2CXAResourceInfo.
     * 
     * @return a string representation of this class.
     */
    @Override
    public String toString() {
        StringBuilder rval = new StringBuilder(1024);
        rval.append(getClass().getSimpleName()).append(" : ");
        rval.append(NL);
        rval.append("cfName = ");
        rval.append(getCfName());
        rval.append(NL);
        rval.append("cmConfig = ");
        rval.append(getCmConfig());
        rval.append(NL);

        return rval.toString();
    }

    @Override
    public int hashCode() {
        assert false : "hashCode not designed";
        return 117;
    }

}