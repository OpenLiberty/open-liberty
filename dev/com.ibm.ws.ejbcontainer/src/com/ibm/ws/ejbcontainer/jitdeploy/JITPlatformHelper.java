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

package com.ibm.ws.ejbcontainer.jitdeploy;

import com.ibm.websphere.ras.TrConfigurator;
import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Just In Time Deployment platform specific utility methods. <p>
 * 
 * This is a Liberty platform specific override. <p>
 */
@Trivial
final class JITPlatformHelper {
    /**
     * Returns the path name of the logs directory for the server process. <p>
     * 
     * The platform specific separator character is used and the path does
     * not end with a separator character. <p>
     **/
    static String getLogLocation() {
        return TrConfigurator.getLogLocation();
    }
}
