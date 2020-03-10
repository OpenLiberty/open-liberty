/*******************************************************************************
 * Copyright (c) 2012,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.j2c;

/**
 * Copied to the com.ibm.ws.jca.cm.handle package in order to avoid causing split packages
 * on com.ibm.ej2.j2c with the com.ibm.ws.jca.cm bundle.
 * TODO see if we can switch over usage throughout all other bundles and
 * completely remove com.ibm.ej2.j2c from com.ibm.ejs.j2c from container.service.compat
 */
public interface HandleListInterface extends com.ibm.ws.jca.cm.handle.HandleListInterface {
}
