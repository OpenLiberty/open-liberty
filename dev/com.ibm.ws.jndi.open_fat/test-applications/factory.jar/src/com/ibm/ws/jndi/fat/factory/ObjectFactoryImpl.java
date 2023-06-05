/*
 * =============================================================================
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * =============================================================================
 */
package com.ibm.ws.jndi.fat.factory;

import java.util.Hashtable;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;

public class ObjectFactoryImpl implements ObjectFactory {
    @Override
    public Object getObjectInstance(Object o, Name n, Context c, Hashtable<?, ?> envmt) throws Exception {
        Reference ref = (Reference) o;

        Properties p = new Properties();
        for (int i = 0; i < ref.size(); i++) {
            StringRefAddr sra = (StringRefAddr) ref.get(i);
            p.put(sra.getType(), sra.getContent());
        }

        return p;
    }
}
