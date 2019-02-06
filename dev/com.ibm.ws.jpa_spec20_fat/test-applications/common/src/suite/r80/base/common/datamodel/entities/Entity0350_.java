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

import java.math.BigDecimal;
import java.sql.Date;

import javax.persistence.metamodel.SingularAttribute;

@javax.persistence.metamodel.StaticMetamodel(value = suite.r80.base.common.datamodel.entities.Entity0350.class)
public class Entity0350_ {
    public static volatile SingularAttribute<Entity0350, BigDecimal> entity0350_id1;
    public static volatile SingularAttribute<Entity0350, Date> entity0350_id2;
    public static volatile SingularAttribute<Entity0350, Byte> entity0350_id3;
    public static volatile SingularAttribute<Entity0350, String> entity0350_string01;
    public static volatile SingularAttribute<Entity0350, String> entity0350_string02;
    public static volatile SingularAttribute<Entity0350, String> entity0350_string03;
    public static volatile SingularAttribute<Entity0350, Integer> entity0350_version;
}
