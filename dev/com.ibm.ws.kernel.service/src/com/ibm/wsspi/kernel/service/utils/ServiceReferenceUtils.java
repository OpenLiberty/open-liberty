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
package com.ibm.wsspi.kernel.service.utils;

import java.util.Arrays;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

public class ServiceReferenceUtils {

    private static final Integer DEFAULT_RANKING = 0;

    /**
     * @param ref the service reference
     * @return the service id
     * @see Constants#SERVICE_ID
     */
    public static Long getId(ServiceReference<?> ref) {
        return (Long) ref.getProperty(Constants.SERVICE_ID);
    }

    /**
     * @param ref the service reference
     * @return the service ranking, or 0 if unspecified
     * @see Constants#SERVICE_RANKING
     */
    public static Integer getRanking(ServiceReference<?> ref) {
        Object ranking = ref.getProperty(Constants.SERVICE_RANKING);
        return ranking instanceof Integer ? ((Integer) ranking) : DEFAULT_RANKING;
    }

    /**
     * Sorts an array of service references in reverse order (highest service
     * ranking first). This method properly handles asynchronous updates to the
     * service ranking.
     * 
     * @param refs input and output array of service references
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void sortByRankingOrder(ServiceReference<?>[] refs) {
        if (refs.length > 1) {
            ConcurrentServiceReferenceElement[] tmp = new ConcurrentServiceReferenceElement[refs.length];

            // Take a snapshot of the service rankings.
            for (int i = 0; i < refs.length; i++) {
                tmp[i] = new ConcurrentServiceReferenceElement(null, refs[i]);
            }

            Arrays.sort(tmp);

            // Copy the sorted service references.
            for (int i = 0; i < refs.length; i++) {
                refs[i] = tmp[i].getReference();
            }
        }
    }

}
