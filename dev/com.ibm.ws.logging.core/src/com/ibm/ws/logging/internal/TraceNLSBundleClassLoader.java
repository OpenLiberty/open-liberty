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
package com.ibm.ws.logging.internal;

import java.net.URL;

/**
 * The BaseResourceBundleClassLoader allows us to insert ourselves into the
 * ResourceBundle.getBundle bundle-loading strategy in order to force the
 * loading of the base ResourceBundle (i.e. the ResourceBundle with NO locale
 * suffix (_de,_fr,etc).
 * 
 * This allows us to load the ENGLISH version of the ResourceBundle when we
 * are running in a non-ENGLISH locale. The ENGLISH messages are in the base
 * ResourceBundle. We don't actually ship a separate english (_en) version.
 * This prevents ResourceBundle.getBundle from ever loading the ENGLISH messages
 * from a non-ENGLISH default locale, due to its bundle-loading strategy, which
 * works as follows:
 * 1) load the specified bundle (_en)
 * 2) if no _en, load the default locale bundle.
 * 3) if no default locale bundle, load the base bundle.
 * We don't ship _en, so #1 fails. Presumably #2 will succeed (if running in
 * any locale that we ship a ResourceBundle for). Thus, #3 is never reached,
 * and so the ENGLISH bundle is never loaded.
 * 
 * So in order to workaround this problem and get the ENGLISH ResourceBundle
 * to load, we create this BaseResourceBundleClassLoader and pass it to
 * ResourceBundle.getBundle. BaseResourceBundleClassLoader gets control for
 * every class getBundle attempts to load. We fail the load for any class
 * name that contains a language suffix, which forces getBundle to try option
 * #3, load the base bundle, which we allow to pass.
 * 
 * Now you may be asking, "why didn't you just return the base ResourceBundle
 * when getBundle asked for the _en bundle?" Well, that doesn't work, because
 * apparently Class.forNameImpl detects when the loaded class doesn't match the
 * class name that was passed in, and it fails. So the only way to get this to
 * work is just fail the class loading until it tries to load the class we want
 * it to load.
 */
public class TraceNLSBundleClassLoader extends ClassLoader {

    private String bundleName = null;

    /**
     * CTOR takes the ResourceBundle's ClassLoader (i.e. the ClassLoader associated
     * with the class that logged the message.
     * 
     * @param parent The parent ClassLoader (used for loading the ResourceBundle).
     */
    public TraceNLSBundleClassLoader(ClassLoader parent, String bundleName) {
        super(parent);
        this.bundleName = bundleName;
    }

    /**
     * findClass is called only if loadClass fails. If loadClass failed, then we must
     * not be trying to load the base bundle. So return null.
     * 
     * @param name The class name.
     * 
     * @return null, always.
     */
    @Override
    protected Class<?> findClass(String name) {
        return null;
    }

    /**
     * Load the class only if its name matches the ResourceBundle name associated with
     * this WsLogRecord (which is the base ResourceBundle name - no language suffix).
     * 
     * @param The class name.
     * 
     * @return The class, if the base ResourceBundle. Otherwise, null.
     */
    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        if (name.equals(bundleName)) {
            // We're loading the base bundle. Good! Let it load.
            return super.loadClass(name);
        } else {
            // We're NOT loading the base bundle, so return null.
            return null;
        }
    }

    /**
     * Load the resource only if its name matches the ResourceBundle name associated with
     * this WsLogRecord (which is the base ResourceBundle name - no language suffix).
     * 
     * @param The resource name.
     * 
     * @return The resource, if the base ResourceBundle resource. Otherwise, null.
     */
    @Override
    public URL getResource(String name) {
        if (name.replace('/', '.').replace(".properties", "").equals(bundleName)) {
            // We're loading the base properties file. Let it load.
            return super.getResource(name);
        } else {
            // We're NOT loading the base properties file, so return null.
            return null;
        }
    }
};
