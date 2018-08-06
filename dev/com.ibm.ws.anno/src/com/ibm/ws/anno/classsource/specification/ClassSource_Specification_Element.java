/*
u* IBM Confidential
*
* OCO Source Materials
*
* WLP Copyright IBM Corp. 2014, 2018
*
* The source code for this program is not published or otherwise divested
* of its trade secrets, irrespective of what has been deposited with the
* U.S. Copyright Office.
*/
package com.ibm.ws.anno.classsource.specification;

import java.util.logging.Logger;

import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.anno.classsource.ClassSource_Exception;

import com.ibm.wsspi.anno.util.Util_RelativePath;

public interface ClassSource_Specification_Element {
    void log(Logger useLogger);

    //

    String getName();
    ScanPolicy getPolicy();

    Util_RelativePath getPath();
    String getEntryPrefix();

    void addTo(ClassSource_Aggregate rootClassSource) throws ClassSource_Exception;
}
