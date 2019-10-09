/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.targets.cache;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import com.ibm.ws.annocache.targets.TargetsTableAnnotations;
import com.ibm.ws.annocache.targets.TargetsTableClasses;
import com.ibm.ws.annocache.targets.TargetsTableClassesMulti;
import com.ibm.ws.annocache.targets.TargetsTableContainers;
import com.ibm.ws.annocache.targets.TargetsTableDetails;
import com.ibm.ws.annocache.targets.TargetsTableTimeStamp;
import com.ibm.ws.annocache.util.internal.UtilImpl_InternMap;


public interface TargetCache_Reader {
    List<TargetCache_ParseError> read(TargetsTableContainers containerTable) throws IOException;
    List<TargetCache_ParseError> readResolvedRefs(UtilImpl_InternMap internMap, Set<String> i_resolvedClassNames) throws IOException;
    List<TargetCache_ParseError> readUnresolvedRefs(UtilImpl_InternMap internMap, Set<String> i_unresolvedClassNames) throws IOException;
    List<TargetCache_ParseError> read(TargetsTableTimeStamp stampTable) throws IOException;
    List<TargetCache_ParseError> read(TargetsTableClasses classTable) throws IOException;
    List<TargetCache_ParseError> readMulti(TargetsTableClassesMulti classTable) throws IOException;
    List<TargetCache_ParseError> read(TargetsTableAnnotations targetTable) throws IOException;
    List<TargetCache_ParseError> read(TargetsTableDetails detailTable) throws IOException;

    void close() throws IOException;
}
