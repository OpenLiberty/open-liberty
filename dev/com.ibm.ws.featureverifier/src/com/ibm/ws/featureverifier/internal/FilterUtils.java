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
package com.ibm.ws.featureverifier.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ibm.ws.featureverifier.internal.FilterUtils.FilterOperation.NodeOperation;
import com.ibm.ws.featureverifier.internal.XmlErrorCollator.ReportType;

public class FilterUtils {

    public static class FilterNode {}

    public static class FilterItem extends FilterNode {
        String attribName;
        String attribValue;
    }

    public static class FilterOperation extends FilterNode {
        enum NodeOperation {
            AND, OR, NOT
        };

        NodeOperation op;
        List<FilterNode> item;
    }

    public static class ErrorInfo {
        public final XmlErrorCollator.ReportType type;
        public final String shortText;
        public final String summary;
        public final String detail;

        public ErrorInfo(ReportType t, String st, String s, String d) {
            type = t;
            shortText = st;
            summary = s;
            detail = d;
        }
    }

    public static class ParseError extends Exception {
        private static final long serialVersionUID = 1L;
        final ErrorInfo[] eis;

        public ParseError(ErrorInfo... ei) {
            eis = ei;
        }

        public ErrorInfo[] getErrorInfo() {
            return eis;
        }

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer();
            for (ErrorInfo ei : eis) {
                sb.append(ei.shortText + " " + ei.detail + "\n");
            }
            return sb.toString();
        }
    }

    private static FilterNode parseFilterString(String filter) throws ParseError {
        if (filter.startsWith("(") && filter.endsWith(")")) {
            FilterNode result;
            filter = filter.substring(1, filter.length() - 1);
            char initialChar = filter.charAt(0);
            if (initialChar == '!') {
                throw new ParseError(new ErrorInfo(ReportType.ERROR, "[BAD_LDAP_FILTER '" + filter + "' ]", "Checker unable to parse filter fragment " + filter, "Checker was unable to parse the filter fragment as it makes use of ! when ! is not supported for use in the IBM-Provision-Capability header"));
            }
            if (initialChar == '&' || initialChar == '|') {
                FilterOperation fo = new FilterOperation();
                if (initialChar == '&')
                    fo.op = NodeOperation.AND;
                else
                    fo.op = NodeOperation.OR;
                filter = filter.substring(1);
                //we now have (xx) or (xx)(yy)(zz) etc.
                fo.item = new ArrayList<FilterNode>();
                int start = 0;
                int end = 0;
                int depth = 0;
                boolean nextIsEscaped = false;
                for (int i = 0; i < filter.length(); i++) {
                    if (filter.charAt(i) == '\\' && !nextIsEscaped) {
                        nextIsEscaped = true;
                    } else {
                        if (filter.charAt(i) == '(' && !nextIsEscaped) {
                            depth++;
                        } else if (filter.charAt(i) == ')' && !nextIsEscaped) {
                            depth--;
                            if (depth == 0) {
                                end = i + 1;
                                String subfilter = filter.substring(start, end);
                                fo.item.add(parseFilterString(subfilter));
                                start = i + 1;
                            }
                        }
                        nextIsEscaped = false;
                    }
                }
                result = fo;
            } else {
                FilterItem fi = new FilterItem();
                fi.attribName = filter.substring(0, filter.indexOf("="));
                fi.attribValue = filter.substring(filter.indexOf("=") + 1);
                result = fi;
            }
            return result;
        } else {
            throw new ParseError(new ErrorInfo(ReportType.ERROR, "[BAD_LDAP_FILTER " + filter + "]", "Checker unable to parse filter fragment " + filter, "Checker was unable to parse the filter fragment as it did not have a starting and terminating round bracket"));
        }
    }

    private static Set<Set<String>> mergeBranches(List<Set<Set<String>>> branches, int depth) {
        if (branches.size() == 0) {
            return new HashSet<Set<String>>();
        }
        if (depth == (branches.size() - 1)) {
            //we're at the max depth.. so return our choices to the parent.. 
            return branches.get(depth);
        } else {
            int nextDepth = depth + 1;
            Set<Set<String>> mergedChildren = mergeBranches(branches, nextDepth);
            Set<Set<String>> thisBranchChildren = branches.get(depth);
            Set<Set<String>> results = new HashSet<Set<String>>();
            for (Set<String> fromThisBranch : thisBranchChildren) {
                for (Set<String> fromChildBranch : mergedChildren) {
                    Set<String> merged = new HashSet<String>(fromThisBranch);
                    merged.addAll(fromChildBranch);
                    results.add(merged);
                }
            }
            return results;
        }
    }

    private static Set<Set<String>> addToSet(FilterNode node) {
        Set<Set<String>> results = new HashSet<Set<String>>();
        if (node instanceof FilterItem) {
            FilterItem fi = (FilterItem) node;
            HashSet<String> currentSet = new HashSet<String>();
            currentSet.add(fi.attribName + "=" + fi.attribValue);
            results.add(currentSet);
        } else if (node instanceof FilterOperation) {
            FilterOperation fo = (FilterOperation) node;
            if (fo.op.equals(NodeOperation.OR)) {
                for (FilterNode child : fo.item) {
                    Set<Set<String>> childPermutations = addToSet(child);
                    results.addAll(childPermutations);
                }
            } else if (fo.op.equals(NodeOperation.AND)) {
                List<Set<Set<String>>> listOfResults = new ArrayList<Set<Set<String>>>();
                for (FilterNode child : fo.item) {
                    listOfResults.add(addToSet(child));
                }
                results.addAll(mergeBranches(listOfResults, 0));
            }
        }
        return results;
    }

    private static Set<Set<String>> filterTheFilters(Set<Set<String>> filters) throws ParseError {
        Set<Set<String>> results = new HashSet<Set<String>>();
        for (Set<String> filter : filters) {
            //we currently only support type=osgi.subsystem.feature
            if (!filter.contains("type=osgi.subsystem.feature")) {
                throw new ParseError(new ErrorInfo(ReportType.ERROR, "[BAD_LDAP_FILTER " + filter + "]", "Expected type=osgi.subsystem.feature missing from filter attributes "
                                                                                                         + filter, "When processing the IBM-Provision-Capability header, a clause was found that was missing the expected type=osgi.subsystem.feature missing from filter attributes "
                                                                                                                   + filter));
            }
            Set<String> featureNames = new HashSet<String>();
            for (String attrib : filter) {
                if (attrib.startsWith("osgi.identity=")) {
                    featureNames.add(attrib.substring("osgi.identity=".length()));
                }
            }
            results.add(featureNames);
        }
        return results;
    }

    private static Set<Set<String>> parseFiltersInternal(Set<String> filters) throws ParseError {
        FilterNode root;
        if (filters.size() > 1) {
            FilterOperation fo = new FilterOperation();
            fo.op = NodeOperation.AND;
            fo.item = new ArrayList<FilterNode>();
            for (String filter : filters) {
                fo.item.add(parseFilterString(filter));
            }
            root = fo;
        } else {
            root = parseFilterString(filters.iterator().next());
        }
        //now we have a nice tree of filter nodes, we can figure out our combinations =)
        Set<Set<String>> combinations;
        combinations = addToSet(root);

        return combinations;
    }

    public static Set<Set<String>> parseFilters(Set<String> filters) throws ParseError {
        return filterTheFilters(parseFiltersInternal(filters));
    }

    public static void main(String... args) throws Exception {
        String test1 = "(|(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.wasJmsClient-1.1))(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.wasJmsClient-2.0))(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.wasJmsServer-1.0)))";
        String test2 = "(&(a=a)(b=b)(|(c=c)(d=d)(e=e)))";
        String test3 = "(|(a=a)(b=b)(&(c=c)(|(d=d)(f=f))(e=e)))";
        String test4 = "(&(a=a)(b=b))";
        String test5 = "(&(a=a)(c=c))";
        Set<String> filters = new HashSet<String>();
        filters.add(test4);
        filters.add(test5);

        System.out.println(FilterUtils.parseFiltersInternal(filters));
        filters.clear();
        filters.add(test1);
        System.out.println(FilterUtils.parseFilters(filters));
    }
}
