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

import java.math.BigInteger;

import javax.persistence.metamodel.SingularAttribute;

@javax.persistence.metamodel.StaticMetamodel(value = suite.r80.base.common.datamodel.entities.Entity0017.class)
public class Entity0017_ {
    public static volatile SingularAttribute<Entity0017, BigInteger> entity0017_id;
    public static volatile SingularAttribute<Entity0017, String> entity0017_string01;
    public static volatile SingularAttribute<Entity0017, String> entity0017_string02;
    public static volatile SingularAttribute<Entity0017, String> entity0017_string03;
    public static volatile SingularAttribute<Entity0017, Long> entity0017_version;
}
