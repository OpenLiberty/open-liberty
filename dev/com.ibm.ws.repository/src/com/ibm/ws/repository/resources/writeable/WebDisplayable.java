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
package com.ibm.ws.repository.resources.writeable;

import com.ibm.ws.repository.common.enums.DisplayPolicy;

/**
 * Contains options which determine whether the resource should be displayed on a website.
 */
public interface WebDisplayable {

    /**
     * Sets the WebDisplay policy
     *
     * @param policy The {@link DisplayPolicy} to use
     */
    public void setWebDisplayPolicy(DisplayPolicy policy);

    public DisplayPolicy getWebDisplayPolicy();
}
