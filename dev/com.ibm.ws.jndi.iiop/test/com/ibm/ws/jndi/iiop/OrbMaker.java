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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import junit.framework.AssertionFailedError;

import org.apache.yoko.orb.OBPortableServer.POAHelper;
import org.omg.CORBA.ORB;
import org.omg.PortableInterceptor.ORBInitializer;

public class OrbMaker {
    private static final String PREFIX = ORBInitializer.class.getName() + "Class.";
    final List<String> args = new ArrayList<String>();
    final Properties props = new Properties();

    public OrbMaker arg(String arg) {
        args.add(arg);
        return this;
    }

    public OrbMaker prop(String key, String value) {
        props.put(key, value);
        return this;
    }

    public OrbMaker initializer(Class<? extends ORBInitializer> c) {
        return prop(PREFIX + c.getName(), "");
    }

    public ORB makeClient() {
        return ORB.init(args.toArray(new String[args.size()]), props);
    }

    public ORB makeServer() {
        ORB result = makeClient();
        try {
            POAHelper.narrow(result.resolve_initial_references("RootPOA")).the_POAManager().activate();
        } catch (Exception e) {
            e.printStackTrace();
            throw (Error) new AssertionFailedError().initCause(e);
        }
        return result;
    }
}