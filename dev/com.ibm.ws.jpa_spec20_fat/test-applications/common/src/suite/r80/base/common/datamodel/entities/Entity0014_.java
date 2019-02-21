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

@javax.persistence.metamodel.StaticMetamodel(value = suite.r80.base.common.datamodel.entities.Entity0014.class)
public class Entity0014_ {
    public static volatile SingularAttribute<Entity0014, Short> entity0014_id;
    public static volatile SingularAttribute<Entity0014, String> entity0014_string01;
    public static volatile SingularAttribute<Entity0014, String> entity0014_string02;
    public static volatile SingularAttribute<Entity0014, String> entity0014_string03;
    public static volatile SingularAttribute<Entity0014, Timestamp> entity0014_version;
}
