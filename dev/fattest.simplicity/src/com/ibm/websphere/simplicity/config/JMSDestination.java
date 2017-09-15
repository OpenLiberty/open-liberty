/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity.config;

import java.util.Arrays;
import java.util.List;

public class JMSDestination extends AdminObject {

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.simplicity.config.ConfigElement#clone()
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        JMSDestination clone = (JMSDestination) super.clone();
        return clone;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(getClass().getSimpleName()).append('{');
        buf.append("id=\"" + (getId() == null ? "" : getId()) + "\" ");
        if (getJndiName() != null)
            buf.append("jndiName=\"" + getJndiName() + "\" ");

        List<?> nestedElementsList = Arrays.asList(
                        getProperties_FAT1()
                        );
        for (ConfigElementList<?> nestedElements : (List<ConfigElementList<?>>) nestedElementsList)
            if (nestedElements != null && nestedElements.size() > 0)
                for (Object o : nestedElements)
                    buf.append(", " + o);
        buf.append("}");
        return buf.toString();
    }

}
