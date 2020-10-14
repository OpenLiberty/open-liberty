/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws390.ola.jca;

import java.io.UnsupportedEncodingException;
import java.rmi.RemoteException;

import javax.ejb.CreateException;
import javax.ejb.RemoteHome;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.websphere.ola.Execute;
import com.ibm.websphere.ola.ExecuteHome;

/**
 * Session Bean implementation class RemoteEJBProxy
 */
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
@RemoteHome(com.ibm.websphere.ola.ExecuteHome.class)
@Stateless
public class RemoteEJBProxy
{
    private static final TraceComponent tc = Tr.register(RemoteEJBProxy.class, "OLA", "com.ibm.ws390.ola.resources.olaMessages");
    String remoteEjbHomeJndiName = "";

    /**
     * Looks up the remote OLA EJB and calls execute on it.
     * @param inbytes The input Bytes. The first 256 bytes of this input
     *                represents the jndiName.
     * @return The remote OLA EJB response.
     * @throws RemoteException
     */
    public byte[] execute(byte[] inbytes) throws RemoteException
    {
        byte[] outputBytes = null;

        //---------------------------------------------------------------------
        // Extract the remote ejb's JNDI name.
        //---------------------------------------------------------------------
        int fixedJndiNameSize = 256;
        int netPayloadSize = inbytes.length - fixedJndiNameSize;
        byte[] jndiNameBytes = new byte[fixedJndiNameSize];
        System.arraycopy(inbytes, 0, jndiNameBytes, 0, fixedJndiNameSize);

        //---------------------------------------------------------------------
        // On entry the jndi name is in ebcdic. Convert it to ascii.
        //---------------------------------------------------------------------
        try
        {
            remoteEjbHomeJndiName = (new String(jndiNameBytes, "IBM-1047")).trim(); 
        }
        catch(UnsupportedEncodingException uee)
        {
            processException("Failure while attempting to decode remote jndi name with charset IBM-1047", uee);
        }

        //---------------------------------------------------------------------
        // Extract the remote ejb's payload as is.
        //---------------------------------------------------------------------
        byte[] payload = new byte[netPayloadSize];
        System.arraycopy(inbytes, fixedJndiNameSize, payload, 0, netPayloadSize);

        //---------------------------------------------------------------------
        // Lookup remote EJB and call execute.
        //---------------------------------------------------------------------
        try
        {
            InitialContext ctx = new InitialContext();
            ExecuteHome remoteEjbHome = (ExecuteHome)ctx.lookup(remoteEjbHomeJndiName);
            Execute remoteEjb = remoteEjbHome.create();
            outputBytes = remoteEjb.execute(payload);
        }
        catch (NamingException ne)
        {
            processException("Failure while attempting to lookup the target ejb " +
                             "identified with the JNDI name of " + remoteEjbHomeJndiName, ne);
        }
        catch (CreateException ce)
        {
            processException("Failure while attempting to create an instance of the target "+
                             "EJB identified with the JNDI name of " + remoteEjbHomeJndiName,ce);
        }
        catch (Throwable t)
        {
            processException("Failure while attempting process work on the target EJB "+
                             "identified with the JNDI name of " + remoteEjbHomeJndiName, t);
        }

        return outputBytes;
    }
    
    /**
     * Processes exceptions.
     * @param failureMessage The message to be used.
     * @param t The exception to be processed.
     * @throws RemoteException
     */
    private void processException(String failureMessage, Throwable t) throws RemoteException
    {
        Tr.error(tc, "CWWKB0389E", new Object[]{remoteEjbHomeJndiName, t.toString()});
        throw new RemoteException(failureMessage, t); 
    }
}
