/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container.util;

import java.io.Serializable;

/**
 * This class is used to StatefulBeanO object when serializing a SFSB
 * to the passivation file. There is no need to hold any of the StatefulBeanO
 * data in the replacement object since a new StatefulBeanO is created when
 * the SFSB is activated.
 */
public class StatefulBeanOReplacement implements Serializable
{
    private static final long serialVersionUID = -7308408948738957218L;

    /**
     * Default CTOR.
     */
    public StatefulBeanOReplacement()
    {
        // Intentionally left empty since no data in this object.
    }

}
