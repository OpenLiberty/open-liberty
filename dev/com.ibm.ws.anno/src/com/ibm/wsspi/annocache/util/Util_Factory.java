/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.wsspi.annocache.util;

import java.util.List;
import java.util.Set;

import com.ibm.wsspi.anno.util.Util_BidirectionalMap;
import com.ibm.wsspi.anno.util.Util_InternMap.ValueType;

public interface Util_Factory extends com.ibm.wsspi.anno.util.Util_Factory {

	@Override
    String getHashText();

	@Override
    Set<String> createIdentityStringSet();

	@Override
    Util_InternMap createInternMap(ValueType valueType, String name);

	@Override
    Util_BidirectionalMap createBidirectionalMap(ValueType holderType, String holderTag,
                                                 ValueType heldType, String heldTag);

	@Override
    Util_BidirectionalMap createBidirectionalMap(ValueType holderType, String holderTag,
                                                 ValueType heldType, String heldTag,
                                                 boolean isEnabled);

    //

    Set<String> createIdentityStringSet(int size);
	Set<String> createIdentityStringSet(Set<String> initialElements);

    Util_InternMap createEmptyInternMap(ValueType valueType, String name);

    Util_BidirectionalMap createEmptyBidirectionalMap(ValueType holderType, String holderTag,
                                                      ValueType heldType, String heldTag);

    //

    String NORMALIZED_SEP = "/";
    char NORMALIZED_SEP_CHAR = '/';

    String normalize(String path);
    String denormalize(String n_path);

    String append(String headPath, String tailPath);
    String n_append(String n_headPath, String n_tailPath);

    String subtractPath(String basePath, String fullPath);
    String n_subtractPath(String n_basePath, String n_fullPath);

    Util_RelativePath addRelativePath(String basePath, String relativePath);
    Util_RelativePath n_addRelativePath(String n_basePath, String n_relativePath);
    Util_RelativePath subtractRelativePath(String basePath, String fullPath);
    Util_RelativePath n_subtractRelativePath(String n_basePath, String n_fullPath);

    Util_RelativePath createRelativePath(String basePath, String relativePath, String fullPath);
    Util_RelativePath n_createRelativePath(String n_basePath, String n_relativePath, String n_fullPath);

    List<? extends Util_RelativePath> selectJars(String basePath);
}
