/*******************************************************************************
 * Copyright (c) 1997, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.servlet.cache.DynamicContentProvider; //MSI
import com.ibm.ws.cache.web.ExternalCacheFragment;

import com.ibm.wsspi.cache.GenerateContents;

/**
 * This class holds the internal representation of an entry,
 * which has links to contained fragments instead of expanding them.
 * A FragmentComposerMemento has a ArrayList of either:
 * <ul>
 *     <li>A character array: For HTML static content.
 *     <li>A MementoEntry: For a contained entry.
 *         This contains information needed to get the entry's
 *         cache id (in case it is in the cache) or to execute
 *         the entry (in case it is not in the cache).
 * </ul>
 * This representation has the following advantages over an expanded
 * representation:
 * <ul>
 *     <li>It is more compact to save memory.
 *     <li>The invalidation of an entry does not cause its containing
 *         entry/s to be invalidated.
 *     <li>LRU statistics about a contained fragment are more accurate.
 * </ul>
 * This is the value that is cached in a Cache (an internal cache).
 * This class also knows how to expand the internal representation
 * into a character array, which is what is cached in an external cache.
 */
public class FragmentComposerMemento implements Serializable, GenerateContents {
   private static final long serialVersionUID = 1342185474L;
   private static TraceComponent tc = Tr.register(FragmentComposerMemento.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");
   private static int traceCount = 0;

   /**
    * This holds the entire state of the FragmentComposerMemento.
    * It is the sequence of entries that make up an entry.
    * Its elements are either a character array or a MementoEntry.
    */
   private Object contents[];
   private boolean consumeSubfragments = false;
   private boolean containsESI=false;

   /**
    * The external cache fragment.
    */
   private ExternalCacheFragment externalCacheFragment = null;

   /**
    * The external cache group id.
    */
   private String externalCacheGroupId = null;

   private int outputStyle = FragmentComposer.NONE;
   private String contentType = null;
   private String characterEncoding = null;

   //we can't store these in the contents list b/c attribute bytes could be confused with output bytes.
   private CacheProxyRequest.Attribute attributes[] = null;
   private byte[] attributeBytes = null;

   /**
    * This gets the consumeSubfragments variable.
    *
    * @return The consumeSubfragments variable.
    */
   public boolean getConsumeSubfragments() {
      return consumeSubfragments;
   }

   /**
    * This sets the consumeSubfragments variable.
    *
    * @para, The consumeSubfragments variable.
    */
   public void setConsumeSubfragments(boolean consumeSubfragments) {
      this.consumeSubfragments = consumeSubfragments;
   }

   public void setContainsESIContent(boolean b) {
      containsESI=b;
   }

   public void setOutputStyle(int outputStyle) {
      this.outputStyle=outputStyle;
   }
   
   public void setContentType(String contentType) {
      this.contentType = contentType;
   }
   
   public void setCharacterEncoding(String charEnc) {
	   this.characterEncoding = charEnc;
   }

   public boolean getContainsESIContent() {
      return containsESI;
   }

   public void addContents(Object contents[]) {
      this.contents = contents;
   }


   /**
    * This allows a set of attributes modified by the associated cache entry to be appended to the memento.
    *
    */
   public void addAttributes(CacheProxyRequest.Attribute changedAttrs[]) {
      this.attributes = changedAttrs;
   }

   /**
    * This allows a set of attributes modified by the associated cache entry to be appended to the memento.
    *
    */
   public void addAttributeBytes(byte[] changedAttrBytes) {
      this.attributeBytes = changedAttrBytes;
   }

   /**
    * This displays a page, recursively expanding the contained fragments.
    *
    * @param request The HttpServletRequest object.
    * @param response The HttpServletResponse object.
    */
   public void displayPage(Servlet s, CacheProxyRequest request, CacheProxyResponse response) throws ServletException, IOException {
      int number = -1;
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
         number = traceCount++;
         Tr.debug(tc, "displayPage BEGINS " + number);
      }
      if (containsESI)
         response.setContainsESIContent(containsESI);
         
      if (contentType != null)
	  response.setContentType(contentType);   
      
      if (!response.isCommitted()) {
    	  response.setHeader("CACHED_RESPONSE", "true");
      }	  
      
      if(characterEncoding != null)
      {
          response.setCharacterEncoding(characterEncoding);
          if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
              Tr.debug(tc,"setting response charEncoding to " + characterEncoding);
      }
			   
      OutputStream outputStream = null;
      if (FragmentComposer.BYTE == outputStyle) {       
          outputStream = response.getOutputStream();
      }                                                 
      PrintWriter printWriter = null;
      if (FragmentComposer.CHAR == outputStyle) {       
          printWriter = response.getWriter();
      }                                                 
      
      displayFragment(s, request, response, outputStream, printWriter, contents);
      
      if (attributes != null) {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "setting last unready attributes: ", (Object) attributes);
         request.setAttributeTableUnReadied(attributes);
         request.readyAttributes();
      } else if (attributeBytes != null) {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "setting last unready attribute bytes");
         request.setAttributeTableBytes(attributeBytes);
         request.readyAttributes();
      }
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
         Tr.debug(tc, "displayPage ENDS " + number);
   }
   
   public void displayFragment(Servlet s, CacheProxyRequest request, CacheProxyResponse response, OutputStream outputStream, PrintWriter printWriter, Object[] contents) throws ServletException, IOException {
       for (int i = 0; i < contents.length; i++) {
    	   
           Object object = contents[i];
			if (object instanceof byte[]) {
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
					Tr.debug(tc, " writing array =\'"+ new String((byte[]) object) + "\'");
				if (outputStream == null && printWriter == null)
					outputStream = response.getOutputStream();
				if (outputStream != null)
					outputStream.write((byte[]) object);
				else if (printWriter != null)
					printWriter.write(new String((byte[]) object));
			} else if (object instanceof char[]) {
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
					Tr.debug(tc, " writing array =\'"+ new String((char[]) object) + "\'");
				if (printWriter == null && outputStream == null)
					printWriter = response.getWriter();
				if (printWriter != null)
					printWriter.write((char[]) object);
				else if (outputStream != null)
					outputStream.write((new String((char[]) object)).getBytes());
			} else if (object instanceof String) {
				if (consumeSubfragments && response.getResponse().isCommitted()) {
					if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
						Tr.event(tc,"Cannot set content-type header. Response already committed");
					}
				}else 
					response.setContentType((String) object);

           } else if (object instanceof ResponseSideEffect) {
        	   ResponseSideEffect responseSideEffect = (ResponseSideEffect) object;
				if (consumeSubfragments && response.getResponse().isCommitted()) {
					if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
						Tr.event(tc, "Cannot set header. Response already committed. Header was: " + responseSideEffect.toString());
					}
				} else {
					responseSideEffect.performSideEffect(response);
				}
           } else if (object instanceof DynamicContentProvider) {				     
  	          DynamicContentProvider dynamicContentProvider = (DynamicContentProvider) object;
  	          if (outputStyle == FragmentComposer.BYTE)
  	              dynamicContentProvider.provideDynamicContent(request, outputStream);	
  	          if (outputStyle == FragmentComposer.CHAR)
  	              dynamicContentProvider.provideDynamicContent(request, printWriter);	
           }
           else {									     
              MementoEntry mementoEntry = (MementoEntry) object;
              if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                 Tr.debug(tc, " handling mementoEntry with template: " + mementoEntry.getTemplate());
              
              //It's much more efficient only to de-serialize attributes when needed (cache miss)
              byte[] bytes = mementoEntry.getAttributeTableBytes();
              if (bytes != null) {
                 request.setAttributeTableBytes(bytes);
              } else {
                 request.setAttributeTableUnReadied(mementoEntry.getAttributeTable());
              }
              
              if (!consumeSubfragments || (consumeSubfragments && mementoEntry.getDoNotConsume())) { //don't execute children if we cached their output in this entry
                 if (mementoEntry.getInclude() || mementoEntry.isAsync()) {
                 	if (mementoEntry.getNamedDispatch()){	
                 		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
             				Tr.debug(tc, "Using NamedDispatcher for servletName:" + mementoEntry.getTemplate() + " contextPath: " + mementoEntry.getContextPath());
                 		
                 		if (mementoEntry.getContextPath() != null) {
                 			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                 				Tr.debug(tc, "Including across webapps to " + mementoEntry.getContextPath());
                 			
                 			if (s instanceof ServletWrapper)
                 				((ServletWrapper) s).getServletContext().getContext(mementoEntry.getContextPath()).getNamedDispatcher(mementoEntry.getTemplate()).include(request, response);
                 			else
                 				((HttpServlet) s).getServletContext().getContext(mementoEntry.getContextPath()).getNamedDispatcher(mementoEntry.getTemplate()).include(request, response);
                 		} else {
                 			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                 				Tr.debug(tc, "including in same webapp to " + mementoEntry.getContextPath());
                 			
                 			if (s instanceof ServletWrapper)
                 				((ServletWrapper) s).getServletContext().getNamedDispatcher(mementoEntry.getTemplate()).include(request, response);
                 			else
                 				((HttpServlet) s).getServletContext().getNamedDispatcher(mementoEntry.getTemplate()).include(request, response);
                 		}
                 	} else { //context path is non null if the memento entry is in a different web module                 		
                 		if (mementoEntry.getContextPath() != null) {
                 			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                 				Tr.debug(tc, "Including across webapps to " + mementoEntry.getContextPath());
                 			
                 			if (s instanceof ServletWrapper)
                 				((ServletWrapper) s).getServletContext().getContext(mementoEntry.getContextPath()).getRequestDispatcher(mementoEntry.getTemplate()).include(request, response);
                 			else
                 				((HttpServlet) s).getServletContext().getContext(mementoEntry.getContextPath()).getRequestDispatcher(mementoEntry.getTemplate()).include(request, response);
                 		} else {
                 			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                 				Tr.debug(tc, "including in same webapp to " + mementoEntry.getContextPath());
                 			request.getRequestDispatcher(mementoEntry.getTemplate()).include(request,response);
                 		}
                 	}
                 } else {
                 		if (mementoEntry.getNamedDispatch()){
                 			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                 				Tr.debug(tc, "Using NamedDispatcher for servletName:" + mementoEntry.getTemplate() + " contextPath: " + mementoEntry.getContextPath());
                 			
                 			if (mementoEntry.getContextPath() != null) {
                 				if (s instanceof ServletWrapper)
                 					((ServletWrapper) s).getServletContext().getContext(mementoEntry.getContextPath()).getNamedDispatcher(mementoEntry.getTemplate()).forward(request, response);
                 				else
                 					((HttpServlet) s).getServletContext().getContext(mementoEntry.getContextPath()).getNamedDispatcher(mementoEntry.getTemplate()).forward(request, response);
                 			} else {
                     			if (s instanceof ServletWrapper)
                     				((ServletWrapper) s).getServletContext().getNamedDispatcher(mementoEntry.getTemplate()).forward(request, response);
                     			else
                     				((HttpServlet) s).getServletContext().getNamedDispatcher(mementoEntry.getTemplate()).forward(request, response);
                 			}	
                 		}
                 		else{
                 			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                 				Tr.debug(tc, "Using RequestDispatcher for servletName:" + mementoEntry.getTemplate() + " contextPath: " + mementoEntry.getContextPath());
                 			
                 			if (mementoEntry.getContextPath() != null) {
                 				if (s instanceof ServletWrapper)
                 					((ServletWrapper) s).getServletContext().getContext(mementoEntry.getContextPath()).getRequestDispatcher(mementoEntry.getTemplate()).forward(request, response);
                 				else
                 					((HttpServlet) s).getServletContext().getContext(mementoEntry.getContextPath()).getRequestDispatcher(mementoEntry.getTemplate()).forward(request, response);
                 			} else {
                 				request.getRequestDispatcher(mementoEntry.getTemplate()).forward(request,response);
                 			}
                 		}
                 }
              } else{
                  request.readyAttributes();
                  displayFragment(s, request, response, outputStream, printWriter, mementoEntry.getContents());
              }
              
              //handle end attributes
              CacheProxyRequest.Attribute eAttributes[] = mementoEntry.getEndAttributeTable();
              byte eAttributeBytes[] =mementoEntry.getEndAttributeTableBytes();
              if (eAttributes != null) {
            	  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            		  Tr.debug(tc, "setting last unready attributes: " + eAttributes);
                  request.setAttributeTableUnReadied(eAttributes);
                  request.readyAttributes();
              } else if (eAttributeBytes != null) {
                  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                     Tr.debug(tc, "setting last unready attribute bytes");
                  request.setAttributeTableBytes(eAttributeBytes);
                  request.readyAttributes();
              }
               
           }
        }  
   }
     
   /**
    * This displays a page without expanding the contained fragments.
    * It is useful for debugging and demos.
    *
    * @param request The HttpServletRequest object.
    * @param response The HttpServletResponse object.
    */
   public void viewContents(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
      OutputStream outputStream = null;
      PrintStream printStream = null;
      PrintWriter printWriter = null;

      for (int i = 0; i < contents.length; i++) {
         Object object = contents[i];
         if (object instanceof byte[]) {
            if (outputStream == null)
               outputStream = response.getOutputStream();
            outputStream.write((byte[]) object);
            continue;
         }
         if (object instanceof char[]) {
            if (printWriter == null)
               printWriter = response.getWriter();
            printWriter.write((char[]) object);
            continue;
         }
         if (object instanceof String) {
            if (outputStyle == FragmentComposer.CHAR) {
               if (printWriter == null)
                  printWriter = response.getWriter();
               printWriter.println("<xmp>setContentType: " + object + "</xmp>");
            } else {
               if (outputStream == null)
                  outputStream = response.getOutputStream();
               if (printStream == null)
                  printStream = new PrintStream(outputStream);
               printStream.println("<xmp>setContentType: " + object + "</xmp>");
            }
            continue;
         }

         if (object instanceof ResponseSideEffect) {
            if (outputStyle == FragmentComposer.CHAR) {
               if (printWriter == null)
                  printWriter = response.getWriter();
               printWriter.println("<xmp>sideEffect: " + object + "</xmp>");
            } else {
               if (outputStream == null)
                  outputStream = response.getOutputStream();
               if (printStream == null)
                  printStream = new PrintStream(outputStream);
               printStream.println("<xmp>sideEffect: " + object + "</xmp>");
            }
            continue;
         }
	 if (object instanceof DynamicContentProvider) {		    //MSI-begin
				if (outputStyle == FragmentComposer.CHAR) {
					if (printWriter == null)
					printWriter = response.getWriter();
					printWriter.println("<xmp>[DynamicContentProvider: " + object.getClass() + "]</xmp><br>");
				} else {
					if (outputStream == null)
						outputStream = response.getOutputStream();
					if (printStream == null)
						printStream = new PrintStream(outputStream);
					printStream.println("<xmp>[DynamicContentProvider: " + object.getClass() + "]</xmp><br>");
				}
			continue;
	  } 									    //MSI-end
         if (!(object instanceof MementoEntry)) {
            throw new IllegalStateException("each element in contents should be a String, " + "ResponseSideEffect, or a MementoEntry");
         }
         MementoEntry mementoEntry = (MementoEntry) object;
         if (outputStyle == FragmentComposer.CHAR) {
            if (printWriter == null)
               printWriter = response.getWriter();
            CacheProxyRequest.Attribute tmp[] = mementoEntry.getAttributeTable();
            if (tmp != null) {
               printWriter.println("<xmp>[modified request attributes: " + tmp + "]<br></xmp>");
            }
	    if (consumeSubfragments && !mementoEntry.getDoNotConsume())
               printWriter.println("<xmp>[CONSUMED include: " + mementoEntry.getTemplate() + "]</xmp><br>");
            else
               printWriter.println("<xmp>[include: " + mementoEntry.getTemplate() + "]</xmp><br>");
         } else {
            if (outputStream == null)
               outputStream = response.getOutputStream();
            if (printStream == null)
               printStream = new PrintStream(outputStream);
            CacheProxyRequest.Attribute tmp[] = mementoEntry.getAttributeTable();
            if (tmp != null) {
               printStream.println("<xmp>[modified request attributes: " + tmp + "]</xmp><br>");
            }
	    if (consumeSubfragments && !mementoEntry.getDoNotConsume())
               printStream.println("<xmp>[CONSUMED include: " + mementoEntry.getTemplate() + "]</xmp><br>");
            else
               printStream.println("<xmp>[include: " + mementoEntry.getTemplate() + "]</xmp><br>");
         }
      }

      if (attributes != null) {
         if (printStream != null) {
            printStream.println("<xmp>[modified request attributes: " + attributes + "]</xmp><br>");
         } else {
            if (printWriter == null)
               printWriter = response.getWriter();
            printWriter.println("<xmp>[modified request attributes: " + attributes + "]</xmp><br>");
         }
      } else if (attributeBytes != null) {
         if (printStream != null) {
            printStream.println("<xmp>[modified request attributes (serialized for distribution): " + attributeBytes + "]</xmp><br>");
         } else {
            if (printWriter == null)
               printWriter = response.getWriter();
            printWriter.println("<xmp>[modified request attributes (serialized for distribution): " + attributeBytes + "]</xmp><br>");
         }
      }
   }

   /**
    * This calculates a page's display without expanding the contained fragments.
    * It is useful for debugging and demos.
    *
    * @param request The HttpServletRequest object.
    * @param response The HttpServletResponse object.
    */
   public byte[] generateContents() {

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      PrintStream printStream = new PrintStream(baos);

      generateFragment(baos, printStream, contents);

      if (attributes != null) {
         printStream.println("<xmp>[modified request attributes: " + attributes + "]</xmp><br>");
      } else if (attributeBytes != null) {
         printStream.println("<xmp>[modified request attributes (serialized for distribution): ");
         printStream.write(attributeBytes, 0, attributeBytes.length);
         printStream.println("]</xmp><br>");
      }
      return baos.toByteArray();
   }
   
   private void generateFragment(ByteArrayOutputStream baos, PrintStream printStream, Object[] contents){
       for (int i = 0; i < contents.length; i++) {
           Object object = contents[i];
           if (object instanceof byte[]) {
              try {
                 printStream.write((byte[]) object);
              } catch (Exception e) {
                 com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.cache.servlet.FragmentComposerMemento.generateContents", "384", this);
                 Tr.error(tc, "dynacache.error", e.getMessage());
                 if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "error generating contents in FCM: " + e.getMessage());
              }
              continue;
           }
           if (object instanceof char[]) {
              try {
                 OutputStreamWriter osw = new OutputStreamWriter(baos, "UTF8");
                 osw.write((char[]) object);
                 osw.flush();
              } catch (Exception e) {
                 com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.cache.servlet.FragmentComposerMemento.generateContents", "395", this);
                 Tr.error(tc, "dynacache.error", e.getMessage());
                 if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "error generating contents in FCM: " + e.getMessage());
              }
              continue;
           }
           if (object instanceof String) {
              printStream.println("<xmp>setContentType: " + object + "</xmp>");
              continue;
           }
           if (object instanceof ResponseSideEffect) {
              printStream.println("<xmp>sideEffect: " + object + "</xmp>");
              continue;
           }
           if (object instanceof DynamicContentProvider) {    //MSI-begin
               try {
                   printStream.println("<xmp>[DynamicContentProvider: " + object.getClass() + "]</xmp><br>");
                   continue;
               } catch (Exception e) {
                   com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.cache.servlet.FragmentComposerMemento.generateContents", "395", this);
                   Tr.error(tc, "dynacache.error", e.getMessage());
                   if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                       Tr.debug(tc, "error generating contents in FCM: " + e.getMessage());
               }
               continue;
           }							   //MSI-end
           if (!(object instanceof MementoEntry)) {
              throw new IllegalStateException("each element in contents ArrayList should be a char/byte array, " + "ResponseSideEffect, or a MementoEntry");
           }
           MementoEntry mementoEntry = (MementoEntry) object;
           if (consumeSubfragments && !mementoEntry.getDoNotConsume()){
              printStream.println("<xmp>[CONSUMED include: " + mementoEntry.getTemplate() + "]</xmp><br>");
	      generateFragment(baos, printStream, mementoEntry.contents);
	     }
           else
              printStream.println("<xmp>[include: " + mementoEntry.getTemplate() + "]</xmp><br>");
        }
   }

   /**
    * This gets the externalCacheFragment variable.
    *
    * @return The externalCacheFragment variable.
    */
   public ExternalCacheFragment getExternalCacheFragment() {
      return externalCacheFragment;
   }

   /**
    * This sets the externalCacheFragment variable.
    *
    * @param externalCacheFragment The externalCacheFragment.
    */
   public void setExternalCacheFragment(ExternalCacheFragment externalCacheFragment) {
      this.externalCacheFragment = externalCacheFragment;
   }

   /**
    * This gets the external cache group id.
    *
    * @return The external cache group id.
    */
   public String getExternalCacheGroupId() {
      return externalCacheGroupId;
   }

   /**
    * This sets the external cache group id.
    *
    * @param externalCacheGroupId The external cache group id.
    */
   public void setExternalCacheGroupId(String externalCacheGroupId) {
      this.externalCacheGroupId = externalCacheGroupId;
   }

   /**
    * This overrides the method in object of FragmentComposerMenento
    * It returns the hashCode of the contents.
    *
    * @return The hashCode.
    */
	@Override
	public int hashCode()  {
	     int hc = 0;
	     for (int i = 0; i < contents.length; i++) {
		    Object object = contents[i];
		    if (object instanceof byte[]) {	
			    hc += object.hashCode();
			    continue;
		    } else if (object instanceof char[]) {		      
			    hc += object.hashCode();	
			    continue;
		    } else if (object instanceof String) {
		        continue;
		    } else if (object instanceof ResponseSideEffect) {
		        continue;
		    } else if (object instanceof DynamicContentProvider) {
			    continue;
		    } else if (object instanceof MementoEntry) {
			    //FragmentComposer
			    MementoEntry mementoEntry = (MementoEntry) object;
			    if (consumeSubfragments && !mementoEntry.getDoNotConsume()){
			       hc += mementoEntry.hashCode();
			   }
		    }
	     }
	     return hc;
	}
}
