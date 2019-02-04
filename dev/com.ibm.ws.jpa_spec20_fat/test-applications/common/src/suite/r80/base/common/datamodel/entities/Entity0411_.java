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

package suite.r80.base.common.datamodel.entities;

import java.sql.Timestamp;

import javax.persistence.metamodel.SingularAttribute;

@javax.persistence.metamodel.StaticMetamodel(value = suite.r80.base.common.datamodel.entities.Entity0411.class)
public class Entity0411_ {
    public static volatile SingularAttribute<Entity0411, Entity0011> entity0411_id;
    public static volatile SingularAttribute<Entity0411, String> entity0411_string01;
    public static volatile SingularAttribute<Entity0411, String> entity0411_string02;
    public static volatile SingularAttribute<Entity0411, String> entity0411_string03;
    public static volatile SingularAttribute<Entity0411, Timestamp> entity0411_version;
}
