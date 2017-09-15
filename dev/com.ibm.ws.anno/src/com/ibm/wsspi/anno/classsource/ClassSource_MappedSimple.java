/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.wsspi.anno.classsource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

/**
 * <p>Simplified API for new class source type. Extenders need only
 * provide an implementation of the simplified {@link ClassSource_MappedSimple.SimpleClassProvider} API. Scan
 * processing (iteration, lookup caching, timing and other statistics)
 * are handled by the default implementation.</p>
 */
public interface ClassSource_MappedSimple extends ClassSource {

    static interface SimpleClassProvider {
        /**
         * <p>Answer a name for this provider. The name is used logging and is
         * intended to be meaningful and easy to read.</p>
         * 
         * @return A friendly name for this simple class provider.
         */
        String getName();

        /**
         * <p>Answer the resource names of this provider. No order is presumed
         * for the names, however, this may be supplied by an implementation.</p>
         * 
         * <p>The values <strong>must</strong> be proper class resource
         * names. Any mapping of the names must be internal to the provider.</p>
         * 
         * @return The paths supplied by this simple class provider.
         */
        Collection<String> getResourceNames();

        /**
         * <p>Answer an input stream for a specified resource. The path is
         * expected to be one supplied by {@link #getResourceNames()}.</p>
         * 
         * <p>The result input stream must be closed.</p>
         * 
         * @param resourceName The resource to open as an input stream.
         * @return The input stream for the resource.
         * 
         * @throws IOException Thrown if the stream could not be opened.
         */
        InputStream openResource(String resourceName) throws IOException;
    }

    /**
     * <p>Answer the simple class provider used by this simple class source.</p>
     * 
     * @return The simple class provider of this simple class source.
     */
    SimpleClassProvider getProvider();
}