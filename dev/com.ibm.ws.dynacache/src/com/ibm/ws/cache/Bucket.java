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
package com.ibm.ws.cache;

final class Bucket extends Queue {

	Element findByKey(Object key) {
		for (QueueElement e = head; e != null; e = e.next) {
			final Element el = (Element) e;
			if (key.equals(el.key)) {
				return el;
			}
		}

		return null;
	}

	Element replaceByKey(Object key, Object object) {
		final Element element = removeByKey(key);
		addToTail(new Element(key, object));
		return element;
	}

	void addByKey(Object key, Object object) {
		addToTail(new Element(key, object));
	}

	Element removeByKey(Object key) {
		final Element e = findByKey(key);
		if (e != null) {
			remove(e);
		}
		return e;
	}

}
