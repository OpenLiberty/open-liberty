/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container.service.metadata;

public class MetaDataException extends Exception {
    private static final long serialVersionUID = 2919646540712528838L;

    public MetaDataException(String s) {
        super(s);
    }

    public MetaDataException(String s, Throwable t) {
        super(s, t);
    }

    public MetaDataException(Throwable t) {
        super(t);
    }
}
