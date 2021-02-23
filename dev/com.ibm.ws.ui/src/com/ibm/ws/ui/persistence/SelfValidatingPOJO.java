/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ui.persistence;


/**
 * A SelfValidatingPOJO is defined as a simple POJO (getter/setter) Object
 * which is able to determined whether or not the unmarshalled representation
 * is valid. This is important as an unmarshalled POJO can result in an
 * interpretation that is missing fields.
 */
public interface SelfValidatingPOJO {

    /**
     * Validates the POJO. The definition of a 'valid' POJO depends on the
     * POJO.
     * 
     * @throws InvalidPOJOException If the POJO is not valid
     */
    public void validateSelf() throws InvalidPOJOException;

}
