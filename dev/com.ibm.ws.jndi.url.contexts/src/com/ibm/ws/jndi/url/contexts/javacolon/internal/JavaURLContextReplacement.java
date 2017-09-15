/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jndi.url.contexts.javacolon.internal;

import java.io.Serializable;
import java.util.Hashtable;

/**
 * This class is used during SSFB passivation/activation as a replacement object
 * (to be used instead of the javax.naming.Context during serialization) since
 * Liberty's implementation of javax.naming.Context is not serializable. This works
 * by storing the env map and the JNDI name, so that the context can be looked up
 * again on deserialization (activation).
 */
public class JavaURLContextReplacement implements Serializable {
    private static final long serialVersionUID = 4440131108499555728L;

    private String base;
    private Hashtable<?, ?> env;

    JavaURLContextReplacement() {}

    void setBase(String base) {
        this.base = base;
    }

    String getBase() {
        return base;
    }

    void setEnv(Hashtable<?, ?> env) {
        this.env = env;
    }

    Hashtable<?, ?> getEnv() {
        return env;
    }
}
