/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.aries.buildtasks.semantic.versioning;

public class BinaryCompatibilityStatus
{
    private final boolean compatible;
    private final String reason;

    public BinaryCompatibilityStatus(boolean compatible, String reason) {
        this.compatible = compatible;
        this.reason = reason;
    }

    public boolean isCompatible()
    {
        return compatible;
    }

    public String getReason()
    {
        return reason;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + (compatible ? 1231 : 1237);
        result = prime * result + ((reason == null) ? 0 : reason.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        BinaryCompatibilityStatus other = (BinaryCompatibilityStatus) obj;
        if (compatible != other.compatible)
            return false;
        if (reason == null) {
            if (other.reason != null)
                return false;
        } else if (!reason.equals(other.reason))
            return false;
        return true;
    }

}
