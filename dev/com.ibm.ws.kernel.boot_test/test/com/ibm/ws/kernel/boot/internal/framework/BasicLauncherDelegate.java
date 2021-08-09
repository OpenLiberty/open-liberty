/*******************************************************************************
 * Copyright (c) 2010, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.internal.framework;

import com.ibm.ws.kernel.boot.BootstrapConfig;
import com.ibm.ws.kernel.launch.internal.LauncherDelegateImpl;

public class BasicLauncherDelegate extends LauncherDelegateImpl {
    public BasicLauncherDelegate(BootstrapConfig config) {
        super(config);
    }

}
