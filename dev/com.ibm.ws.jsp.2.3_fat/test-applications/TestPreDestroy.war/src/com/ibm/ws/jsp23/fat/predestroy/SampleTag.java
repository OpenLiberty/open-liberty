/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp23.fat.predestroy;

import java.io.IOException;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.Tag;

/*
 * When an exception is thrown from doEndTag, we want to ensure
 * predestroy is still called (via try-finally in OL PR 22478 / PH49514). 
 * In WAS 9, this also ensures instances of this class are removed
 * from the WASAnnotationHelper map. 
 */
public class SampleTag implements Tag {
  private PageContext pageContext;
  
  private Tag parent;
  
  @PreDestroy
  public void doPreDestroy() {
    System.out.println("DESTROY CALLED for " + this.toString());
  }
  
  @PostConstruct
  public void doPostConstruct() {
    System.out.println("CONSTRUCT CALLED for " + this.toString());
  }
  
  public void setPageContext(PageContext pageContext) {
    this.pageContext = pageContext;
  }
  
  public void setParent(Tag parent) {
    this.parent = parent;
  }
  
  public Tag getParent() {
    return this.parent;
  }
  
  public int doStartTag() throws JspTagException {
    return 0;
  }
  
  public int doEndTag() throws JspTagException {  
        throw new JspTagException("Purposefully thrown Exception");
  }
  
  public void release() {}
}
