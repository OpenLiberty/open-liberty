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
package com.ibm.ws.jsp.runtime;

import javax.servlet.ServletContext;
import javax.servlet.jsp.tagext.Tag;

import com.ibm.ws.jsp.taglib.annotation.AnnotationHandler;

public class TagArray {
    private Tag[] tags;

    private int next = -1;
    
    private AnnotationHandler tagAnnotationHandler;
    
    public TagArray(int size, ServletContext context) {
        tags = new Tag[size];
        
        // LIDB4147-24
        
        this.tagAnnotationHandler = AnnotationHandler.getInstance
             (context);
    }

    public Tag get() {
        Tag tag = null;
        if (next >= 0) {
            tag = tags[next--];
        }
        return tag;
    }

    public void put(Tag tag) {
        if (next < (tags.length - 1)) {
            tags[++next] = tag;
            return;
        }
        
        this.tagAnnotationHandler.doPreDestroyAction (tag);   // LIDB4147-24
        
        tag.release();
    }

    public void releaseTags() {
        for (int i = 0; i < tags.length; i++) {
            if (tags[i] != null) {
                this.tagAnnotationHandler.doPreDestroyAction (tags[i]);   // LIDB4147-24
                 
                tags[i].release();
                tags[i] = null;
            }
        }
    }
    
    public int numberInUse() {
        return next+1;
    }
}
