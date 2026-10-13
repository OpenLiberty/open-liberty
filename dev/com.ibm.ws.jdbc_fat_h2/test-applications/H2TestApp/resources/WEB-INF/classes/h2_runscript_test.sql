--
-- Copyright (c) 2026 IBM Corporation and others.
-- All rights reserved. This program and the accompanying materials
-- are made available under the terms of the Eclipse Public License 2.0
-- which accompanies this distribution, and is available at
-- http://www.eclipse.org/legal/epl-2.0/
--
-- SPDX-License-Identifier: EPL-2.0
--
-- Contributors:
--     IBM Corporation - initial API and implementation
--

CREATE TABLE RUNSCRIPT_TEST (
    ID INT PRIMARY KEY,
    NAME VARCHAR(50)
);

INSERT INTO RUNSCRIPT_TEST VALUES (1, 'Alpha');
INSERT INTO RUNSCRIPT_TEST VALUES (2, 'Beta');
INSERT INTO RUNSCRIPT_TEST VALUES (3, 'Gamma');
