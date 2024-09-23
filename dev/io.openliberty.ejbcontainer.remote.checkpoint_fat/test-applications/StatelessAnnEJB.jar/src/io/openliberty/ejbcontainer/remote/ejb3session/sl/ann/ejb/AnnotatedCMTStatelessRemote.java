/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.ejbcontainer.remote.ejb3session.sl.ann.ejb;

import javax.ejb.Remote;

/**
 * Remote interface for basic Container Managed Transaction Stateless Session
 * bean. Annotated as a remote business interface
 **/
@Remote
public interface AnnotatedCMTStatelessRemote {
    public void tx_ADefault();

    public void tx_ARequired();

    public void tx_ANotSupported();

    public void tx_ARequiresNew();

    public void tx_ASupports();

    public void tx_ANever();

    public void tx_AMandatory();
}