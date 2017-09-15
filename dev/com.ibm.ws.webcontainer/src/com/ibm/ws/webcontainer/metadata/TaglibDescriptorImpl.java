/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.metadata;

import javax.servlet.descriptor.TaglibDescriptor;

public class TaglibDescriptorImpl implements TaglibDescriptor {

    private String location = null;
    private String uri = null;
    
    public TaglibDescriptorImpl(String loc, String u) {
        location = loc;
        uri = u;
    }
    
    @Override
    public String getTaglibLocation() {
        return location;
    }

    @Override
    public String getTaglibURI() {
        return uri;
    }

}
