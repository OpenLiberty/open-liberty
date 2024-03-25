/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.test.tag;

import java.io.IOException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.Tag;


public class SampleTag implements Tag {
  private PageContext pageContext;
  
  private Tag parent;
  
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
    try {
          this.pageContext.getOut().write("***");
    } catch (Exception e) {
      throw new JspTagException(e.getCause());
    }
    return 1;
  }
  
  public int doEndTag() throws JspTagException {  
    try {
          this.pageContext.getOut().write("***");
    } catch (Exception e) {
      throw new JspTagException(e.getCause());
    }
    return 0;
  }
  
  public void release() {}
}
