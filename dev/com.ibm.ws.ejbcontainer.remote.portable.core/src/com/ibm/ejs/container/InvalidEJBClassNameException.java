/*******************************************************************************
 * Copyright (c) 1998, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

/**
 * InvalidEJBClassNameException
 * This exception is thrown by the container when an
 * attempt is made to install an EJB by specifiying its
 * attribute list in the form of a Properties object.
 * It indicates that the EJB's class name as specified in
 * the attr list could not be found in the jar file
 * that contains the code for the EJB.
 **/

public class InvalidEJBClassNameException extends ContainerException
{
    private static final long serialVersionUID = 3765545543954619613L;

    public InvalidEJBClassNameException(String s, Throwable ex) {
        super(s, ex);
    }
}
