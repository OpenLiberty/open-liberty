/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.webcontainer.metadata;

import java.util.List;
import java.util.Map;

public class BaseJspComponentMetaData {
    protected List jspPropertyGroups = null;
    protected Map jspTaglibs = null;

    public List getJspPropertyGroups() {
        return jspPropertyGroups;
    }
    
    public Map getJspTaglibs() {
        return jspTaglibs;
    }

    public void setJspPropertyGroups(List jspPropertyGroups) {
        this.jspPropertyGroups = jspPropertyGroups;
    }

    public void setJspTaglibs(Map jspTagLibs) {
        this.jspTaglibs = jspTagLibs;
    }

}
