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
