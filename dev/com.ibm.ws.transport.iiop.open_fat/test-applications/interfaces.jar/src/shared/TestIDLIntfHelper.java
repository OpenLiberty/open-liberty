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

abstract public class TestIDLIntfHelper
{
  private static String  _id = "IDL:shared/TestIDLIntf:1.0";

  public static void insert (org.omg.CORBA.Any a, shared.TestIDLIntf that)
  {
    org.omg.CORBA.portable.OutputStream out = a.create_output_stream ();
    a.type (type ());
    write (out, that);
    a.read_value (out.create_input_stream (), type ());
  }

  public static shared.TestIDLIntf extract (org.omg.CORBA.Any a)
  {
    if (!a.type().equal(type()))
        throw new org.omg.CORBA.BAD_OPERATION("extract() failed.Expected a shared.TestIDLIntf .");
    return read (a.create_input_stream ());
  }

  private static volatile org.omg.CORBA.TypeCode __typeCode = null;
  public static org.omg.CORBA.TypeCode type ()
  {
  org.omg.CORBA.TypeCode __localTc = __typeCode;
    if (__localTc == null)
    {
      __localTc = org.omg.CORBA.ORB.init ().create_interface_tc (shared.TestIDLIntfHelper.id (), "TestIDLIntf");
      __typeCode = __localTc;
    }
    return __localTc;
  }

  public static String id ()
  {
    return _id;
  }

  public static shared.TestIDLIntf read (org.omg.CORBA.portable.InputStream istream)
  {
    return narrow (istream.read_Object (_TestIDLIntfStub.class));
  }

  public static void write (org.omg.CORBA.portable.OutputStream ostream, shared.TestIDLIntf value)
  {
    ostream.write_Object ((org.omg.CORBA.Object) value);
  }

  public static shared.TestIDLIntf narrow (org.omg.CORBA.Object obj)
  {
    if (obj == null)
      return null;
    else if (obj instanceof shared.TestIDLIntf)
      return (shared.TestIDLIntf)obj;
    else if (!obj._is_a (id ()))
      throw new org.omg.CORBA.BAD_PARAM ();
    else
    {
      org.omg.CORBA.portable.Delegate delegate = ((org.omg.CORBA.portable.ObjectImpl)obj)._get_delegate ();
      return new shared._TestIDLIntfStub (delegate);
    }
  }

  public static shared.TestIDLIntf unchecked_narrow (org.omg.CORBA.Object obj)
  {
    if (obj == null)
      return null;
    else if (obj instanceof shared.TestIDLIntf)
      return (shared.TestIDLIntf)obj;
    else
    {
      org.omg.CORBA.portable.Delegate delegate = ((org.omg.CORBA.portable.ObjectImpl)obj)._get_delegate ();
      return new shared._TestIDLIntfStub (delegate);
    }
  }

}
