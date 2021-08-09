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
package com.ibm.ws.jmx.connector.client.rest.internal;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
public class Activator {

    private final static String PKGS_KEY = "jmx.remote.protocol.provider.pkgs";
    private final static String PKGS = "com.ibm.ws.jmx.connector.client";
    private final static Pattern P = Pattern.compile("(?:\\A|,)" + PKGS + "(?:\\z|,)");

    @Activate
    protected void activate() {

        String jmx = System.getProperty(PKGS_KEY);
        System.setProperty(PKGS_KEY, add(jmx));
    }

    @Deactivate
    protected void deactivate() {
        String jmx = System.getProperty(PKGS_KEY);
        if (PKGS.equals(jmx)) {
            System.clearProperty(PKGS_KEY);
        } else {
            System.setProperty(PKGS_KEY, remove(jmx));
        }
    }

    static String add(String jmx) {
        if (jmx == null) {
            return PKGS;
        } else {
            return jmx + "," + PKGS;
        }
    }

    static String remove(String jmx) {
        Matcher m = P.matcher(jmx);
        return m.replaceAll("");
    }

}
