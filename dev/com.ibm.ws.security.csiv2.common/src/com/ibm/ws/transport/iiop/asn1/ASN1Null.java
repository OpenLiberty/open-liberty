/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Some of the code was derived from code supplied by the Apache Software Foundation licensed under the Apache License, Version 2.0.
 */

package com.ibm.ws.transport.iiop.asn1;

import java.io.IOException;

/**
 * A NULL object.
 */
public abstract class ASN1Null
    extends DERObject
{
    public ASN1Null()
    {
    }

    public int hashCode()
    {
        return 0;
    }

    public boolean equals(
        Object o)
    {
        if ((o == null) || !(o instanceof ASN1Null))
        {
            return false;
        }

        return true;
    }

    abstract void encode(DEROutputStream out)
        throws IOException;
}
