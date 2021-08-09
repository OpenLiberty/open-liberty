/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.utils;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * <p>This class implements a version scheme of the form n.n.n.n where each
 *   n is a number. This implementation was derived from the version in
 *   messaging.impl which itself was derived from a class in the config service.
 * </p>
 * 
 * <p>This class differs in several ways:
 *   <ul>
 *     <li>It is Serializable.</li>
 *     <li>It correctly implements hashCode and equals, so it can be added to a Map or Set.</li>
 *     <li>It is Cloneable.</li>
 *   </ul>
 * </p>
 * 
 * <p>This class is immutable</p>
 */
public final class Version implements Comparable<Version>, Externalizable, Cloneable
{
  /** The serial version UID of this class */
  private static final long serialVersionUID = 918567830801402389L;
  /** The components of the version */
  private int[] _components;
  /** indicates whether calls to readExternal should work (defaults to true) */
  private boolean _readOnly = true;
  
  /* ---------------------------------------------------------------------- */
  /* Version method                                    
  /* ---------------------------------------------------------------------- */
  /**
   * This constructors reads a string into a version object. An empty string produces
   * and undefined behaviour.
   * 
   * @param ver the string version spec.
   * @throws NumberFormatException if a version component contains a non-number
   */
  public Version(String ver)
  {
    String quote = "\"";
    // Replace all quotes in the version. This is for cases where the version
    // has been enclosed in quotes.
    ver = ver.replace(quote, "");
    
    // split the version up based on the periods.
    String[] componentsOfVersion = ver.trim().split("\\.");
    _components = new int[componentsOfVersion.length];
    
    // convert the period separated strings into ints.
    for (int i = 0; i < componentsOfVersion.length; i++)
    {
      _components[i] = Integer.parseInt(componentsOfVersion[i]);
    }
  }
  
  /* ------------------------------------------------------------------------ */
  /* Version method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This constructor creates the version from the specified component versions.
   * An array of length zero produces an undefined behaviour.
   * 
   * @param ver the version components in int form.
   */
  public Version(int[] ver)
  {
    _components = new int[ver.length];
    
    // Note an array copy is required in order to ensure that this class is immutable.
    System.arraycopy(ver, 0, _components, 0, ver.length);
  }
  
  /* ------------------------------------------------------------------------ */
  /* Version method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * <p>This constructor should only be used during deserialization. It allows 
   *   readExternal to update the contents of this Version once and once only.
   * </p>
   * 
   * <p>The behaviour of a Version constructed using this constructor is undefined
   *   until the readExternal method is called.
   * </p>
   */
  public Version()
  {
    _readOnly = false;
  }
  
  /* ------------------------------------------------------------------------ */
  /* compareTo method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method works as follows:
   * 
   * <p>If this version is 6.1.0.0 the following would result in 0 being returned: 
   *   <ul>
   *     <li>6.1.0.0</li>
   *     <li>6.1</li>
   *   </ul>
   * </p>
   * 
   * <p>If this version is 6.1.0.0 the following would result in 1 being returned:
   *   <ul>
   *     <li>6.0.2</li>
   *     <li>6.0.2.17</li>
   *   </ul>
   * </p>
   * 
   * <p>If this version is 6.1.0.0 the following would result in -1 being returned:
   *   <ul>
   *     <li>6.1.0.3</li>
   *     <li>7.0.0.0</li>
   *   </ul>
   * </p>
   * 
   * @param ver The version
   * @return    0 if they are equal, -1 if this is less than the other, and 1
   *             if this is greater than the other.
   * 
   * @see java.lang.Comparable#compareTo(Object)
   */
  public int compareTo(Version ver) 
  {
    int[] otherComponents = ver._components;

    // work out the minimum length of the components for each version.
    int mylen = _components.length;
    int otherlen = otherComponents.length;
    // work out if the other one is longer than this one.
    boolean longer = (mylen < otherlen);
    int minLen = (longer ? mylen : otherlen);
    
    // loop around all the entries up to the minimum length
    int i = 0;
    for ( i=0; i < minLen; i++ ) 
    {
      int firstVal = _components[i];
      int secondVal = otherComponents[i];
      if ( firstVal < secondVal ) return -1;
      if ( firstVal > secondVal ) return 1;
    }

    
    if (mylen != otherlen)
    {
      // if we are here then we know that so far they are equal.
      // In order for them to be truly equal the longer one must
      // contain only zeros. Otherwise the longer version is
      // higher than the shorter version.
      if (longer) 
      {
        for (int j = i+1; j<otherlen; j++) 
        {
          if (otherComponents[j] != 0) return -1;
        }
      } 
      else 
      {
        for (int j = i+1; j<mylen; j++) 
        {
          if (_components[j] != 0) return 1;
        }
      }
    }
    
    // if we get here then they are equal.
    return 0;
  }
  
