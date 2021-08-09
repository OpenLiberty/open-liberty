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

package com.ibm.ws.jpa.diagnostics.puparser.pu;

import java.util.List;
import java.util.Map;

public interface PUP_PersistenceUnit {
    public String getDescription();
    public String getProvider();
    public String getJtaDataSource();
    public String getNonJtaDataSource();
    public List<String> getMappingFile();
    public List<String> getJarFile();
    public List<String> getClazz();
    public Boolean isExcludeUnlistedClasses();
    public String getName();
       
    public PUP_PersistenceUnitCachingType pup_getSharedCacheMode();
    public PUP_PersistenceUnitValidationModeType pup_getValidationMode();
    public PUP_PersistenceUnitTransactionType pup_getTransactionType();
    public Map<String, String> pup_getProperties();
    
}
