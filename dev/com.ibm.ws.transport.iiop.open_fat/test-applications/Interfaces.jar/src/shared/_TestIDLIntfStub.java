/*
 * =============================================================================
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * =============================================================================
 */
package shared;


public class _TestIDLIntfStub extends org.omg.CORBA_2_3.portable.ObjectImpl implements shared.TestIDLIntf
{
  // Constructors
  // NOTE:  If the default constructor is used, the
  //        object is useless until _set_delegate (...)
  //        is called.
  public _TestIDLIntfStub ()
  {
    super ();
  }

  public _TestIDLIntfStub (org.omg.CORBA.portable.Delegate delegate)
  {
    super ();
    _set_delegate (delegate);
  }

  public String s ()
  {
    while(true) {
      if ( !this._is_local() ) {
        org.omg.CORBA.portable.InputStream _in = null;
        try {
          org.omg.CORBA.portable.OutputStream _out = _request ("_get_s",true);
          _in = _invoke (_out);
          String __result = _in.read_string ();
          return __result;
        } catch (org.omg.CORBA.portable.ApplicationException _ex) {
          _in = _ex.getInputStream ();
          String _id = _ex.getId ();
          throw new org.omg.CORBA.UNKNOWN( "Unexpected User Exception: " + _id );
        } catch (org.omg.CORBA.portable.RemarshalException _rm) {
          continue;
        } catch (org.omg.CORBA.portable.UnknownException _ue) {
          Throwable _oe = _ue.originalEx;
          if (_oe instanceof Error)
              throw (Error)_oe;
          else if (_oe instanceof RuntimeException)
              throw (RuntimeException)_oe;
          else
              throw _ue;
        } finally { _releaseReply (_in); }
      }
      else {
        org.omg.CORBA.portable.ServantObject _so =
          _servant_preinvoke( "_get_s",_opsClass );
        if ( _so == null ) { 
          continue;
        }
        try {
          String __result = ((shared.TestIDLIntfOperations)_so.servant).s(  );
          return __result;
        } finally { _servant_postinvoke( _so ); }
      }
    }
  } // s

  public void s (String newS)
  {
    while(true) {
      if ( !this._is_local() ) {
        org.omg.CORBA.portable.InputStream _in = null;
        try {
          org.omg.CORBA.portable.OutputStream _out = _request ("_set_s",true);
          _out.write_string (newS);
          _in = _invoke (_out);
          return;
        } catch (org.omg.CORBA.portable.ApplicationException _ex) {
          _in = _ex.getInputStream ();
          String _id = _ex.getId ();
          throw new org.omg.CORBA.UNKNOWN( "Unexpected User Exception: " + _id );
        } catch (org.omg.CORBA.portable.RemarshalException _rm) {
          continue;
        } catch (org.omg.CORBA.portable.UnknownException _ue) {
          Throwable _oe = _ue.originalEx;
          if (_oe instanceof Error)
              throw (Error)_oe;
          else if (_oe instanceof RuntimeException)
              throw (RuntimeException)_oe;
          else
              throw _ue;
        } finally { _releaseReply (_in); }
      }
      else {
        org.omg.CORBA.portable.ServantObject _so =
          _servant_preinvoke( "_set_s",_opsClass );
        if ( _so == null ) { 
          continue;
        }
        try {
          ((shared.TestIDLIntfOperations)_so.servant).s( newS );
          return;
        } finally { _servant_postinvoke( _so ); }
      }
    }
  } // s

  // Type-specific CORBA::Object operations
  private static String[] __ids = {
    "IDL:shared/TestIDLIntf:1.0"};

  public String[] _ids ()
  {
    return (String[])__ids.clone ();
  }

  final public static java.lang.Class _opsClass =
    shared.TestIDLIntfOperations.class;

  private void readObject (java.io.ObjectInputStream s)
  {
     try 
     {
       String str = s.readUTF ();
       org.omg.CORBA.Object obj = org.omg.CORBA.ORB.init ((String[])null, null).string_to_object (str);
       org.omg.CORBA.portable.Delegate delegate = ((org.omg.CORBA.portable.ObjectImpl) obj)._get_delegate ();
       _set_delegate (delegate);
     } catch (java.io.IOException e) {}
  }

  private void writeObject (java.io.ObjectOutputStream s)
  {
     try 
     {
       String str = org.omg.CORBA.ORB.init ((String[])null, null).object_to_string (this);
       s.writeUTF (str);
     } catch (java.io.IOException e) {}
  }
} // class _TestIDLIntfStub
