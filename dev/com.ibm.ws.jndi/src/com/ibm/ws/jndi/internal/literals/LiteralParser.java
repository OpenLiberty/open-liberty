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
package com.ibm.ws.jndi.internal.literals;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public enum LiteralParser {
    ;
    private static final TraceComponent tc = Tr.register(LiteralParser.class);

    public static Object parse(String s) {
        for (LiteralType type : LiteralType.values())
            if (type.matches(s))
                return type.parse(s);
        if (tc.isDebugEnabled())
            Tr.debug(tc, "String did not match any known types", s);
        return s;
    }
}
