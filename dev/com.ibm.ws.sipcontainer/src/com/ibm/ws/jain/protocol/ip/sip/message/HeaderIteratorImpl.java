/*******************************************************************************
 * Copyright (c) 2003,2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jain.protocol.ip.sip.message;

import jain.protocol.ip.sip.SipParseException;
import jain.protocol.ip.sip.header.Header;
import jain.protocol.ip.sip.header.HeaderIterator;
import jain.protocol.ip.sip.header.HeaderParseException;

import java.util.NoSuchElementException;

import com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.message.MessageImpl.HeaderEntry;

/**
 * This class provides an Iterator over a list of Headers. There are two 
 * differences between HeaderIterator and java.util.Iterator:
 * 
 * HeaderIterator contains no remove() method
 * HeaderIterator's next() method can throw a HeaderParseException. This is 
 * because the next header's value may not have been parsed until the next()
 * method is invoked.
 * 
 * @author  Assaf Azaria, Mar 2003.
 */
abstract class HeaderIteratorImpl implements HeaderIterator
{
	/**
	 * the header node this iterator is currently pointing at.
	 * null if reached end of list.
	 */
	protected HeaderEntry m_entry;
	
	protected HeaderIteratorListener m_listener;
	
	/** constructor */
	HeaderIteratorImpl(HeaderEntry entry, HeaderIteratorListener listener) {
		m_entry = entry;
		m_listener = listener;
	}
	
	/** @see jain.protocol.ip.sip.header.HeaderIterator#hasNext() */
	public boolean hasNext() {
		return m_entry != null;
	}

	public Header remove() throws NoSuchElementException{
		HeaderEntry currentEntry = getCurrentEntry();
		if (currentEntry == null){
			throw new NoSuchElementException();
		}
		if (m_listener != null){
			m_listener.onEntryDeleted(currentEntry);
		}
		return currentEntry.getHeader();
	}

	//this gets the current entry to which the iterator is at
	protected abstract HeaderEntry getCurrentEntry();

	/**
	 * an iterator over a list of headers, that goes through the entire
	 * list of headers in the message
	 */
	static class General extends HeaderIteratorImpl {
		private enum LAST_OPERATION{HAS_NEXT,NEXT};
		
		private LAST_OPERATION lastOperation = null;
		private HeaderEntry currentHeaderEntry = null;
		/** constructor */

		General(HeaderEntry entry, HeaderIteratorListener listener) {
			super(entry,listener);
			lastOperation = null;
			currentHeaderEntry = null;
		}

		@Override
		public boolean hasNext() {
			if (lastOperation == null || lastOperation == LAST_OPERATION.NEXT){
				moveToNextParsableHeader();
			}
			lastOperation = LAST_OPERATION.HAS_NEXT;
			return currentHeaderEntry != null;
		}


		/** @see jain.protocol.ip.sip.header.HeaderIterator#next() */
		public Header next() throws HeaderParseException, NoSuchElementException {
			if (lastOperation == null || lastOperation == LAST_OPERATION.NEXT){
				moveToNextParsableHeader();
			}
			if (currentHeaderEntry == null) {
				throw new NoSuchElementException();
			}
			lastOperation = LAST_OPERATION.NEXT;
			return currentHeaderEntry.getHeader();
			
		}

		private void moveToNextParsableHeader() {
			boolean foundParsableHeader = false;
			currentHeaderEntry = null;
			while (m_entry != null && !foundParsableHeader){
				currentHeaderEntry = m_entry;
				m_entry = m_entry.getNext();
				try {
					currentHeaderEntry.getHeader().parse();
				}
				catch (SipParseException e) {
					continue;
				}
				foundParsableHeader = true;
			}
		}

		@Override
		protected HeaderEntry getCurrentEntry() {
			if (lastOperation == null){
				return null;
			}
			return currentHeaderEntry;
		}
	}

	/**
	 * an iterator over a list of headers, that goes through headers
	 * of same name, and stops when it reaches a header with a different name
	 */
	static class Specific extends General {
		/** constructor */
		Specific(HeaderEntry entry, HeaderIteratorListener listener) {
			super(entry,listener);
		}
		
		/** @see jain.protocol.ip.sip.header.HeaderIterator#next() */
		public Header next() throws HeaderParseException, NoSuchElementException {
			Header header = super.next();
			HeaderEntry entry = m_entry;
			
			if (entry != null) {
				HeaderImpl h = entry.getHeader();
				if (!HeaderImpl.headerNamesEqual(h.getName(), header.getName())) {
					m_entry = null;
				}
			}
			return header; 
		}
	}

	/**
	 * an iterator over a list of headers, with no attempt to parse each header
	 */
	static class Unparsed extends HeaderIteratorImpl {
		/** constructor */
		Unparsed(HeaderEntry entry, HeaderIteratorListener listener) {
			super(entry,listener);
		}
		
		/** @see jain.protocol.ip.sip.header.HeaderIterator#next() */
		public Header next() throws HeaderParseException, NoSuchElementException {
			if (m_entry == null) {
				throw new NoSuchElementException();
			}
			HeaderImpl header = m_entry.getHeader();
			m_entry = m_entry.getNext();
			return header;
		}

		@Override
		protected HeaderEntry getCurrentEntry() {
			if (m_entry == null){
				return null;
			}
			return m_entry.getPrev();
		}
	}
	
	/**
	 * an iterator over a list of a specific header, with no attempt to parse each header
	 */
	static class UnparsedSpecific extends Unparsed {
		/** constructor */
		UnparsedSpecific(HeaderEntry entry, HeaderIteratorListener listener) {
			super(entry,listener);
		}

		/** @see jain.protocol.ip.sip.header.HeaderIterator#next() */
		public Header next() throws HeaderParseException, NoSuchElementException {
			if (m_entry == null) {
				throw new NoSuchElementException();
			}
			//get the current header from the current entry
			HeaderImpl currHeader = m_entry.getHeader();
			//get the next entry
			HeaderEntry nextEntry = m_entry.getNext();

			//in this part we forward the iterator to the next entry
			//if the next entry exists (is not null) and is not of the same header name 
			//we set it with a null, because for this case, we're done
			if (nextEntry != null  && !HeaderImpl.headerNamesEqual(currHeader.getName(), nextEntry.getHeader().getName())) {
				m_entry = null;
			}else{
				//we get here if the next entry is null or if it is of the same header name
				m_entry = nextEntry;
			}
			
			return currHeader; 
		}
	}

	static public interface HeaderIteratorListener {
		public void onEntryDeleted(HeaderEntry headerEntry);
	}

}
