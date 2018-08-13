/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.diagnostics.ormparser.entitymapping;

import java.util.List;
import java.util.Set;

public interface IEntityMappings {
    public String getVersion();

    public List<IEntity> _getEntity();

    public List<IEmbeddable> _getEmbeddable();

    public List<IMappedSuperclass> _getMappedSuperclass();

    public IPersistenceUnitMetadata _getPersistenceUnitMetadata();

    public Set<String> _getNamedNativeQueryClasses();

    public Set<String> _getSQLResultSetClasses();

    public Set<String> _getNamedStoredProcedureResultSetClasses();
}
