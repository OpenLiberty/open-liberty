/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jndi.url.contexts.javacolon;

import javax.naming.NamingException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.naming.NamingConstants.JavaColonNamespace;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

/**
 * Helper class to obtain the Component meta data for the thread.
 * This allows all java: helpers to use common code and produce
 * a common message when the thread does not contain the correct
 * context.
 */
public class JavaJNDIComponentMetaDataAccessor {
    private static final TraceComponent tc = Tr.register(JavaJNDIComponentMetaDataAccessor.class);

    /**
     * Helper method to get the component metadata from the thread context.
     * 
     * @return the component metadata data for the thread.
     * @throws NamingException Throws NamingException if running on a non-Java EE thread.
     */
    public static ComponentMetaData getComponentMetaData(JavaColonNamespace namespace, String name) throws NamingException {
        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        if (cmd == null) {
            String fullName = name.isEmpty() ? namespace.toString() : namespace + "/" + name;
            String msg = Tr.formatMessage(tc, "JNDI_NON_JEE_THREAD_CWWKN0100E", fullName);
            NamingException nex = new NamingException(msg);
            throw nex;
        }
        return cmd;
    }
}
