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
package com.ibm.ws.jsp.tsx.tag;

// defect 229195: add support for obtaining index variable inside of tsx:repeat tag.
// PK63483  sartoris    04/01/2008  tsx:repeat tag not breaking out of loop when iterating through query results

import java.io.IOException;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.servlet.jsp.tagext.TryCatchFinally;

public class RepeatTag extends BodyTagSupport implements TryCatchFinally{
    /**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 3257568395246778423L;
	static protected Logger logger;
	private static final String CLASS_NAME="com.ibm.ws.jsp.tsx.tag.RepeatTag";
    static {
        logger = Logger.getLogger("com.ibm.ws.jsp");
    }

	
    private String index = "";
    private int start = 0;
    // PQ73915.1 - end default
    // private int end = 0;
    private int end = Integer.MAX_VALUE;
    // PQ73915.1
    private int currentIteration = 0;
    
    private StringBuffer internalBodyContent;
    private boolean breakTsxRepeatLoop = false;
    

    public String getIndex() {
        return (index);
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public int getStart() {
        return (start);
    }

    public void setStart(int start) {
        // PQ73915.5 - start must >= 0
        // this.start = start;
        this.start = (start > 0) ? start : 0;
        // PQ73915.5
    }

    public int getEnd() {
        return (end);
    }

    public void setEnd(int end) {
        // PQ73915.2 - end must >= start
        // this.end = end;    	
        this.end = (end >= start) ? end : Integer.MAX_VALUE;
        // PQ73915.2
    }

    public RepeatTag() {}

    public int doStartTag() throws JspException {
    	this.internalBodyContent = new StringBuffer();
    	
        DefinedIndexManager indexMgr = (DefinedIndexManager) pageContext.getAttribute("TSXDefinedIndexManager", PageContext.PAGE_SCOPE);
        if (indexMgr == null) {
            indexMgr = new DefinedIndexManager();
            pageContext.setAttribute("TSXDefinedIndexManager", indexMgr, PageContext.PAGE_SCOPE);
        }
        Stack repeatStack = (Stack) pageContext.getAttribute("TSXRepeatStack", PageContext.PAGE_SCOPE);
        if (repeatStack == null) {
            repeatStack = new Stack();
            pageContext.setAttribute("TSXRepeatStack", repeatStack, PageContext.PAGE_SCOPE);
        }

        Hashtable repeatLookup = (Hashtable) pageContext.getAttribute("TSXRepeatLookup", PageContext.PAGE_SCOPE);
        if (repeatLookup == null) {
            repeatLookup = new Hashtable();
            pageContext.setAttribute("TSXRepeatLookup", repeatLookup, PageContext.PAGE_SCOPE);
        }
        if (index == null || index.equals("")) {
            index = indexMgr.getNextIndex();
        }
        else {
            if (indexMgr.exists(index) == true) {
                throw new JspException("Index specified in <tsx:repeat> tag has already been defined. index =[" + index + "]");
            }
            else {
                indexMgr.addIndex(index);
            }
        }
        if (start > 0) {
            currentIteration = start;
        }
        pageContext.setAttribute (index, new Integer(currentIteration));

        repeatStack.push(index);
        repeatLookup.put(index, new Integer(currentIteration++));

        // PQ73915.3 - end must >= 0
        // if (end < 1) {
        if (end < 0) {
            // PQ73915.3
            end = Integer.MAX_VALUE;
        }
        
        // begin 150288: part 1: create a BodyContent writer to store temporary data created
        //						 the jsp repeat tag instead of using current JspWriter.
        //return (EVAL_BODY_INCLUDE);
        return (EVAL_BODY_BUFFERED);
        // end 150288: part 1

    }

    public int doAfterBody() throws JspException {
        Hashtable repeatLookup = (Hashtable) pageContext.getAttribute("TSXRepeatLookup", PageContext.PAGE_SCOPE);

        //PK63483 - add check for pageContext attribute that is set in GetPropertyTag
        Boolean breakRepeat = (Boolean)pageContext.findAttribute("TSXBreakRepeat");
        if (breakTsxRepeatLoop || breakRepeat != null) {
            return (SKIP_BODY);
        }
        
        BodyContent bodyContent = getBodyContent();
        String output = bodyContent.getString();
        this.internalBodyContent.append(output);
        try{
        	bodyContent.clearBuffer();
        }catch (IOException io){
        }
        
        // PQ73915.4 - includes end index
        //if ((currentIteration + 1) <= end) {
        if (currentIteration <= end) {
            // PQ73915.4
        	pageContext.setAttribute (index, new Integer(currentIteration));
            repeatLookup.put(index, new Integer(currentIteration++));
            return (EVAL_BODY_AGAIN);
        }
        else {
            return (SKIP_BODY);
        }
    }

    public int doEndTag() throws JspException {
    	doOutput(true);
        return (EVAL_PAGE);
    }

    private void doOutput(boolean writeToClient) throws JspException {
        Stack repeatStack = (Stack) pageContext.getAttribute("TSXRepeatStack", PageContext.PAGE_SCOPE);
        index = (String) repeatStack.pop();
        currentIteration = 0;

        if(writeToClient){
	        // begin 150288: part 2
	        String output = this.internalBodyContent.toString();
	        JspWriter out = pageContext.getOut();
	        try {
	            out.write(output);
	        }
	        catch (java.io.IOException io) {
	            throw new JspException("Unable to write <tsx:repeat> tag output " + io.getMessage());
	        }
	        //PK63483 - start 
	        //remove attribute from context in case of multiple tsx:repeat tags 
	        finally {
				super.pageContext.removeAttribute("TSXBreakRepeat", 2); // prepare for next repeat tag.				
			}
	        //PK63483 - end
        }

        // begin 150288: part 3 remove used repeat tag index so it can be reused when done.
        //				needed for nested <tsx:repeat> tags.
        DefinedIndexManager indexMgr = (DefinedIndexManager) pageContext.getAttribute("TSXDefinedIndexManager", PageContext.PAGE_SCOPE);
        if (indexMgr != null) {
            indexMgr.removeIndex(index);
        }
        // end 150288: part 3
        
        reset();

	}

	public void release() {
        super.release();
        reset();

    }
	/* (non-Javadoc)
	 * @see javax.servlet.jsp.tagext.TryCatchFinally#doCatch(java.lang.Throwable)
	 */
	public void doCatch(Throwable caughtException) throws Throwable {
		
		breakTsxRepeatLoop = true;
		if(caughtException instanceof ArrayIndexOutOfBoundsException || caughtException instanceof  NoSuchElementException){
			doOutput (true);	// only output for certain exceptions.
		}
		else{
			logger.logp(Level.SEVERE,CLASS_NAME, "doCatch","Caught unexpected exception in tsx:repeat. breaking loop @["+ currentIteration +"]",caughtException);
			doOutput(false);	// do not write output from tag to client.
		}

	}
	/* (non-Javadoc)
	 * @see javax.servlet.jsp.tagext.TryCatchFinally#doFinally()
	 */
	public void doFinally() {
		// TODO Auto-generated method stub

	}
	
	private void reset(){
        index = "";
        start = 0;
        // PQ73915.6 - default end index value
        // end = 0;
        end = Integer.MAX_VALUE;
        // pq73915.6
        
        this.internalBodyContent = new StringBuffer();
        breakTsxRepeatLoop = false;
        
	}
	
}
