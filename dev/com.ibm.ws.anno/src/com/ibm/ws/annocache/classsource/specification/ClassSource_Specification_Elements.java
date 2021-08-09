/*******************************************************************************
 * Copyright (c) 2011, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.classsource.specification;

import java.util.List;

import com.ibm.wsspi.annocache.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.annocache.util.Util_RelativePath;

public interface ClassSource_Specification_Elements extends ClassSource_Specification {
    List<? extends ClassSource_Specification_Element> getInternalElements();
    ClassSource_Specification_Element addInternalElement(String name, ScanPolicy policy, Util_RelativePath relativePath);

    List<? extends ClassSource_Specification_Element> getExternalElements();
    ClassSource_Specification_Element addExternalElement(String name, Util_RelativePath relativePath);
}
