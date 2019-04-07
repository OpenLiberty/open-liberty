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

package javaee8.web.jpa;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Version;

@Entity
public class UserEntity {

    @Id
    @GeneratedValue
    public long id;

    @Basic
    public String strData;

    @Version
    public long version;

    public UserEntity() {}

    public UserEntity(String data) {
        strData = data;
    }

    @Override
    public String toString() {
        return "SimpleTestEntity [id=" + id + ", strData=" + strData + ", version=" + version + "]";
    }

}