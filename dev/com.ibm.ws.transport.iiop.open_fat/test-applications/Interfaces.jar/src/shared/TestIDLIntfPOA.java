/*
 * =============================================================================
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * =============================================================================
 */
package shared;

public abstract class TestIDLIntfPOA extends org.omg.PortableServer.Servant
                implements shared.TestIDLIntfOperations, org.omg.CORBA.portable.InvokeHandler
{

  public shared.TestIDLIntf _this() {
     return shared.TestIDLIntfHelper.narrow(
        super._this_object());
  }

  public shared.TestIDLIntf _this(org.omg.CORBA.ORB orb) {
     return shared.TestIDLIntfHelper.narrow(
        super._this_object(orb));
  }

  public String[] _all_interfaces(
     org.omg.PortableServer.POA poa,
     byte[] objectId) {
         return (String[])__ids.clone();
  }

  // Type-specific CORBA::Object operations
  private static String[] __ids = {
    "IDL:shared/TestIDLIntf:1.0"};

  private static java.util.Hashtable _methods = new java.util.Hashtable ();
  static
  {
    _methods.put ("_get_s", new java.lang.Integer (0));
    _methods.put ("_set_s", new java.lang.Integer (1));
  }

  public org.omg.CORBA.portable.OutputStream _invoke (String $method,
                                org.omg.CORBA.portable.InputStream in,
                                org.omg.CORBA.portable.ResponseHandler $rh)
  {
    org.omg.CORBA.portable.OutputStream out = null;
    java.lang.Integer __method = (java.lang.Integer)_methods.get ($method);
    if (__method == null)
      throw new org.omg.CORBA.BAD_OPERATION (0, org.omg.CORBA.CompletionStatus.COMPLETED_MAYBE);

    switch (__method.intValue ())
    {
       case 0:  // shared/TestIDLIntf/_get_s
       {
         String __result = null;
         __result = this.s ();
         out = $rh.createReply();
         out.write_string (__result);
         break;
       }

       case 1:  // shared/TestIDLIntf/_set_s
       {
         String newS = in.read_string ();
         this.s (newS);
         out = $rh.createReply();
         break;
       }

       default:
         throw new org.omg.CORBA.BAD_OPERATION (0, org.omg.CORBA.CompletionStatus.COMPLETED_MAYBE);
    }

    return out;
  } // _invoke


} // class _TestIDLIntfPOA
