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

import java.util.List;

import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.ManyToMany;

/**
 * Assumed from JPA Spec, section 4.4.4
 */
@Embeddable
public class SpecContactInfoOLGH16588 {
    @Embedded
    private SpecAddressOLGH16588 primaryAddress;

    @ElementCollection
    @CollectionTable(name = "PREV_ADDRESSES")
    private List<SpecAddressOLGH16588> previousAddresses;

    @ManyToMany
    private List<SpecPhoneOLGH16588> phones;
}
