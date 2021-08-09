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

public interface IEntityMappings {
    public String getVersion();

    public List<IEntity> getEntityList();

    public List<IEmbeddable> getEmbeddableList();

    public List<IMappedSuperclass> getMappedSuperclassList();

    public IPersistenceUnitMetadata getIPersistenceUnitMetadata();
}
