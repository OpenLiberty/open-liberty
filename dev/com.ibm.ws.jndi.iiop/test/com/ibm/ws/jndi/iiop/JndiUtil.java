/* ***************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * ***************************************************************************/

package com.ibm.ws.jndi.iiop;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.Properties;

import javax.naming.CompoundName;
import javax.naming.Context;
import javax.rmi.PortableRemoteObject;

public enum JndiUtil {
    USE_STRING_NAMES {
        @Override
        public Object lookup(Context ctx, String name) throws Exception {
            return ctx.lookup(name);
        }

        @Override
        public void bind(Context ctx, JndiBindOperation bindOp, String name, Object obj) throws Exception {
            bindOp.bind(ctx, name, obj);
        }
    },
    USE_COMPOUND_NAMES {
        @Override
        public Object lookup(Context ctx, String name) throws Exception {
            return ctx.lookup(compound(name));
        }

        @Override
        public void bind(Context ctx, JndiBindOperation bindOp, String name, Object obj) throws Exception {
            bindOp.bind(ctx, compound(name), obj);
        }
    };

    @SuppressWarnings("serial")
    static Properties PROPS = new Properties() {
        {
            put("jndi.syntax.direction", "left_to_right");
            put("jndi.syntax.separator", "/");
        }
    };

    static CompoundName compound(String s) throws Exception {
        return new CompoundName(s, JndiUtil.PROPS);
    }

    public abstract Object lookup(Context ctx, String name) throws Exception;

    public Testable lookupObject(Context ctx, String name) throws Exception {
        Testable stub = lookupTestable(ctx, name);
        final String id = name.replaceFirst(".*/", "");
        assertEquals(id, stub.getName());
        return stub;
    }

    private Testable lookupTestable(Context ctx, String name) throws Exception {
        final Object obj = lookup(ctx, name);
        assertThat(obj, instanceOf(org.omg.CORBA.Object.class));
        Testable stub = (Testable) PortableRemoteObject.narrow(obj, Testable.class);
        return stub;
    }

    public Context lookupContext(Context ctx, String name) throws Exception {
        final Object obj = lookup(ctx, name);
        assertThat(obj, instanceOf(Context.class));
        return (Context) obj;
    }

    public abstract void bind(Context ctx, JndiBindOperation bindOp, String name, Object obj) throws Exception;

    public void bindObject(Context ctx, JndiBindOperation bindOp, String name, Testable obj) throws Exception {
        bind(ctx, bindOp, name, obj);
        final Testable actual = lookupTestable(ctx, name);
        assertEquals(obj.getName(), actual.getName());
    }
}
