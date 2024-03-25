/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.test.tld;

import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;

import com.ibm.wsspi.jsp.taglib.config.GlobalTagLibConfig;
import com.ibm.wsspi.jsp.taglib.config.TldPathConfig;

public class TestGlobalConfig extends GlobalTagLibConfig {
    public TestGlobalConfig() {
        super();

        setJarName("test-tld.jar");

        // NEW CONSTRUCTOR
        addtoTldPathList(new TldPathConfig("WEB-INF/tld/test1.tld", "/WEB-INF/tld/test1.tld", false));

        // OLD CONSTRUCTOR
        addtoTldPathList(new TldPathConfig("WEB-INF/tld/test1.tld", "/WEB-INF/tld/test1.tld", null));

        setClassloader(AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            public ClassLoader run() {
                return TestGlobalConfig.class.getClassLoader();
            }
        }));

        setJarURL(AccessController.doPrivileged(new PrivilegedAction<URL>() {
            public URL run() {
                return getClassloader().getResource("WEB-INF/tld/test1.tld");
            }
        }));
    }

    @SuppressWarnings("unchecked")
    private void addtoTldPathList(TldPathConfig tldPathConfig) {
        getTldPathList().add(tldPathConfig);
    }
}
