/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2011, 2018
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.anno.classsource.specification;

import java.util.List;

import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy;

import com.ibm.wsspi.anno.util.Util_RelativePath;

public interface ClassSource_Specification_Elements extends ClassSource_Specification {
    List<? extends ClassSource_Specification_Element> getInternalElements();
    ClassSource_Specification_Element addInternalElement(String name, ScanPolicy policy, Util_RelativePath relativePath);

    List<? extends ClassSource_Specification_Element> getExternalElements();
    ClassSource_Specification_Element addExternalElement(String name, Util_RelativePath relativePath);
}
