/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.commons.logging.test.bundle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DoTest {
    public static void useCommonsLogging() {
        // This test is validating that the Liberty commons.logging works when an application
        // has an embedded version of commons.logging.  Here we are only making sure the Log
        // can get created and that we did not use the context loader to do it.

        // With org.apache.commons.logging.Log.allowFlawedContext=false set in bootstrap.properties
        // this will cause a LogConfigurationException if the thread context loader is used.
        // This causes an error because commons.logging detects that the Liberty context loader
        // does not have the logging impl's class loader in its hierarchy.
        Log log = LogFactory.getLog(DoTest.class);
        log.info("Info test to commons logging.");
    }
}
