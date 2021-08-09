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

import com.ibm.ws.repository.resources.ApplicableToProduct;

/**
 * Writable interface to indicate that this resource is something that can hold an applies to field indicating which products it is applicable to.
 */
public interface ApplicableToProductWritable extends ApplicableToProduct {

    /**
     * Sets the applies to field for the resource
     *
     * @param appliesTo the new appliesTo field value
     */
    public void setAppliesTo(String appliesTo);
}
