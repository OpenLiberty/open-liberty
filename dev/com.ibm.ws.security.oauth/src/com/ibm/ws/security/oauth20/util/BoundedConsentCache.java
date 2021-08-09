/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.util;

import java.io.Serializable;

public class BoundedConsentCache extends BoundedCommonCache<ConsentCacheKey> implements Serializable {

    private static final long serialVersionUID = -6352052283899728953L;

    public BoundedConsentCache(int maxCapacity) {
        super(maxCapacity);
    }

}
