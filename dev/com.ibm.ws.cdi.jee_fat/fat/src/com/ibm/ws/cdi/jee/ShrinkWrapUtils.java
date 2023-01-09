/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.ws.cdi.jee;

import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 *
 */
public class ShrinkWrapUtils {

    /**
     * Add a resource to the root of a WebArchive, using the provided relative path.
     * The path is both relative to the source package AND relative to the root of
     * the target archive.
     *
     * @param  archive      The WebArchive to add to
     * @param  pkg          The source package
     * @param  resourceName The relative path of the resource.
     * @return              The updated WebArchive
     */
    public static WebArchive addAsRootResource(WebArchive archive, Package pkg, String resourceName) {
        return archive.addAsWebResource(pkg, resourceName, resourceName);
    }

}
