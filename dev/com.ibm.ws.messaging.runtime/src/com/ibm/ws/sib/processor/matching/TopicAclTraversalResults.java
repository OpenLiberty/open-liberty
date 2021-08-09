/*******************************************************************************
 * Copyright (c) 2004, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.processor.matching;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

import java.security.Principal;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.security.BusSecurityConstants;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * @author Neil Young
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class TopicAclTraversalResults 
{
  // Standard trace boilerplate

  private static final TraceComponent tc =
    SibTr.register(
      TopicAclTraversalResults.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  // Special security Groups
  MPGroup everyone = new MPGroup(BusSecurityConstants.EVERYONE);
  MPGroup allAuthenticated = new MPGroup(BusSecurityConstants.ALLAUTHENTICATED);
  /**Lists of user ACLs
   */
  public List levelUsersAllowedToSubscribe = new ArrayList();
  public List levelUsersAllowedToPublish = new ArrayList();

  public List accumUsersAllowedToSubscribe = new ArrayList();
  public List accumUsersAllowedToPublish = new ArrayList();
  /**Lists of group ACLs
   */
  
  public List levelGroupAllowedToSubscribe = new ArrayList();
  public List levelGroupAllowedToPublish = new ArrayList();
  
  public List accumGroupAllowedToSubscribe = new ArrayList();
  public List accumGroupAllowedToPublish = new ArrayList();
  public void reset() 
  {
     (accumUsersAllowedToSubscribe).clear();
     (accumUsersAllowedToPublish).clear();
     (accumGroupAllowedToSubscribe).clear();
     (accumGroupAllowedToPublish).clear();
  }

  /**
   * Method consolidate
   * Used to add a set of acls to the accumulated permission vectors
   *  
   * @param targets  A list of topic acls
   */  
  public void consolidate(List targets) 
  {
    if (tc.isEntryEnabled()) SibTr.entry(tc, "consolidate", targets);
     Iterator itr = targets.iterator();
     
     // Reset the lists for this level
     levelUsersAllowedToSubscribe.clear();
     levelUsersAllowedToPublish.clear();
     levelGroupAllowedToSubscribe.clear();
     levelGroupAllowedToPublish.clear();
     
     while (itr.hasNext())
     {
       Object target = itr.next();
       if (target instanceof TopicAcl)
       {
         // Extract the principal
         Principal principal = ((TopicAcl)target).getPrincipal();
         // Check the operation type
         if(((TopicAcl)target).getOperationType() == 1)  
         { 
           if(principal == null)
           {
             // This is an inheritance blocker
             accumUsersAllowedToPublish.clear();
             accumGroupAllowedToPublish.clear();
           }
           else if(principal instanceof MPGroup)
           {
             levelGroupAllowedToPublish.add(principal);
           }
           else
           {
             levelUsersAllowedToPublish.add(principal);                     
           }
         }
         else
         { 
           if(principal == null)
           {
               // This is an inheritance blocker
               accumUsersAllowedToSubscribe.clear();
               accumGroupAllowedToSubscribe.clear();
           }
           else if(principal instanceof MPGroup)
           {
             levelGroupAllowedToSubscribe.add(principal);
           }
           else
           {
             levelUsersAllowedToSubscribe.add(principal);                     
           }
         }
       }
     }
     
     if(!levelUsersAllowedToSubscribe.isEmpty())
     {
       accumUsersAllowedToSubscribe.addAll(levelUsersAllowedToSubscribe);
     }
     if(!levelUsersAllowedToPublish.isEmpty())
     {
       accumUsersAllowedToPublish.addAll(levelUsersAllowedToPublish);
     }    
     if(!levelGroupAllowedToSubscribe.isEmpty())
     {
       accumGroupAllowedToSubscribe.addAll(levelGroupAllowedToSubscribe);
     }    
     if(!levelGroupAllowedToPublish.isEmpty())
     {
       accumGroupAllowedToPublish.addAll(levelGroupAllowedToPublish);
     } 
     
     if (tc.isEntryEnabled()) SibTr.exit(tc, "consolidate");        
  }

  /**
   * Method checkPermission
   * Used to determine whether a user is allowed to perform a particular
   * operation.
   * 
   * @param user  The user
   * @param operation The subscribe or publish operation
   * 
   * @return boolean The result of the permission check. 
   */  
  public boolean checkPermission(Principal user, int operation) 
  {  
    if (tc.isEntryEnabled()) 
      SibTr.entry(tc, "checkPermission", new Object[]{user, new Integer(operation)});
    boolean allowed = false;
    
    if(operation == 1) // a publish operation
    {
      // check whether permission has been granted to special group Everyone. 
      // We can do this before we check whether the user is authenticated
      if(accumGroupAllowedToPublish.size() > 0
         && accumGroupAllowedToPublish.contains(everyone))
      {
        allowed = true;
      }
      // Now check whether the user is authenticated, ie we have a username
      else if (user.getName() != null 
               && user.getName().length() > 0)
      {
        // User is authenticated
                  
        // Check user list next
        if(accumUsersAllowedToPublish.size() > 0 &&
            accumUsersAllowedToPublish.contains(user))
        {
          allowed = true;
        }
        else // Now check the groups
        {
          if(accumGroupAllowedToPublish.size() > 0)
          {
            // user hasn't been granted access directly, check the groups
            // check for AllAuthenticated first     
            if (accumGroupAllowedToPublish.contains(allAuthenticated))
            {
              allowed = true;
            }
            else
            {              
              Iterator itr = accumGroupAllowedToPublish.iterator();
              while (itr.hasNext())
              {
                MPGroup group = (MPGroup)itr.next();
                if(group.isMember(user))
                {
                  allowed = true;
                  break;
                }
              } // eof while
            }
          }
        } 
      }
    }
    else // a subscribe operation
    {
      // check whether permission has been granted to special group Everyone. 
      // We can do this before we check whether the user is authenticated
      if(accumGroupAllowedToSubscribe.size() > 0
         && accumGroupAllowedToSubscribe.contains(everyone))
      {
        allowed = true;
      }
      // Now check whether the user is authenticated, ie we have a username
      else if (user.getName() != null 
               && user.getName().length() > 0)
      {
        // User is authenticated
                  
        // Check user list next        
        if(accumUsersAllowedToSubscribe.size() > 0 &&
           accumUsersAllowedToSubscribe.contains(user))
        {
          allowed = true;
        }
        else // Now check the groups
        {
          if(accumGroupAllowedToSubscribe.size() > 0)
          {
            // user hasn't been granted access directly, check the groups
            // check for AllAuthenticated first     
            if (accumGroupAllowedToSubscribe.contains(allAuthenticated))
            {
              allowed = true;
            }
            else
            {                       
              Iterator itr = accumGroupAllowedToSubscribe.iterator();
           
              while (itr.hasNext())
              {
                MPGroup group = (MPGroup)itr.next();
                if(group.isMember(user))
                {
                  allowed = true;
                  break;
                }
              }
            }
          } 
        } 
      }       
    }
    if (tc.isEntryEnabled()) SibTr.exit(tc, "checkPermission", new Boolean(allowed));
    return allowed;
  }

}
