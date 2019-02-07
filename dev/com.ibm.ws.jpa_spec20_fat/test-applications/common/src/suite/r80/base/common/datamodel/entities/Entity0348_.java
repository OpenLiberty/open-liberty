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
import java.util.Date;

import javax.persistence.metamodel.SingularAttribute;

@javax.persistence.metamodel.StaticMetamodel(value = suite.r80.base.common.datamodel.entities.Entity0348.class)
public class Entity0348_ {
    public static volatile SingularAttribute<Entity0348, Long> entity0348_id1;
    public static volatile SingularAttribute<Entity0348, Short> entity0348_id2;
    public static volatile SingularAttribute<Entity0348, Date> entity0348_id3;
    public static volatile SingularAttribute<Entity0348, String> entity0348_string01;
    public static volatile SingularAttribute<Entity0348, String> entity0348_string02;
    public static volatile SingularAttribute<Entity0348, String> entity0348_string03;
    public static volatile SingularAttribute<Entity0348, Timestamp> entity0348_version;
}
