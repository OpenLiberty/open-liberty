/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.web;

import jakarta.data.Column;
import jakarta.data.Generated;
import jakarta.data.Id;
import jakarta.data.MappedSuperclass;

/**
 *
 */
@MappedSuperclass
public class Tax {
    @Column("taxId")
    @Generated
    @Id
    public Long key;

    public String leviedBy;

    @Column("taxRate")
    public float rate;
}
