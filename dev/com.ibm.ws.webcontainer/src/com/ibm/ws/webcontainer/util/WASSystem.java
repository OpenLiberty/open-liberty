/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.util;

import java.io.InputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.Properties;

import com.ibm.ejs.ras.TraceNLS;


public class WASSystem {
    private static String PROPERTIES_PATH = "/com/ibm/ws/webcontainer/appserver.properties";
    private static Properties _props;

    private static TraceNLS nls = TraceNLS.getTraceNLS(WASSystem.class, "com.ibm.ws.webcontainer.resources.Messages");
    static{
        try {
            /*
             ** read the properties as a resource
             */
            _props = new Properties();
            InputStream in = getResourceAsStream(PROPERTIES_PATH);
            _props.load(in);
        }
        catch(Throwable th) {
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(th, "com.ibm.ws.webcontainer.util.WASSystem", "39");
        }
    }

    //no instance of this can be created.
    private WASSystem() {
    }

    @SuppressWarnings("unchecked")
    public static Enumeration getPropertyNames() {
        return _props.propertyNames();
    }

    public static String getProperty(String name) {
        return _props.getProperty(name);
    }

    public static URL getResource(String path) {
        return WASSystem.class.getResource(path);
    }

    public static InputStream getResourceAsStream(String path) {
        return WASSystem.class.getResourceAsStream(path);
    }

    @SuppressWarnings("unchecked")
    public static Object createObject(String classname) {
        try {
            Class aClass = Class.forName(classname);
            return(Object)aClass.newInstance();
        }
        catch( Throwable th ) {
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(th, "com.ibm.ws.webcontainer.util.WASSystem.createObject", "71");
            throw new IllegalStateException(MessageFormat.format(nls.getString("{0}.is.not.a.valid.class","{0} is not a valid class"), new Object[]{classname}));
        }
    }
}
