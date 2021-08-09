/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.wim.util;

import java.util.ArrayList;
import java.util.Arrays;

public class NodeHelper {
    /**
     * Returns the top level nodes inside the given nodes in lowercase.
     * Top level means these nodes are not contained by other nodes.
     * For example, if there are the following nodes defined:
     * <UL>
     * <LI>dc=yourco,dc=com
     * <LI>cn=users,dc=yourco,dc=com
     * <LI>cn=groups,dc=yourco,dc=com
     * </UL>
     * "dc=yourco,dc=com" is the top level node.
     *
     * @param nodes the nodes
     * @return top level nodes in lower case form.
     */
    public static String[] getTopNodes(String[] nodes) {
        int nodeSize = nodes.length;
        if (nodeSize == 1) {
            return nodes;
        }
        ArrayList<String> nodeList = new ArrayList<String>(nodeSize);
        for (int i = 0; i < nodes.length; i++) {
            String DN = nodes[i];
            // If one of the nodes is empty node, one this empty node is top node
            if (DN.length() == 0) {
                return new String[] {
                                      DN
                };
            }
            // Remove duplicate nodes
            boolean duplicated = false;
            for (int j = 0; j < nodeList.size(); j++) {
                if (DN.equalsIgnoreCase((String) nodeList.get(j))) {
                    duplicated = true;
                }
            }
            if (!duplicated) {
                nodeList.add(DN);
                nodes[i] = DN.toLowerCase();
            }
        }
        Arrays.sort(nodes, new StringLengthComparator());
        ArrayList<String> sortNodeList = new ArrayList<String>(nodeSize);
        for (int i = 0; i < nodes.length; i++) {
            sortNodeList.add(nodes[i]);
        }
        int count = 1;
        int i = sortNodeList.size() - count;
        // Find out super nodes:
        while (i > 0 && i < sortNodeList.size()) {
            String node = (String) sortNodeList.get(i);
            ArrayList<String> removeList = new ArrayList<String>();
            for (int j = 0; j < i; j++) {
                String subNode = (String) sortNodeList.get(j);

                int index = subNode.indexOf(node);
                if (index > -1 && (subNode.length() - index) == node.length()) {
                    removeList.add(subNode);
                }
            }
            for (int j = 0; j < removeList.size(); j++) {
                sortNodeList.remove(removeList.get(j));
            }
            count++;
            i = sortNodeList.size() - count;
        }
        ArrayList<String> resultList = new ArrayList<String>();
        for (int k = 0; k < nodeList.size(); k++) {
            String node = (String) nodeList.get(k);
            if (sortNodeList.contains(node.toLowerCase())) {
                resultList.add(node);
            }
        }
        return (String[]) resultList.toArray(new String[0]);

    }
}
