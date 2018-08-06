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

public interface TargetCache_Readable {
    List<TargetCache_ParseError> readUsing(TargetCache_Reader reader) throws IOException;
}
