/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.adapter.spi.jbv;

import javax.validation.constraints.Size;

/**
 * This class is the interface that is implemented by the Managed Connection
 * Factory Objects that are used to test for Java Bean Validation. It is annotated
 * with a JavaBean constraint annotation
 */
public interface JBVFATMCF {

    @Size(min = 4, message = "The minimum value allowed is 4")
    public String getMcfProperty1();

}
