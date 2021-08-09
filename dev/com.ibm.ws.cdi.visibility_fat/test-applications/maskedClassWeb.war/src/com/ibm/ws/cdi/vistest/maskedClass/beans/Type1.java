/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.vistest.maskedClass.beans;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;

/**
 * This class will not be loaded because it is masked by test.Type1 in the EJB jar.
 * <p>
 * The naming of these classes is important so that when CDI tries to validate this BDA, it will look at test.Type1 first.
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
