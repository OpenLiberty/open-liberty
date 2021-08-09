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
package com.ibm.ws.sib.processor.impl;

import javax.security.auth.Subject;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.security.auth.AuthUtils;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * @author Neil Young
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class SecurityContext 
{
  private static final TraceComponent tc =
    SibTr.register(SecurityContext.class, SIMPConstants.MP_TRACE_GROUP, SIMPConstants.RESOURCE_BUNDLE);

  /* Output source info */
  static {
    if (tc.isDebugEnabled())
      SibTr.debug(tc, "Source info: ");
  }

  /** Support for authorisation utilities */
  private AuthUtils authorisationUtils = null;  
  private Subject subject = null;
  private String userId = null;
  private String alternateUser = null;
  private String discriminator = null;
  private JsMessage msg = null;
        
  public SecurityContext(String userId,
                         String discriminator) 
  {
    if (tc.isEntryEnabled()) 
      SibTr.entry(tc, "SecurityContext", 
        new Object[]{"userid(" +userId + ")", discriminator});
            
    this.userId = userId;
    this.discriminator = discriminator;
    
    if (tc.isEntryEnabled()) 
      SibTr.exit(tc, "SecurityContext", this);      
  }  

  public SecurityContext(Subject subject,
                         String alternateUser,
                         String discriminator,
                         AuthUtils authorisationUtils) 
  {
    if (tc.isEntryEnabled())
    {
      String report = "<null>";
      if (subject != null)
      { 
        report = "subject(" + authorisationUtils.
                                getUserName(subject) + ")";
      }
      SibTr.entry(tc, "SecurityContext", new Object[]{report, alternateUser, discriminator, authorisationUtils});
    }         
    
    this.authorisationUtils = authorisationUtils;        
    this.subject = subject;
    this.alternateUser = alternateUser;
    this.discriminator = discriminator;
    
    if (tc.isEntryEnabled()) 
      SibTr.exit(tc, "SecurityContext", this);      
  }

  public SecurityContext(JsMessage msg,
                         String alternateUser,
                         String discriminator,
                         AuthUtils authorisationUtils) 
  {
    if (tc.isEntryEnabled()) 
      SibTr.entry(tc, "SecurityContext", 
        new Object[]{"Message(" + msg + ")", alternateUser, discriminator});
    
    this.msg = msg;
    this.authorisationUtils = authorisationUtils;  
    this.alternateUser = alternateUser;
    this.discriminator = discriminator;
    
    if (tc.isEntryEnabled()) 
      SibTr.exit(tc, "SecurityContext", this);      
  }
      
  /** toString method 
   * @return String representation of the subscription state
   */
  public String toString()
  {
    String report = null;
    
    if(isSubjectBased())
    {     
      if(authorisationUtils != null)
      {
        report = "subject(" + authorisationUtils.getUserName(subject) + ")";
      }      
    }
    else if(isUserIdBased())
    {       
      report = "id(" + userId + ")";
    }
    else if(isMsgBased())
    {       
      report = "msg(" + msg.getSecurityUserid() + ")";
    }
    
    if(alternateUser != null)
    {
      report = report + ", alt user(" + alternateUser + ")";     
    }
    
    if(discriminator != null)
    {
      report = report + ", discriminator(" + discriminator + ")";     
    }    
    
    return report;
  }  
    
  /**
   * @return
   */
  public String getDiscriminator() 
  {
	  return discriminator;
  }

  /**
   * @return
   */
  public boolean testDiscriminatorAtCreate() 
  {
	  return discriminator != null;
  }

  public boolean isSubjectBased() 
  {
      return subject != null;
  }

  public boolean isUserIdBased() 
  {
      return userId != null;
  }

  public boolean isAlternateUserBased() 
  {
      return alternateUser != null;
  }
  
  public boolean isMsgBased() 
  {
      return msg != null;
  }

/**
 * @return
 */
public Subject getSubject() {
	return subject;
}

/**
 * @return
 */
public String getUserId() {
	return userId;
}

/**
 * @return
 */
public String getAlternateUser() {
  return alternateUser;
}

/**
 * @param subject
 */
public void setSubject(Subject subject) {
	this.subject = subject;
}

/**
 * @param string
 */
public void setUserId(String string) {
	userId = string;
}

/**
 * @param string
 */
public void setDiscriminator(String string) {
	discriminator = string;
}

  /**
   * @return
   */
  public JsMessage getMsg() {
    return msg;
  }
 
  /**
   * Extract the appropriate username from this security context 
   * @return
   */
  public String getUserName(boolean notAlternateUser) 
  {
    if (tc.isEntryEnabled()) 
       SibTr.entry(tc, "getUserName", 
         new Object[]{new Boolean(notAlternateUser)});
                   
    String userName = null;
    
    if(!notAlternateUser // this catches the case where we want the
                         // user associated with the subject
       && isAlternateUserBased())                             
    {       
      userName = alternateUser;
    }
    else if(isSubjectBased())
    {     
      if(authorisationUtils != null)
      {
        userName = authorisationUtils.getUserName(subject);
      }      
    }
    else if(isUserIdBased())
    {       
      userName = userId;
    }
    else if(isMsgBased())
    {       
      userName = msg.getSecurityUserid();
    }
 
    if (tc.isEntryEnabled()) 
       SibTr.exit(tc, "getUserName", userName);        
    return userName;
  }
  
  /**
   * Check whether this security context belongs to the privileged SIBServerSubject
   * or not.
   * 
   * @param subject
   * @param destinationUuid
   * @param discriminatorName
   * @param operation
   * @return
   */   
   public boolean isSIBServerSubject()
   {
     if (tc.isEntryEnabled()) 
       SibTr.entry(tc, "isSIBServerSubject"); 
       
     boolean ispriv = false;
      
     // If the context is userid or alternate user based then it cannot be the
     // privileged SIBServerSubject, so we only need to check the Subject and
     // msg based contexts.     
     if(isSubjectBased())
     {     
       if(authorisationUtils != null)
       {
         ispriv = authorisationUtils.isSIBServerSubject(subject);
       }        
     }
     else if(isMsgBased())
     {     
       if(authorisationUtils != null)
       {
         ispriv = authorisationUtils.sentBySIBServer(msg);
       }             
     }

     if (tc.isEntryEnabled())
       SibTr.exit(tc, "isSIBServerSubject", new Boolean(ispriv));

     return ispriv; 
   }       
}
