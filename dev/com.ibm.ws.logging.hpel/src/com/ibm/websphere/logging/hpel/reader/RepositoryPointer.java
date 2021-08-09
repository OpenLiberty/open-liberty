/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.logging.hpel.reader;

import java.io.Serializable;

/**
 * A pointer to a location in an HPEL repository.
 * <p>
 * The RepositoryPointer points to an exact point in the repository and can be
 * used in queries to RepositoryReaders.  Implementations of the 
 * RepositoryPointer interface will vary based on the characteristics of the
 * repository.  For example, a file based repository could use a file name and
 * file offset in the repository pointer, whereas a database based repository
 * could use a primary key value in the repository pointer.  As such, there are
 * no methods defined on the RepositoryPointer interface. 
 * <p> 
 * Instances of this interface are obtained from 
 * {@link RepositoryLogRecordHeader} and can be used in 
 * {@link RepositoryReader} requests.
 * 
 * @ibm-api
 */
public interface RepositoryPointer extends Serializable {
}
