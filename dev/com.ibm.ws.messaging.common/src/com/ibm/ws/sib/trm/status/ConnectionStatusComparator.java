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

package com.ibm.ws.sib.trm.status;

/*
 * This class compares ConnectionStatus objects
 */

public final class ConnectionStatusComparator implements java.util.Comparator {

  // If o1 comes before o2 return -1
  // If o1 == o2 return 0
  // If o1 comes after o2 return 1

  public int compare (Object o1, Object o2) {

    String re1 = ((ConnectionStatus)o1).getRemoteEngineName();
    String re2 = ((ConnectionStatus)o2).getRemoteEngineName();

    return re1.compareTo(re2);
  }

}
