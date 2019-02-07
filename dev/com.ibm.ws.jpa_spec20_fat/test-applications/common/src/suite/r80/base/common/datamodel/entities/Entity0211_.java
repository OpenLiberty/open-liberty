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

@javax.persistence.metamodel.StaticMetamodel(value = suite.r80.base.common.datamodel.entities.Entity0211.class)
public class Entity0211_ {
    public static volatile SingularAttribute<Entity0211, Integer> entity0211_id1;
    public static volatile SingularAttribute<Entity0211, Integer> entity0211_id2;
    public static volatile SingularAttribute<Entity0211, Integer> entity0211_id3;
    public static volatile SingularAttribute<Entity0211, String> entity0211_string01;
    public static volatile SingularAttribute<Entity0211, String> entity0211_string02;
    public static volatile SingularAttribute<Entity0211, String> entity0211_string03;
    public static volatile SingularAttribute<Entity0211, Timestamp> entity0211_version;
}
