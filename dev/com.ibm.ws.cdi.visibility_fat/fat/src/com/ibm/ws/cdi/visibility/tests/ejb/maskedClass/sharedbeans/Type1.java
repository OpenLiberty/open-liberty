/*******************************************************************************
 * Copyright (c) 2015, 2022 IBM Corporation and others.
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
package com.ibm.ws.cdi.visibility.tests.ejb.maskedClass.sharedbeans;

import java.io.File;

import javax.enterprise.context.ApplicationScoped;

/**
 * This class will be included in both the war and the ejb jar.
 * <p>
 * The copy in the war should be masked by the copy in the ejb jar.
 */
@ApplicationScoped
public class Type1 {

    public String getMessage() {
        try {
            final File f = new File(getClass().getProtectionDomain().getCodeSource().getLocation().getPath());
            if (f.getCanonicalPath().contains("maskedClassWeb.war")) {
                return "from web";
            } else if (f.getCanonicalPath().contains("maskedClassEjb.jar")) {
                return "from ejb";
            }
            return "unkown " + f.getCanonicalPath();
        } catch (Exception e) {
            return e.getMessage();
        }
    }
}
