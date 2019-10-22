/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jbatch.jms.internal;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BatchOperationGroup {

    HashSet<String> groupNames = new HashSet<String>();
    
    public void addGroup(String anotherGroup){
    	groupNames.add(anotherGroup);
    }

    public void removeGroup(String anotherGroup){
    	groupNames.remove(anotherGroup);
    }
    
    public Set<String> getGroupNames(){
        return groupNames;
    }
    
}
