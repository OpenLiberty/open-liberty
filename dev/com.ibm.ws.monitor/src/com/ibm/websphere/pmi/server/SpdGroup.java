/*******************************************************************************
 * Copyright (c) 1997, 2002 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.pmi.server;

import java.util.*;

public interface SpdGroup extends SpdData {
    public boolean addSorted(SpdData data); /* in a sorted order */

    public boolean add(SpdData data); /* add - not sorted */

    public boolean remove(SpdData data);

    public Iterator members();

    public void updateAggregate();
}
