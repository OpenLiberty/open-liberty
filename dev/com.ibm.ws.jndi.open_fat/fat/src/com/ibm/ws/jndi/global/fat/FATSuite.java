/*
 * =============================================================================
 * Copyright (c) 2012, 2023 IBM Corporation and others.
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
package com.ibm.ws.jndi.global.fat;

import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.ShrinkHelper;

import junit.framework.AssertionFailedError;

/**
 * All of the JNDI tests
 */
@RunWith(Suite.class)
@SuiteClasses({
                JNDIGlobalTests.class,
                JNDIGlobalRefTests.class,
                JNDIEntryTests.class,
                JNDIURLEntryTests.class,
                JNDIFeatureTests.class,
                ParentLastJndiTests.class
})
public class FATSuite {
    static final JavaArchive FACTORY_JAR;
    static final WebArchive JNDI_GLOBAL_WAR;
    static final WebArchive PARENT_LAST_WAR;
    static final WebArchive READ_JNDI_ENTRY_WAR;
    static final WebArchive READ_JNDI_URL_ENTRY_WAR;

    static {
        try {
            FACTORY_JAR = ShrinkHelper.buildJavaArchive("factory.jar", "com.ibm.ws.jndi.fat.factory");
            JNDI_GLOBAL_WAR = ShrinkHelper.buildDefaultApp("jndi-global.war", "com.ibm.ws.jndi.global.fat.web");
            PARENT_LAST_WAR = ShrinkHelper.buildDefaultApp("parentLast.war", "com.ibm.ws.jndi.fat.parentlast");
            READ_JNDI_ENTRY_WAR = ShrinkHelper.buildDefaultApp("ReadJndiEntry.war", "com.ibm.ws.jndi.fat.web");
            READ_JNDI_URL_ENTRY_WAR = ShrinkHelper.buildDefaultApp("ReadJndiURLEntry.war", "com.ibm.ws.jndi.fat.web");
        } catch (Exception e) {
            throw (AssertionFailedError) new AssertionFailedError("Could not assemble test application").initCause(e);
        }
    }

}
