/*
 * $Header: /cvshome/wascvs/jsp/src/org/apache/jasper/runtime/PerThreadTagHandlerPool.java,v 1.1.1.1 2003/10/17 13:47:57 backhous Exp $
 * $Revision: 1.1.1.1 $
 * $Date: 2003/10/17 13:47:57 $
 *
 * ====================================================================
 * 
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights 
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer. 
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:  
 *       "This product includes software developed by the 
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written 
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */ 

package org.apache.jasper.runtime;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.Servlet;
import java.util.Hashtable;
import java.util.Enumeration;

/**
 * Pool of tag handlers that can be reused.
 * Experimental: use thread local.
 *
 * @author Jan Luehe
 * @author Costin Manolache
 */
public class PerThreadTagHandlerPool extends TagHandlerPool {

    public static int MAX_SIZE=100;

    private int maxSize=MAX_SIZE;
    private int initialSize=5;
    private ThreadLocal perThread=new ThreadLocal();
    // for cleanup
    private Hashtable threadData=new Hashtable();

    private static class PerThreadData {
        Tag handlers[];
        int current;
    }

    /**
     * Constructs a tag handler pool with the default capacity.
     */
    public PerThreadTagHandlerPool() {
        super();
    }

    protected void init(Servlet servlet) {
        String maxSizeS=TagHandlerPool.getOption(servlet.getServletConfig(), OPTION_MAXSIZE, null);
        maxSize=Integer.parseInt(maxSizeS);
        if( maxSize <0  ) {
            maxSize=MAX_SIZE;
        }
    }

    /**
     * Constructs a tag handler pool with the given capacity.
     *
     * @param capacity Tag handler pool capacity
     * @deprecated
     */
    public PerThreadTagHandlerPool(int capacity) {
        this.maxSize = capacity;
	//this.handlers = new Tag[capacity];
	//this.current = -1;
    }

    /**
     * Gets the next available tag handler from this tag handler pool,
     * instantiating one if this tag handler pool is empty.
     *
     * @param handlerClass Tag handler class
     *
     * @return Reused or newly instantiated tag handler
     *
     * @throws JspException if a tag handler cannot be instantiated
     */
    public Tag get(Class handlerClass) throws JspException {
        PerThreadData ptd=(PerThreadData)perThread.get();
        if( ptd!=null && ptd.current >=0 ) {
            return ptd.handlers[ptd.current--];
        } else {
	    try {
		return (Tag) handlerClass.newInstance();
	    } catch (Exception e) {
		throw new JspException(e.getMessage(), e);
	    }
	}
    }

    /**
     * Adds the given tag handler to this tag handler pool, unless this tag
     * handler pool has already reached its capacity, in which case the tag
     * handler's release() method is called.
     *
     * @param handler Tag handler to add to this tag handler pool
     */
    public void reuse(Tag handler) {
        PerThreadData ptd=(PerThreadData)perThread.get();

        if( ptd==null ) {
            ptd=new PerThreadData();
            ptd.handlers=new Tag[ initialSize ];
            ptd.current=0;
            threadData.put( ptd, ptd );
        }

	if (ptd.current < (ptd.handlers.length - 1)) {
	    ptd.handlers[++ptd.current] = handler;
            return;
        }

        // no more space
        if( ptd.handlers.length < maxSize ) {
            // reallocate
            Tag newH[]=new Tag[ptd.handlers.length + initialSize];
            System.arraycopy(ptd.handlers, 0, newH, 0, ptd.handlers.length);
            ptd.handlers=newH;
            ptd.handlers[++ptd.current]=handler;
            return;
        }

        //else
	handler.release();
    }

    /**
     * Calls the release() method of all available tag handlers in this tag
     * handler pool.
     */
    public synchronized void release() {
        Enumeration ptdE=threadData.keys();
        while( ptdE.hasMoreElements() ) {
            PerThreadData ptd=(PerThreadData)ptdE.nextElement();
            for (int i=ptd.current; i>=0; i--) {
                if( ptd.handlers != null && ptd.handlers[i]!=null )
                    ptd.handlers[i].release();
            }
        }
    }
}

