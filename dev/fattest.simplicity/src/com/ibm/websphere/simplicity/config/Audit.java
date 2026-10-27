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
package com.ibm.websphere.simplicity.config;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * Defines configuration attributes of the {@code <audit>} element (audit-1.0 feature).
 *
 * Example server.xml snippet:
 * <pre>
 *   &lt;audit generateNewSession="false"/&gt;
 * </pre>
 */
public class Audit extends ConfigElement {

    private Boolean generateNewSession;

    /**
     * @return whether the audit subsystem is allowed to create a new HTTP session
     *         when none exists ({@code true} = legacy behaviour, {@code false} = fix for GH-29751)
     */
    public Boolean isGenerateNewSession() {
        return generateNewSession;
    }

    /**
     * @param generateNewSession set to {@code false} to prevent the audit subsystem
     *                           from creating a new HTTP session
     */
    @XmlAttribute
    public void setGenerateNewSession(Boolean generateNewSession) {
        this.generateNewSession = generateNewSession;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("Audit{");
        buf.append("id=\"").append(this.getId()).append("\" ");
        if (generateNewSession != null)
            buf.append("generateNewSession=\"").append(generateNewSession).append("\" ");
        buf.append("}");
        return buf.toString();
    }
}
