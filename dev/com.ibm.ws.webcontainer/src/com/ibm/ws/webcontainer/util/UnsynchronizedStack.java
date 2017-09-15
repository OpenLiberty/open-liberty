/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.util;

import java.util.LinkedList;
import java.util.NoSuchElementException;

public class UnsynchronizedStack <E> extends LinkedList<E>  {
    private static final long serialVersionUID = 3257562923390809657L;
    public UnsynchronizedStack() {
        super();
    }
    public E peek(){
        try{
            return this.getLast();
        }catch (NoSuchElementException e){
            return null;
        }
    }
    public E pop(){
        try{
            return this.removeLast();
        }catch (NoSuchElementException e){
            return null;
        }
    }
    
    public void push(E obj){
        this.add(obj);
    }
}
