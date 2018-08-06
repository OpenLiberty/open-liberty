/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corporation 2011, 2018
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */

package com.ibm.wsspi.anno.util;

import java.util.List;
import java.util.Set;

import com.ibm.wsspi.anno.util.Util_InternMap.ValueType;

public interface Util_Factory {
    String getHashText();

    //

    Set<String> createIdentityStringSet();
    Set<String> createIdentityStringSet(int size);
    Set<String> createIdentityStringSet(Set<String> initialElements);

    //

    Util_InternMap createInternMap(ValueType valueType, String name);
    Util_InternMap createEmptyInternMap(ValueType valueType, String name);

    //

    Util_BidirectionalMap createBidirectionalMap(ValueType holderType, String holderTag,
                                                 ValueType heldType, String heldTag);
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
