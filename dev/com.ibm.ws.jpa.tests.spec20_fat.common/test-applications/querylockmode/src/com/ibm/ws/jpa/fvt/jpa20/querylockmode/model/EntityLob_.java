/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.jpa20.querylockmode.model;

import java.io.Serializable;
import java.util.List;

import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.SingularAttribute;

@javax.persistence.metamodel.StaticMetamodel(value = com.ibm.ws.jpa.fvt.jpa20.querylockmode.model.EntityLob.class)
public class EntityLob_ {
    public static volatile SingularAttribute<EntityLob, Integer> entityLob_id;
    public static volatile SingularAttribute<EntityLob, Byte[]> entityLob_lob01;
    public static volatile SingularAttribute<EntityLob, byte[]> entityLob_lob02;
    public static volatile SingularAttribute<EntityLob, String> entityLob_lob03;
    public static volatile SingularAttribute<EntityLob, String[]> entityLob_lob04;
    public static volatile SingularAttribute<EntityLob, Object> entityLob_lob05;
    public static volatile SingularAttribute<EntityLob, Object[]> entityLob_lob06;
    public static volatile ListAttribute<EntityLob, String> entityLob_lob07;
    public static volatile SingularAttribute<EntityLob, List[]> entityLob_lob08;
    public static volatile SingularAttribute<EntityLob, char[]> entityLob_lob09;
    public static volatile SingularAttribute<EntityLob, Character[]> entityLob_lob10;
    public static volatile SingularAttribute<EntityLob, Serializable> entityLob_lob11;
    public static volatile SingularAttribute<EntityLob, Serializable[]> entityLob_lob12;
    public static volatile SingularAttribute<EntityLob, Integer> entityLob_version;
}
