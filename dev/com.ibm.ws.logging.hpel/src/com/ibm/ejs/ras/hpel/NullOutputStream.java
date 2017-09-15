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
/**
 * 
 */
package com.ibm.ejs.ras.hpel;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author rohits
 *
 */
public class NullOutputStream extends OutputStream {

    /**
     * 
     */
    public NullOutputStream() {
        // TODO Auto-generated constructor stub
    }

    /* (non-Javadoc)
     * @see java.io.OutputStream#write(int)
     */
    @Override
    public void write(int b) throws IOException {
        // TODO Auto-generated method stub

    }

}
