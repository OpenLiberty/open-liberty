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
package com.ibm.ws.kernel.boot.app.classpath;

import java.lang.reflect.Method;

import javax.annotation.PostConstruct;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.Startup;

@Startup
@Singleton
@LocalBean
public class CheckJvmAppClasspathPackagesBean {

    private static ClassLoader JVM_APP_LOADER = ClassLoader.getSystemClassLoader();

    @PostConstruct
    public void printJvmAppClasspathPackages() {
        Method m;
        try {
            m = ClassLoader.class.getDeclaredMethod("getPackages");
            m.setAccessible(true);
            Package[] pkgs = (Package[]) m.invoke(JVM_APP_LOADER);
            for (Package p : pkgs) {
                System.out.println("AppLoader can load: " + p.getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
