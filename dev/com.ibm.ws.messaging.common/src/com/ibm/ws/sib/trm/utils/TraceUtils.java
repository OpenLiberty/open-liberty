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
package com.ibm.ws.sib.trm.utils;

import javax.security.auth.Subject;

/**
 * Utility class for tracing purposes.
 */
public final class TraceUtils 
{
   /**
    * @param subject
    * @return a stringified Subject. Needed as Subject.toString is a privileged action.
    */
   public static final String subjectToString(final Subject subject)
   {
      String subj = "<null>";
      if (subject != null)
      {
        subj = "Subject hashcode=0x" + Integer.toHexString(subject.hashCode());
      }
      return subj;
   }
}
