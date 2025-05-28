/*******************************************************************************
 * Copyright (c) 2014, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.targets.cache;

import java.io.IOException;
import java.util.List;

public interface TargetCache_Readable {
    /**
     * API For adding type information to a call to reader.
     * 
     * The reader has a number of type specific read methods.  Implementations of
     * {@link #readUsing(TargetCache_Reader)} invoke a read method using the concrete
     * type of the {@link TargetCache_Readable} which is doing the read.
     * 
     * See {@link TargetCacheImpl_DataBase#read(File, TargetCache_Readable...)}.
     * See also, for example, {TargetsTableAnnotationsImpl#readUsing(TargetCache_Reader)}.
     * 
     * 
     * @param reader A cache reader.
     * 
     * @return A list of parse errors.  Empty if parsing had no errors.
     * 
     * @throws IOException Thrown if there was a basic IO failure during the read.
     */
    List<TargetCache_ParseError> readUsing(TargetCache_Reader reader) throws IOException;
}
