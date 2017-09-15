/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.classloading;

import java.io.Serializable;

/**
 * This interface represents the identity of a classloader. An identity
 * consists of a domain, for example ear, war, osgi and a domain specific identity.
 * This allows two applications with the same identity to not clash.
 * 
 * @see ClassLoadingService#createIdentity(String, String)
 */
public interface ClassLoaderIdentity extends Serializable {
    String getDomain();

    String getId();
}
