/*******************************************************************************
 * Copyright (c) 2010, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.contributor;

import com.ibm.wsspi.artifact.factory.contributor.ArtifactContainerFactoryContributor;

/**
 * This is the old "internal" interface, retained for back compatibility.
 * <p>
 * As part of promoting this internal interface to be accessible as spi, it
 * had to be repackaged, and is now the ArtifactContainerFactoryContributor interface.
 * which this interface extends, allowing both to be accepted.
 */
public interface ArtifactContainerFactoryHelper extends ArtifactContainerFactoryContributor {}
