/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.processor.matching;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.security.auth.Subject;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.security.auth.AuthUtils;
//import com.ibm.ws.sib.security.users.UserRepository;
//import com.ibm.ws.sib.security.users.UserRepositoryException;
//import com.ibm.ws.sib.security.users.UserRepositoryFactory;
//import com.ibm.ws.sib.security.users.UserRepository.Group;
//import com.ibm.ws.sib.security.users.UserRepositoryFactory.BehaviouralModifiers;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 */
public class MPPrincipal implements Principal
{
  private static final TraceComponent tc =
    SibTr.register(MPPrincipal.class, SIMPConstants.MP_TRACE_GROUP, SIMPConstants.RESOURCE_BUNDLE);

  /* Output source info */
  static {
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      SibTr.debug(tc, "Source info: @(#)SIB/ws/code/sib.processor.impl/src/com/ibm/ws/sib/processor/matching/MPPrincipal.java, SIB.processor, WASX.SIB, ff1246.02 1.18");
  }

  /** The user name */
  private String name;
  /** The groupst he user is in */
  private List<String> groups;

  public MPPrincipal(String nm)
  {
    name = nm.toLowerCase();
  }

  public MPPrincipal(String busName, Subject subject, AuthUtils utils)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      String report = "<null>";
      if (subject != null)
      {
        report = "subject(" + utils.getUserName(subject) + ")";
      }
      SibTr.entry(tc, "MPPrincipal",  new Object[] { report, utils});
    }

    if (subject != null)
    {
      name = utils.getUserName(subject).toLowerCase();
//      try
//      {
//        UserRepository rep = UserRepositoryFactory.getUserRepository(busName, BehaviouralModifiers.LAZILY_RETRIEVE_ENTITY_DATA, BehaviouralModifiers.CACHED_GROUP_DATA_ALLOWED);
//
//        Set<Group> setOfGroups = rep.getUserUsingUniqueName(name).getGroups();
//        groups = new ArrayList<String>();
//
//        for (Group group : setOfGroups)
//        {
//          groups.add(group.getSecurityName().toLowerCase());
//        }
//      }
//      catch (UserRepositoryException e)
//      {
//        // No FFDC code needed
//      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "MPPrincipal", this);
  }

  public MPPrincipal(String busName, String userName)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "MPPrincipal", new Object[] { busName, userName });

    name = userName.toLowerCase();

//    try
//    {
//      UserRepository rep = UserRepositoryFactory.getUserRepository(busName, BehaviouralModifiers.LAZILY_RETRIEVE_ENTITY_DATA, BehaviouralModifiers.CACHED_GROUP_DATA_ALLOWED);
//
//      Set<Group> setOfGroups = rep.getUserUsingUniqueName(name).getGroups();
//      groups = new ArrayList<String>();
//
//      for (Group group : setOfGroups)
//      {
//        groups.add(group.getSecurityName().toLowerCase());
//      }
//    }
//    catch (UserRepositoryException e)
//    {
//      // No FFDC code needed
//    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "MPPrincipal", this);
  }

  //------------------------------------------------------------------------------
  // Method: MPUser.equals
  //------------------------------------------------------------------------------
  /**
   *
   * Created: 99-01-19
   */
  //---------------------------------------------------------------------------
  public boolean equals(Object o){
    if (o == null) return false;
    if (o == this) return true;

    if (o.getClass() != this.getClass()) return false; // Must be the same subclass to be equal

    return ((MPPrincipal) o).name.equals(name);
  } //equals


  //------------------------------------------------------------------------------
  // Method: MPUser.toString
  //------------------------------------------------------------------------------
  /**
   *
   * Created: 99-01-19
   */
  //---------------------------------------------------------------------------
  public String toString(){
    return "Principal(" + name + ")";
  } //toString


  //------------------------------------------------------------------------------
  // Method: MPUser.hashCode
  //------------------------------------------------------------------------------
  /**
   *
   * Created: 99-01-19
   */
  //---------------------------------------------------------------------------
  public int hashCode(){
    return name.hashCode();
  } //hashCode

  //------------------------------------------------------------------------------
  // Method: MPUser.getName
  //------------------------------------------------------------------------------
  /**
   *
   * Created: 99-01-19
   */
  //---------------------------------------------------------------------------
  public String getName(){
    return name;
  } //getName

  /**
   * @return
   */
  public List getGroups()
  {
	  return groups;
  }
}
