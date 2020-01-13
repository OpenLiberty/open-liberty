/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package loginmodule;

import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;

/**
 * GSSName implementation to test that our connection pooling can pool GSSCredentials
 * but without the need to use real GSSCredentials or GSSName from an implementation like Kerberos.
 */
public class TestGSSName implements GSSName {

    public String name;

    /**
     * @param string
     */
    public TestGSSName(String name) {
        this.name = name;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.ietf.jgss.GSSName#equals(org.ietf.jgss.GSSName)
     */
    @Override
    public boolean equals(GSSName another) throws GSSException {
        if (another instanceof TestGSSName)
            if (this.name.equals(((TestGSSName) another).name))
                return true;
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.ietf.jgss.GSSName#canonicalize(org.ietf.jgss.Oid)
     */
    @Override
    public GSSName canonicalize(Oid mech) throws GSSException {
        //Auto-generated method stub
        return this;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.ietf.jgss.GSSName#export()
     */
    @Override
    public byte[] export() throws GSSException {
        //Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.ietf.jgss.GSSName#getStringNameType()
     */
    @Override
    public Oid getStringNameType() throws GSSException {
        //Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.ietf.jgss.GSSName#isAnonymous()
     */
    @Override
    public boolean isAnonymous() {
        //Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.ietf.jgss.GSSName#isMN()
     */
    @Override
    public boolean isMN() {
        //Auto-generated method stub
        return false;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

}
