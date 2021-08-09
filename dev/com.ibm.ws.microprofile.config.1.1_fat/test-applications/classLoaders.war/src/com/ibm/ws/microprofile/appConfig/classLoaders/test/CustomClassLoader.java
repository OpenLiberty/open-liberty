/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.appConfig.classLoaders.test;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

public class CustomClassLoader extends ClassLoader {

    private final URL customResource;

    public CustomClassLoader() {
        super(Thread.currentThread().getContextClassLoader());
        customResource = super.getResource("CUSTOM-DIR/META-INF/microprofile-config.properties");
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {

        Enumeration<URL> pr = super.getResources(name);
        if (name.equals("META-INF/microprofile-config.properties")) {
            pr = new CustomEnum(pr, customResource);
        }

        return pr;
    }

    class CustomEnum implements Enumeration<URL> {

        private final Enumeration<URL> pr;
        private boolean customElement = false;
        private final URL customResource;

        public CustomEnum(Enumeration<URL> pr, URL customResource) {
            this.pr = pr;
            this.customResource = customResource;
            customElement = (customResource != null);
        }

        /** {@inheritDoc} */
        @Override
        public boolean hasMoreElements() {
            if (customElement)
                return true;
            return pr.hasMoreElements();
        }

        /** {@inheritDoc} */
        @Override
        public URL nextElement() {
            if (customElement) {
                customElement = false;
                return customResource;
            }
            return pr.nextElement();
        }

    }

}