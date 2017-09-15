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
package com.ibm.ws.fat.jcdi.util.html;

import java.nio.CharBuffer;
import java.util.HashMap;
import java.util.ArrayList;

public class ConversationsHtmlParser extends HtmlDefaultParser {
	
	protected String body;
	
	protected ConversationsHtmlEventHandler conversationsHandler;
	
	protected HashMap<String, String> PageInfo = new HashMap<String, String>();
	protected ArrayList<String> items = new ArrayList<String>(); 

	public void reset(String body) {
		this.body = body;
		this.reset(new ConversationsHtmlEventHandler());
	}

	public ConversationsHtmlParser(String body)
	{
		this.body = body;
		this.reset(new ConversationsHtmlEventHandler());
	}

	public String getPageInfo(String attr)
	{
		return PageInfo.get(attr);
	}
	
	public String getTagAttributeNameBasedOnValue(String value)
	{
		for(String key : PageInfo.keySet()) {
			if (PageInfo.get(key).equals(value)) return key;
		}
		return null;
	}
	
	public String getItem(int index)
	{
		if (index >= items.size())
		{
			return null;
		}
		return items.get(index);
	}

	public int getItemListSize()
	{
		return items.size();
	}
	
	public void parse() throws Exception
	{	ArrayList<CharBuffer> output = new ArrayList<CharBuffer>();
		CharBuffer cb = CharBuffer.wrap(body);
		super.parse(new CharBuffer[] {cb}, output);
	}
	
	public class ConversationsHtmlEventHandler implements HtmlDefaultEventHandler {

		public void reset() {
		}
		
		public void startDoc() {
		}
		
		/**
		 * handle a html tag.
		 */
		public CharBuffer handleTag(CharBuffer buffer) {
			HtmlTag tag = new HtmlTag(buffer);

			tag.parseAttributes();
			
			
			// view id
			if (tag.getAttributeValue("name") != null 
				&& tag.getAttributeValue("name").equals("form"))
			{
				String value = tag.getAttributeValue("action");
				System.out.println("handleTag() finds action=" + value);
				if (value != null)
					PageInfo.put("action", value);
					int index = value.indexOf("?cid=");
					if (index > 0) {
						PageInfo.put("cid", value.substring(index)); 
						System.out.println("handleTag() finds cid=" + value.substring(index));
					}
			}

			// view id
			if (tag.getAttributeValue("name") != null 
				&& tag.getAttributeValue("name").equals("javax.faces.ViewState"))
			{
				String value = tag.getAttributeValue("value");
				if (value != null)
					PageInfo.put("javax.faces.ViewState", value);
					System.out.println("handleTag() finds javax.faces.ViewState=" + value);
			}
			
			// submite form
			if (tag.getAttributeValue("name") != null 
					&& tag.getAttributeValue("name").equals("form_SUBMIT"))
			{
				String value = tag.getAttributeValue("value");
				if (value != null) {
					PageInfo.put("form_SUBMIT", value);
					System.out.println("handleTag() finds form_SUBMIT=" + value);
				}
			}

			// parse item id.
			if (tag.getAttributeValue("href") != null 
					&& tag.getAttributeValue("href").equals("#")
				//&& tag.getAttributeValue("onclick").startsWith("return oamSubmitForm("))
				&& tag.getAttributeValue("onclick").startsWith("return myfaces.oam.submitForm"))
			{
				String value = tag.getAttributeValue("onclick");
				int start = value.indexOf("form:");
				if (start > 0) {
					int end = value.indexOf("\')", start);
					if (end > start) {
						PageInfo.put("", value);
						items.add(value.substring(start, end));
						System.out.println("handleTag() finds item=" + value.substring(start, end));
					}
				}
			}

			// Login
			if (tag.getAttributeValue("value") != null 
				&& tag.getAttributeValue("value").equals("Login"))
			{
				String name = tag.getAttributeValue("name");
				String value = tag.getAttributeValue("value");
				System.out.println("handleTag() finds Login tag name=" + name);
				if (value != null)
					PageInfo.put(name, value);
			}

			return buffer;
		}
		/**
		 * Handle DTD.
		 */
		public CharBuffer handleDTD(CharBuffer buffer) {
			return buffer;
		}
		
		
		/**
		 * Handle contents.
		 */
		public CharBuffer handleContents(CharBuffer buffer) {
			return buffer;
		}
		
		/**
		 * Handle comments.
		 */
		public CharBuffer handleComments(CharBuffer buffer) {
			return buffer;
		}
		
		public CharBuffer[] flush() {
			return null;
		}
		
		public void endDoc() {
			
		}
			
		public boolean isFinished() {
			return false;
		}

	}

}
