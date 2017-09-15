/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.internal.statemachine;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.app.manager.AppMessageHelper;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.application.handler.ApplicationHandler;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsResource;
import com.ibm.wsspi.kernel.service.utils.FileUtils;

/**
 *
 */
class DownloadFileAction implements Runnable, Action {
    private static final TraceComponent _tc = Tr.register(DownloadFileAction.class);
    private final WsLocationAdmin _locAdmin;
    private final String _servicePid;
    private final String _location;
    private final ResourceCallback _callback;
    private final AtomicBoolean _active = new AtomicBoolean(true);
    private final AtomicReference<ApplicationHandler<?>> _handler;

    public DownloadFileAction(WsLocationAdmin locAdmin, String servicePid,
                              String location, ResourceCallback callback, AtomicReference<ApplicationHandler<?>> handler) {
        _locAdmin = locAdmin;
        _servicePid = servicePid;
        _location = location;
        _callback = callback;
        _handler = handler;
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore({ MalformedURLException.class, IOException.class })
    public void run() {
        OutputStream output = null;
        InputStream input = null;
        DataOutputStream dOut = null;
        DataInputStream dIn = null;
        URLConnection urlConection = null;
        try {
            URL Url = new URL(_location);
            byte[] buf;
            int byteRead = 0;
            File downloadDir = _locAdmin.getBundleFile(this, _servicePid);

            if (!FileUtils.ensureDirExists(downloadDir)) {
                //we cannot find or build the directory - print error message and return null
                AppMessageHelper.get(_handler.get()).error("CANNOT_CREATE_DIRECTORY", downloadDir, _location);
                _callback.failedCompletion(null);
                return;
            }

            String fileName = getFileName();
            boolean doDownload = true;

            File downloadedFile = new File(downloadDir, fileName);
            File lastModifiedFile = new File(downloadDir, fileName + ".lastModified");

            urlConection = Url.openConnection();
            long lastModified = urlConection.getLastModified();

            if (downloadedFile.exists() && lastModified > 0) {
                if (lastModifiedFile.exists()) {
                    dIn = new DataInputStream(new FileInputStream(lastModifiedFile));
                    if (lastModified == dIn.readLong()) {
                        doDownload = false;
                    }
                }
            }

            output = new BufferedOutputStream(new FileOutputStream(downloadedFile));

            input = urlConection.getInputStream();
            buf = new byte[1024]; //you can change the 1024 to a different size value
            while (_active.get() && (byteRead = input.read(buf)) != -1) {
                output.write(buf, 0, byteRead);
            }
            output.flush();

            if (_active.get()) {
                if (doDownload && lastModified > 0) {
                    try {
                        dOut = new DataOutputStream(new FileOutputStream(lastModifiedFile));
                        dOut.writeLong(lastModified);
                    } catch (IOException ioe) {
                    }
                }

                Container c = _callback.setupContainer(_servicePid, downloadedFile);
                WsResource r = _locAdmin.resolveResource(downloadedFile.getAbsolutePath());
                _callback.successfulCompletion(c, r);
            }
        } catch (MalformedURLException urlException) {
            //it isn't a URL (if it is it is badly formed). This will be thrown a lot if we are given a link
            //to a file which doesn't exist in the correct location.
            _callback.failedCompletion(urlException);
        } catch (Exception exception) {
            FFDCFilter.processException(exception, DownloadFileAction.class.getName(), "downloadFail");
            Tr.warning(_tc, "DOWNLOAD_EXCEPTION_ENCOUNTERED", _location, exception.toString());
            _callback.failedCompletion(exception);
        } finally {
            close(input);
            close(output);
            close(dIn);
            close(dOut);
        }
    }

    @FFDCIgnore(IOException.class)
    private static void close(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
            }
        }
    }

    /**
     * @param _location2
     * @return
     */
    private String getFileName() {
        int index = _location.lastIndexOf('/');
        if (index != -1) {
            return _location.substring(index + 1);
        }
        return _servicePid;
    }

    /** {@inheritDoc} */
    @Override
    public void execute(ExecutorService executor) {
        executor.execute(this);
    }

    /** {@inheritDoc} */
    @Override
    public void cancel() {
        _active.set(false);
    }
}