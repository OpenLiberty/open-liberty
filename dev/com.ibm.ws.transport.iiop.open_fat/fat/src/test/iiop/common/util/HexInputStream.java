/*******************************************************************************
 * Copyright (c) 2015-2023 IBM Corporation and others.
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
package test.iiop.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

public class HexInputStream extends InputStream {
    private final StringReader hex;
    private int available;

    public HexInputStream(String hex) throws IOException {
        this.hex = new StringReader(hex);
        this.available = hex.length() / 2;
    }

    @Override
    public int read() throws IOException {
        available--;
        char[] byteChars = new char[2];
        int bytesRead = hex.read(byteChars);
        if (bytesRead == -1)
            return -1;
        if (bytesRead != 2)
            throw new IOException("IOR string did not contain an even number of hex digits");
        return ((getNybble(byteChars[0]) << 4) | getNybble(byteChars[1]));
    }

    @Override
    public int available() throws IOException {
        return available;
    }

    int getNybble(char ch) throws IOException {
        switch (ch) {
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                return ch - '0';
            case 'a':
            case 'b':
            case 'c':
            case 'd':
            case 'e':
            case 'f':
                return ch - 'a' + 0xa;
            case 'A':
            case 'B':
            case 'C':
            case 'D':
            case 'E':
            case 'F':
                return ch - 'A' + 0xA;
            default:
                throw new IOException("IOR string contains non-hex characters");
        }
    }

}