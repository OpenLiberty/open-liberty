/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.ibm.ws.config.xml;

import java.util.Map;

public interface ServerConfigRawAttributesService {

    /**
     * Retrieves the raw/original config values for the specified configuration element name (i.e. the ibm:alias name for the OCD element)
     * For example `mpHealth` in {@code <OCD id="io.openliberty.microprofile.health" name="%mpHealth" description="%mpHealth.desc" ibm:alias="mpHealth" > }
     *
     * @param elementName The ibm:alias name for the OCD element
     * @return Map of the attribute -> value. Returns empty map if there are no values.
     */
    public Map<String, Object> getRawAttributesFromElement(String elementName);

}
