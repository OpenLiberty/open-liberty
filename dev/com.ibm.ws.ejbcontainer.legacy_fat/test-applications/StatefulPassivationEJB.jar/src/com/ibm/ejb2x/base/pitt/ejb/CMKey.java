/*******************************************************************************
 * Copyright (c) 2002, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ejb2x.base.pitt.ejb;

import java.io.Serializable;

/**
 * The primary key class for a simple entity bean. <p>
 * 
 * @author Chriss Stephens
 * @version $Id: CMEntityKey.java,v 1.4 1999/01/08 16:25:15 chriss Exp $
 */
public class CMKey implements Serializable {
    private static final long serialVersionUID = 528243766243170931L;

    public String primaryKey;

    /**
     * Create a new instance of a primary key set to the value of s.
     */
    //
    public CMKey(String s) {
        primaryKey = s;
    }

    /**
     * Return string representing this primary key.
     */
    @Override
    public String toString() {
        return primaryKey;
    }
}