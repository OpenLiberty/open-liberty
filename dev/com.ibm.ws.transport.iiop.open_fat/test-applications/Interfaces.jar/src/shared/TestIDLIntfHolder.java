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

public final class TestIDLIntfHolder implements org.omg.CORBA.portable.Streamable
{
  public shared.TestIDLIntf value = null;

  public TestIDLIntfHolder ()
  {
  }

  public TestIDLIntfHolder (shared.TestIDLIntf initialValue)
  {
    value = initialValue;
  }

  public void _read (org.omg.CORBA.portable.InputStream i)
  {
    value = shared.TestIDLIntfHelper.read (i);
  }

  public void _write (org.omg.CORBA.portable.OutputStream o)
  {
    shared.TestIDLIntfHelper.write (o, value);
  }

  public org.omg.CORBA.TypeCode _type ()
  {
    return shared.TestIDLIntfHelper.type ();
  }

}