  /* ------------------------------------------------------------------------ */
  /* hashCode method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method returns the sun of the components as the hashCode. This is
   * because 6.1 and 6.1.0.0 need to have the same hashCode as they are equal. 
   * 
   * @see java.lang.Object#hashCode()
   */
  public int hashCode()
  {
    int result = 0;
    
    for (int i : _components)
    {
      result += i;
    }
    
    return result;
  }
  
  /* ------------------------------------------------------------------------ */
  /* equals method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object obj)
  {
    if (obj == this) return true;
    if (obj == null) return false;
    
    if (obj instanceof Version)
    {
      return (compareTo((Version)obj) == 0);
    }
    
    return false;
  }
  
  /* ------------------------------------------------------------------------ */
  /* getMajorVersion method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * @return the major version represented by this version number.
   */
  public int getMajorVersion()
  {
    return _components[0];
  }

  /* ------------------------------------------------------------------------ */
  /* toString method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method returns the string form of this version. The result of this method
   * can be used to create a new Version object, such that: 
   * 
   * <pre>
   *   Version v = new Version(old.toString());
   * </pre>
   * 
   * works.
   * 
   * @see java.lang.Object#toString()
   */
  public String toString()
  {
    StringBuilder builder = new StringBuilder();
    
    for (int i : _components)
    {
      builder.append(i);
      builder.append('.');
    }
    
    builder.deleteCharAt(builder.length() - 1);
    
    return builder.toString();
  }
  
  /* ------------------------------------------------------------------------ */
  /* toComponents method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method returns the components of this version as an array of ints.
   * The result of this method can be used to create a new Version object, such that:
   * <pre>
   *   Version v = new Version(old.toComponentns());</pre>
   * works.
   * 
   * @return the version components.
   */
  public int[] toComponents()
  {
    int[] ver = new int[_components.length];
    
    // Note an array copy is required in order to ensure that this class is immutable.
    System.arraycopy(_components, 0, ver, 0, _components.length);
    
    return ver;
  }
  
  /* ------------------------------------------------------------------------ */
  /* clone method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * @see java.lang.Object#clone()
   */
  public Version clone()
  {
    return new Version(_components);
  }

  /* ------------------------------------------------------------------------ */
  /* writeExternal method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
   */
  public void writeExternal(ObjectOutput out) throws IOException
  {
    out.writeLong(serialVersionUID);
    
    out.writeInt(_components.length);
    
    for (int i : _components)
    {
      out.writeInt(i);
    }
    
    out.flush();
  }

  /* ------------------------------------------------------------------------ */
  /* readExternal method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
   */
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
  {
    if (_readOnly)
    {
      throw new IOException();
    }
    
    _readOnly = true;
    
    in.readLong(); // Read the serial version UID.
    
    int length = in.readInt();
    
    int[] components = new int[length];
    
    for (int i = 0; i < components.length; i++)
    {
      components[i] = in.readInt();
    }
    
    _components = components;
  }
}