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
package com.ibm.ws.cdi.internal.interfaces;

import java.util.Set;
import java.util.function.Supplier;

import javax.enterprise.inject.spi.Extension;

public interface ExtensionArchive extends CDIArchive {

    /**
     * @return
     */
    Set<String> getExtraClasses();

    /**
     * @return
     */
    Set<String> getExtraBeanDefiningAnnotations();

    /**
     * @return
     */
    boolean applicationBDAsVisible();

    /**
     * @return
     */
    boolean isExtClassesOnly();

    /**
     * @return a set of suppliers of extension objects
     */
    Set<Supplier<Extension>> getSPIExtensionSuppliers();
}
