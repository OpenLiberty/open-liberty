/* ============================================================================
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * ============================================================================
 */
package testjms.web;

public interface JmsBytesMessageTests {
    void testBytesMessage_writeByte() throws Exception;
    void testBytesMessage_writeBytes() throws Exception;
    void testBytesMessage_writeChar() throws Exception;
    void testBytesMessage_writeDouble() throws Exception;
    void testBytesMessage_writeFloat() throws Exception;
    void testBytesMessage_writeInt() throws Exception;
    void testBytesMessage_writeLong() throws Exception;
    void testBytesMessage_writeShort() throws Exception;
    void testBytesMessage_writeUTF() throws Exception;
}
