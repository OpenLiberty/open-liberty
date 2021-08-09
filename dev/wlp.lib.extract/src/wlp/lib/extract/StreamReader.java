/*******************************************************************************
 * Copyright (c) 2015, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package wlp.lib.extract;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

class StreamReader extends Thread {
    InputStream is;
    String type;
    OutputStream os;

    StreamReader(InputStream is, String type) {
        this(is, type, null);
    }

    StreamReader(OutputStream os, String type) {
        this.os = os;
        this.type = type;

    }

    StreamReader(InputStream is, String type, OutputStream redirect) {
        this.is = is;
        this.type = type;
        this.os = redirect;
    }

    public void run() {
        try {
            // stdin, process stream is output
            if (type.equals("INPUT")) {
                runOutputStream();
            }
            // else stdout, stderr, process stream is input
            else {
                runInputStream();
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public void runInputStream() throws IOException {
        PrintWriter pw = null;
        if (os != null)
            pw = new PrintWriter(os);

        InputStreamReader isr = new InputStreamReader(is, "UTF-8");
        BufferedReader br = new BufferedReader(isr);
        String line = null;
        while ((line = br.readLine()) != null) {
            if (pw != null)
                pw.println(line);
            System.out.println(line);

        }

        if (pw != null)
            pw.flush();
    }

    public void runOutputStream() throws IOException {
        OutputStreamWriter osr = new OutputStreamWriter(os, "UTF-8");
        BufferedWriter br = new BufferedWriter(osr);
        br.write("Y");
    }
}
