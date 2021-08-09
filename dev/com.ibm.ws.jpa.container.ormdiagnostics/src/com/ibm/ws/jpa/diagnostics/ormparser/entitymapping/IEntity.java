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

import java.util.Set;

public interface IEntity {
    public String getClazz();

    public String getName();

    public Boolean isMetadataComplete();

    public String _getIDClass();

    public Set<String> _getConverters();

    public Set<String> _getEntityListeners();

    public Set<String> _getNamedEntityGraphClasses();

    public Set<String> _getNamedNativeQueryClasses();

    public Set<String> _getSQLResultSetClasses();
}
