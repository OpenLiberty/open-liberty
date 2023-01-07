/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jbatch.jms.internal;

import static java.util.Collections.unmodifiableSet;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

public class BatchOperationGroup {
    final Set<String> groupNames;
    
    public BatchOperationGroup(String...groups) {
        this.groupNames = unmodifiableSet(new TreeSet<>(Arrays.asList(groups)));
    }
    
    public Set<String> getGroupNames(){
        return unmodifiableSet(groupNames);
    }
    
    @Override
    public String toString() {
        return super.toString() + groupNames;
    }
}
