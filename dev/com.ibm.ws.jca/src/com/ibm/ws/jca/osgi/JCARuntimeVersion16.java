/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
package com.ibm.ws.jca.osgi;

import org.osgi.framework.Version;

import com.ibm.ws.jca.osgi.JCARuntimeVersion;

public class JCARuntimeVersion16 implements JCARuntimeVersion {

    @Override
    public Version getVersion() {
        return VERSION_1_6;
    }

}
