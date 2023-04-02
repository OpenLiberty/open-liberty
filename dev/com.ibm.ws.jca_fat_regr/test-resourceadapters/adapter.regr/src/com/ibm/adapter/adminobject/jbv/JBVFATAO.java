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
package com.ibm.adapter.adminobject.jbv;

import javax.validation.constraints.Size;

/**
 * This class is the interface that is implemented by the Administered
 * Objects that are used to test for Java Bean Validation. It is annotated
 * with a JavaBean constraint annotation
 * 
 * @author mageorge
 */
public interface JBVFATAO {

    @Size(min = 5, message = "The minimum value allowed is 5")
    public String getAoProperty1();

}
