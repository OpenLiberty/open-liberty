/* ***************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * ***************************************************************************/
package com.ibm.ws.jndi.iiop;

import java.lang.ClassCastException;
import java.lang.String;
import java.lang.Throwable;
import java.rmi.Remote;
import javax.rmi.CORBA.Tie;
import org.omg.CORBA.BAD_OPERATION;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Object;
import org.omg.CORBA.SystemException;
import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.OutputStream;
import org.omg.CORBA.portable.ResponseHandler;
import org.omg.CORBA.portable.UnknownException;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAPackage.ObjectNotActive;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;

public class _TestableImpl_Tie extends org.omg.PortableServer.Servant implements Tie {
    
    private TestableImpl target = null;
    
    private static final String[] _type_ids = {
        "RMI:com.ibm.ws.jndi.iiop.Testable:0000000000000000"
    };
    
    public void setTarget(Remote target) {
        this.target = (TestableImpl) target;
    }
    
    public Remote getTarget() {
        return target;
    }
    
    public Object thisObject() {
        return _this_object();
    }
    
    public void deactivate() {
        try {
            _poa().deactivate_object(_poa().servant_to_id(this));
        }
        catch(WrongPolicy e) { }
        catch(ObjectNotActive e) { }
        catch(ServantNotActive e) { }
    }
    
    public ORB orb() {
        return _orb();
    }
    
    public void orb(ORB orb) {
        try {
            ((org.omg.CORBA_2_3.ORB)orb).set_delegate(this);
        }
        catch(ClassCastException e) {
            throw new BAD_PARAM("POA Servant needs an org.omg.CORBA_2_3.ORB");
        }
    }
    
    public String[] _all_interfaces(POA poa, byte[] objectId) { 
        return (String [] )  _type_ids.clone();
    }
    
    public OutputStream _invoke(String method, InputStream _in, ResponseHandler reply) throws SystemException {
        try {
            org.omg.CORBA_2_3.portable.InputStream in = 
                (org.omg.CORBA_2_3.portable.InputStream) _in;
            if (method.equals("_get_name")) {
                return _get_name(in, reply);
            }
            throw new BAD_OPERATION();
        } catch (SystemException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new UnknownException(ex);
        }
    }
    
    private OutputStream _get_name(org.omg.CORBA_2_3.portable.InputStream in , ResponseHandler reply) throws Throwable {
        String result = target.getName();
        org.omg.CORBA_2_3.portable.OutputStream out = 
            (org.omg.CORBA_2_3.portable.OutputStream) reply.createReply();
        out.write_value(result,String.class);
        return out;
    }
}
