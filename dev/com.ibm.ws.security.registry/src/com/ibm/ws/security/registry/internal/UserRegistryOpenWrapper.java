/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.registry.internal;

import java.util.List;
import java.util.Map;

import com.ibm.websphere.security.CustomRegistryException;
import com.ibm.websphere.security.EntryNotFoundException;
import com.ibm.websphere.security.Result;
import com.ibm.ws.security.registry.AttributeReader;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.SearchResult;
import com.ibm.ws.security.registry.UserRegistry;

public class UserRegistryOpenWrapper extends UserRegistryWrapper implements com.ibm.websphere.security.AttributeReader {
    private final AttributeReader wrappedUrAttr;

    public UserRegistryOpenWrapper(UserRegistry wrappedUr) {
        super(wrappedUr);
        if (wrappedUr instanceof AttributeReader) {
            wrappedUrAttr = (AttributeReader) wrappedUr;
        } else {
            throw new IllegalArgumentException();

        }
    }

    @Override
    public Map<String, Object> getAttributesForUser(String userSecurityName, List<String> attributeNames) throws EntryNotFoundException, CustomRegistryException {
        try {
            return wrappedUrAttr.getAttributesForUser(userSecurityName, attributeNames);
        } catch (RegistryException e) {
            throw new CustomRegistryException(e.getMessage(), e);
        } catch (com.ibm.ws.security.registry.EntryNotFoundException e) {
            throw new EntryNotFoundException(e.getMessage(), e);
        }

    }

    @Override
    public Result findUsersByAttribute(String attributeName, String value, int limit) throws CustomRegistryException {
        try {
            SearchResult ret = wrappedUrAttr.findUsersByAttribute(attributeName, value, limit);
            Result result = new Result();
            result.setList(ret.getList());
            if (ret.hasMore()) {
                result.setHasMore();
            }
            return result;

        } catch (RegistryException e) {
            throw new CustomRegistryException(e.getMessage(), e);
        }
    }

}
