/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf23.fat.classlevel.bval.beans;

/**
 * The interface that the beans will implement to hold the two passwords.
 * The PasswordValidator uses this as the type for the validatory constraint.
 */
public interface PasswordHolder {

    public String getPassword1();

    public String getPassword2();
}
