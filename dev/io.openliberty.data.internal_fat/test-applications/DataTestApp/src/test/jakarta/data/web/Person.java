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
package test.jakarta.data.web;

import jakarta.data.Column;
import jakarta.data.Entity;
import jakarta.data.Id;

/**
 *
 */
@Entity
public class Person {

    public String firstName;

    @Column("SURNAME")
    public String lastName;

    @Id("SSNUM")
    public long ssn;
}
