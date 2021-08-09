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

import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;

/**
 * GSSCredential implementation to test that our connection pooling can pool GSSCredentials
 * but without the need to use real GSSCredentials from an implementation like Kerberos.
 */
public class TestGSSCredential implements GSSCredential {

    public GSSName name;

    public TestGSSCredential(GSSName name) throws GSSException {
        add(name, Integer.MAX_VALUE, Integer.MAX_VALUE, null, INITIATE_AND_ACCEPT);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.ietf.jgss.GSSCredential#dispose()
     */
    @Override
    public void dispose() throws GSSException {
        //Auto-generated method stub
    }

    /*
     * (non-Javadoc)
     *
     * @see org.ietf.jgss.GSSCredential#getName()
     */
    @Override
    public GSSName getName() throws GSSException {
        //Auto-generated method stub
        return name;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.ietf.jgss.GSSCredential#getName(org.ietf.jgss.Oid)
     */
    @Override
    public GSSName getName(Oid mech) throws GSSException {
        //Auto-generated method stub
        return name;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.ietf.jgss.GSSCredential#getRemainingLifetime()
     */
    @Override
    public int getRemainingLifetime() throws GSSException {
        //Auto-generated method stub
        return Integer.MAX_VALUE;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.ietf.jgss.GSSCredential#getRemainingInitLifetime(org.ietf.jgss.Oid)
     */
    @Override
    public int getRemainingInitLifetime(Oid mech) throws GSSException {
        //Auto-generated method stub
        return Integer.MAX_VALUE;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.ietf.jgss.GSSCredential#getRemainingAcceptLifetime(org.ietf.jgss.Oid)
     */
    @Override
    public int getRemainingAcceptLifetime(Oid mech) throws GSSException {
        //Auto-generated method stub
        return Integer.MAX_VALUE;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.ietf.jgss.GSSCredential#getUsage()
     */
    @Override
    public int getUsage() throws GSSException {
        //Auto-generated method stub
        return INITIATE_AND_ACCEPT;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.ietf.jgss.GSSCredential#getUsage(org.ietf.jgss.Oid)
     */
    @Override
    public int getUsage(Oid mech) throws GSSException {
        //Auto-generated method stub
        return INITIATE_AND_ACCEPT;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.ietf.jgss.GSSCredential#getMechs()
     */
    @Override
    public Oid[] getMechs() throws GSSException {
        //Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.ietf.jgss.GSSCredential#add(org.ietf.jgss.GSSName, int, int, org.ietf.jgss.Oid, int)
     */
    @Override
    public void add(GSSName name, int initLifetime, int acceptLifetime, Oid mech, int usage) throws GSSException {
        this.name = name;

    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object c) {
        if (c instanceof TestGSSCredential)
            try {
                return name.equals(((TestGSSCredential) c).name);
            } catch (GSSException e) {
            }
        return false;
    }

}
