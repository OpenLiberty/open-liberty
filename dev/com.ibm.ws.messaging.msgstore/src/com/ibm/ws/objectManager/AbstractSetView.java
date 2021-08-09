package com.ibm.ws.objectManager;

/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/**
 * A starter implementation of the Set interface. This does not
 * create a new Set and does not extend ManagedObject.
 * 
 * @see Set
 * @see AbstractCollectionView
 * @see AbstractSet
 */
public abstract class AbstractSetView
                extends AbstractCollectionView
                implements Set {
    /**
     * Default no argument constructor.
     */
    protected AbstractSetView()
    {} // AbstractSetView().
} // AbstractSetView.