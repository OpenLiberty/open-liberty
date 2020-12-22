/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jdbc.fat.v41.errormap.driver;

import java.sql.Connection;

public interface ErrorMapConnection extends Connection {

    public void setNextErrorCode(int errorCode);

    public void setNextSqlState(String sqlState);

}
