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
package com.ibm.ws.jaxws.metadata;

import java.util.List;

import javax.xml.namespace.QName;

/**
 * This class contains a collection of utility methods used in the various xxxSer classes within this package.
 */
public class Utils {
    /**
     * Compare tow instance. If both are null, still equal
     * 
     * @param t1
     * @param t2
     * @return
     */
    public static <T> boolean compareInstance(T t1, T t2) {
        if (t1 == t2) {
            return true;
        }
        if (t1 != null && t2 != null && t1.equals(t2)) {
            return true;
        }

        return false;
    }

    /**
     * Compares two strings for equality. Either or both of the values may be null.
     * 
     * @param s1
     * @param s2
     * @return true iff the two strings are equal
     */
    public static boolean compareStrings(String s1, String s2) {
        if (s1 == s2)
            return true;
        if (s1 != null && s2 != null && s1.equals(s2))
            return true;
        return false;
    }

    /**
     * Compares two QNames for equality. Either or both of the values may be null.
     * 
     * @param qn1
     * @param qn2
     * @return
     */
    public static boolean compareQNames(QName qn1, QName qn2) {
        if (qn1 == qn2)
            return true;
        if (qn1 == null || qn2 == null)
            return false;
        return qn1.equals(qn2);
    }

    /**
     * Compares two lists of strings for equality.
     * 
     * @param list1
     * @param list2
     * @return true iff each list contains the same strings in the same order
     */
    public static boolean compareStringLists(List<String> list1, List<String> list2) {
        if (list1 == null && list2 == null)
            return true;
        if (list1 == null || list2 == null)
            return false;
        if (list1.size() != list2.size())
            return false;
        for (int i = 0; i < list1.size(); i++) {
            if (!compareStrings(list1.get(i), list2.get(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Compares two lists of QNames for equality.
     * 
     * @param list1
     * @param list2
     * @return true iff each list contains the same QName values in the same order
     */
    public static boolean compareQNameLists(List<QName> list1, List<QName> list2) {
        if (list1 == list2)
            return true;
        if (list1 == null || list2 == null)
            return false;
        if (list1.size() != list2.size())
            return false;

        for (int i = 0; i < list1.size(); i++) {
            if (!compareQNames(list1.get(i), list2.get(i)))
                return false;
        }

        return true;
    }

    /**
     * Compare two lists
     * 
     * @param list1
     * @param list2
     * @return
     */
    public static <T> boolean compareLists(List<T> list1, List<T> list2) {
        if (list1 == list2)
            return true;
        if (list1 == null || list2 == null)
            return false;
        if (list1.size() != list2.size())
            return false;

        for (int i = 0; i < list1.size(); i++) {
            if (!list1.get(i).equals(list2.get(i))) {
                return false;
            }
        }
        return true;
    }
}