/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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

package com.ibm.websphere.security.wim.scim20.model.extensions;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.ibm.ws.security.wim.scim20.model.extensions.EnterpriseManagerImpl;

@JsonDeserialize(as = EnterpriseManagerImpl.class)
public interface EnterpriseManager {
    public String getDisplayName();

    public String getRef();

    public String getValue();

    public void setDisplayName(String displayName);

    public void setRef(String ref);

    public void setValue(String value);
}
