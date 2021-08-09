/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt.cdi;

import java.lang.annotation.Annotation;

import javax.enterprise.util.AnnotationLiteral;

import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.Claims;

/**
 * ConfigPropertyLiteral represents an instance of the Claim annotation
 */
public class ClaimLiteral extends AnnotationLiteral<Claim> implements Claim {

    /**  */
    private static final long serialVersionUID = 1L;
    public static final Annotation INSTANCE = new ClaimLiteral();

    /** {@inheritDoc} */
    @Override
    public String value() {
        return "";
    }

    /** {@inheritDoc} */
    @Override
    public Claims standard() {
        return Claims.UNKNOWN;
    }

}
