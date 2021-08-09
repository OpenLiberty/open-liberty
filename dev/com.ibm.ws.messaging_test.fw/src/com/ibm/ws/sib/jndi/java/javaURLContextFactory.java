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
package com.ibm.ws.sib.jndi.java;

import java.util.Hashtable;

import javax.naming.ConfigurationException;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.spi.ObjectFactory;

/* ************************************************************************** */
/**
 * A Summary of a goes here. Followed by a paragraph of
 * general description.
 * 
 */
/* ************************************************************************** */
public class javaURLContextFactory implements ObjectFactory {

    /* -------------------------------------------------------------------------- */
    /*
     * getObjectInstance method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see javax.naming.spi.ObjectFactory#getObjectInstance(java.lang.Object, javax.naming.Name, javax.naming.Context, java.util.Hashtable)
     * @param urlInfo
     * @param name
     * @param nameCtx
     * @param environment
     * @return
     * @throws java.lang.Exception
     */
    public Object getObjectInstance(Object urlInfo, Name name, Context nameCtx, Hashtable environment) throws Exception {
        if (urlInfo == null) {
            return new javaURLContext(environment);
        }
        if (urlInfo instanceof String) {
            String url = (String) urlInfo;
            Context context = new javaURLContext(environment);
            try {
                return context.lookup(url);
            } finally {
                context.close();
            }
        }
        if (urlInfo instanceof String[]) {
            // Try each URL until either lookup succeeds or they all fail
            String[] urls = (String[]) urlInfo;
            if (urls.length == 0)
                throw new ConfigurationException("javaURLContextFactory: empty URL array");
            Context context = new javaURLContext(environment);
            try {
                NamingException ne = null;
                for (int i = 0; i < urls.length; i++) {
                    try {
                        return context.lookup(urls[i]);
                    } catch (NamingException e) {
                        ne = e;
                    }
                }
                throw ne;
            } finally {
                context.close();
            }
        }
        throw new ConfigurationException("javaURLContextFactory: cannot understand urlInfo:" + urlInfo);
    }
}
