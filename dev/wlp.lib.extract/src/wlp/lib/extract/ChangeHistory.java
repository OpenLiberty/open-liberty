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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;

/**
 * Record a log of changes made
 */
public final class ChangeHistory {

    interface Action {
        public ReturnCode execute();
    }

    private final Stack history = new Stack();

    public void createdFile(final String filePath) {
        history.push(new Action() {
            // Deletes created file
            public ReturnCode execute() {
                final File file = new File(filePath);
                if (file.delete()) {
                    return ReturnCode.OK;
                } else {
                    return new ReturnCode(ReturnCode.BAD_INPUT);
                }
            }
        });
    }

    public void deletedFile(final String filePath, final byte[] originalFile) {
        history.push(new Action() {
            // Recreates Original File
            public ReturnCode execute() {
                FileOutputStream stream = null;
                try {
                    stream = new FileOutputStream(filePath);
                    stream.write(originalFile);
                    return ReturnCode.OK;
                } catch (IOException e) {
                    return new ReturnCode(ReturnCode.BAD_INPUT);
                } finally {
                    SelfExtractUtils.tryToClose(stream);
                }
            }
        });
    }

    // map: key is property name, value [] is of {old value, new value}
    public void changedProps(final String filePath, final Map props) {
        history.push(new Action() {
            public ReturnCode execute() {
                final File file = new File(filePath);
                final Properties propsObj = new Properties();
                InputStream is = null;
                OutputStream os = null;
                try {
                    is = new FileInputStream(file);
                    propsObj.load(is);

                    // Restore old property values.
                    Iterator entries = props.entrySet().iterator();
                    while (entries.hasNext()) {
                        Map.Entry entry = (Map.Entry) entries.next();
                        String key = (String) entry.getKey();
                        String value = ((String[]) entry.getValue())[0];
                        propsObj.setProperty(key, value);
                    }

                    os = new FileOutputStream(file);
                    propsObj.store(os, null);
                    return ReturnCode.OK;
                } catch (IOException ioe) {
                    return new ReturnCode(ReturnCode.BAD_INPUT);
                } finally {
                    SelfExtractUtils.tryToClose(is);
                    SelfExtractUtils.tryToClose(os);
                }
            }
        });
    }

    // Attempts to undo changes in reverse order of actions taken;
    public ReturnCode rollback() {
        while (!history.isEmpty()) {
            final Action action = (Action) history.pop();
            final ReturnCode ret = action.execute();
            if (ret.getCode() != 0) {
                return ret;
            }
        }
        return ReturnCode.OK;
    }
}
