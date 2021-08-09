/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container.service.config;

import java.util.List;

/**
 * Collated web fragment information.
 * 
 * Provides information for the WEB-INF/lib JAR files.
 * 
 * All JARs are represented as either ordered fragments
 * or excluded fragments.
 * 
 * The excluded fragments collection is empty unless there
 * is an 'absolute-ordering' element in the web descriptor.
 */
public interface WebFragmentsInfo {
    /**
     * Tell the feature level of the web container.
     * 
     * This impacts whether the metadata-complete setting affects
     * the processing of 'absolute-ordering' elements. See {@link #getExcludedFragments()}.
     * 
     * @return The feature level of the web container.
     */
    int getServletSpecLevel();

    /**
     * Tell the schema level of the web module descriptor.
     * 
     * @return The schema level of the web module descriptor.
     */
    String getServletSchemaLevel();

    /**
     * Tell if the web module is metadata-complete. This
     * uses the web module deployment descriptor, the version
     * of that descriptor, and the 'metadata-complete' attribute
     * value.
     * 
     * @return True or false telling if the web module is metadata-complete.
     */
    boolean isModuleMetadataComplete();

    /**
     * An ordered list of fragment information. There is one element
     * for every non-excluded JAR under the WEB-INF/lib folder.
     * 
     * The list is ordered using web module and fragment metadata.
     * 
     * @return The ordered list of fragment information.
     */
    List<WebFragmentInfo> getOrderedFragments();

    /**
     * An unordered list of excluded fragment information. There is
     * one element for every excluded JAR under the WEB-INF/lib folder.
     * 
     * A JAR is excluded if an 'absolute-ordering' element is present
     * in the web module deployment descriptor and the JAR is not listed
     * within that element.
     * 
     * The use of the 'absolute-ordering' element is conditional on the
     * web module descriptor schema version and on the web module descriptor
     * 'metadata-complete' setting. For schema version 3.0, the absolute
     * ordering element is used only when 'metadata-complete' is false. For
     * schema version 3.1, the absolute ordering element is used regardless
     * of the 'metadata-complete' value.
     * 
     * The element <em>should</em> have been used with schema version 3.0, but
     * is not used because WebSphere v8.0 did not use it when metadata-complete
     * is false. The schema version 3.1 behavior is the specification defined
     * behavior for both schema version 3.1 and schema version 3.0.
     * 
     * @return An unordered list of excluded fragments.
     */
    List<WebFragmentInfo> getExcludedFragments();
}
