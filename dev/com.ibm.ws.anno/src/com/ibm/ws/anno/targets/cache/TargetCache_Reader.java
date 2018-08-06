/*
* IBM Confidential
*
* OCO Source Materials
*
* WLP Copyright IBM Corp. 2014, 2018
*
* The source code for this program is not published or otherwise divested
* of its trade secrets, irrespective of what has been deposited with the
* U.S. Copyright Office.
*/
package com.ibm.ws.anno.targets.cache;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import com.ibm.ws.anno.targets.TargetsTableClasses;
import com.ibm.ws.anno.targets.TargetsTableClassesMulti;
import com.ibm.ws.anno.targets.TargetsTableContainers;
import com.ibm.ws.anno.targets.TargetsTableDetails;
import com.ibm.ws.anno.targets.TargetsTableTimeStamp;
import com.ibm.ws.anno.targets.TargetsTableAnnotations;
import com.ibm.ws.anno.util.internal.UtilImpl_InternMap;


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
