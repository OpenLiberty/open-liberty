/* ***************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * ***************************************************************************/
package com.ibm.ws.jndi.iiop;

import javax.naming.CompoundName;
import javax.naming.Context;

public enum JndiBindOperation {
    BIND {
        @Override
        void bind(Context ctx, String name, Object obj) throws Exception {
            ctx.bind(name, obj);
        }

        @Override
        void bind(Context ctx, CompoundName name, Object obj) throws Exception {
            ctx.bind(name, obj);
        }
    },
    REBIND {
        @Override
        void bind(Context ctx, String name, Object obj) throws Exception {
            ctx.rebind(name, obj);
        }

        @Override
        void bind(Context ctx, CompoundName name, Object obj) throws Exception {
            ctx.rebind(name, obj);
        }
    };

    abstract void bind(Context ctx, String name, Object obj) throws Exception;

    abstract void bind(Context ctx, CompoundName compoundName, Object obj) throws Exception;

    public boolean canOverWrite() {
        return this == REBIND;
    }
}
