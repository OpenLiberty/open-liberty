/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.olgh16588.model;

import javax.persistence.Embeddable;

/**
 * Assumed from JPA Spec, section 4.4.4
 */
@Embeddable
public class SpecAddressOLGH16588 {
    private String street;
    private String city;
    private String state;
    private String zipcode;
}
