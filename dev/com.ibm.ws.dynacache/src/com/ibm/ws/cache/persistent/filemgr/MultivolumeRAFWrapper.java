/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.persistent.filemgr;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.AccessController;
import java.security.AccessControlContext;
import java.security.PrivilegedAction;
import java.util.Vector;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cache.DiskCacheSizeInfo;
import com.ibm.ws.cache.HTODDynacache;
import com.ibm.ws.cache.persistent.util.ProfTimer;

//
// This is effectively the same as the RandomAccessFileWrapper.  The
// difference is that no file is allowed to grow beyond the size of
// Constants.MAX_FILESIZE.  A new physical file is opened at that
// limit and I/O continues logically unbroken across the multiple
// files.  The FileManager layer is never aware of the multi-volume
// physical layout.
//

public class MultivolumeRAFWrapper implements PhysicalFileInterface, Constants, Instrumentation {

    private static TraceComponent tc = Tr.register(MultivolumeRAFWrapper.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");
    private final Vector physical = new Vector();
    private final FileManager logical;
    String base_filename;
    String mode;
    long seek;
    long volume;
    long offset;
    int type;
    DiskCacheSizeInfo diskCacheSizeInfo;

    static ProfTimer proftimer = new ProfTimer();

    public MultivolumeRAFWrapper(String filename, String mode, FileManager logical, DiskCacheSizeInfo diskCacheSizeInfo)
        throws IOException {
        this.logical = logical;
        this.base_filename = filename;
        this.mode = mode;
        this.diskCacheSizeInfo = diskCacheSizeInfo;
        this.type = DiskCacheSizeInfo.TYPE_CACHE_DATA;
        String suffix = HTODDynacache.object_suffix;
        if (filename.indexOf(HTODDynacache.dependency_suffix) > 0) {
            this.type = DiskCacheSizeInfo.TYPE_DEPENDENCY_ID_DATA;
            suffix = HTODDynacache.dependency_suffix;
        } else if (filename.indexOf(HTODDynacache.template_suffix) > 0) {
            this.type = DiskCacheSizeInfo.TYPE_TEMPLATE_DATA;
            suffix = HTODDynacache.template_suffix;
        }

        //
        // Search for all the files of this volume.  Must be contiguously numbered
        // filename.0 through filename.nn.  
        //
        // user.dir
        int vol = 0;
        boolean done = false;

        //
        // Convert to absolute filename.  Makes life easier.
        //
        File file = new File(base_filename).getAbsoluteFile();
        base_filename = file.toString();

        File parent = file.getParentFile();
        File[] files = parent.listFiles();
        int len = files.length;
        for (int i = 0; i < len; i++) {
            //
            // We allow any kind of file other than a directory
            //
            if (!files[i].isDirectory()) {
                String next = files[i].getPath();
                if (next.indexOf(suffix) > 0) {
                    int ndx = next.lastIndexOf(".");
                    String extension = next.substring(ndx + 1);
                    try {
                        //
                        // If this does not throw an exception then we now we have a file of the 
                        // form fn.nn where nn is an int, and we assume it is part of the volume group.
                        //
                        int nextvol = Integer.parseInt(extension);
                        getVolume(nextvol);
                    } catch (NumberFormatException e) {
                        // not considered an error - we simply found a file that does not have
                        // a numerical extension
                        //System.out.println("MultivolumeRAFWrapper.<init>: Skipping file " + next);
                    }
                }
            }
        }

        if (physical.size() == 0) {
            //
            // Nothing found, need to start a new one.
            //
            getVolume(0);
        }
    }

    @Override
    public String filename() {
        return base_filename;
    }

    public long cachesize() {
        return 0;
    }

    public boolean multivolume() {
        return true;
    }

    public int pagesize() {
        return 0;
    }

    public int num_volumes() {
        return physical.size();
    }

    public boolean exists(String name) {

        File checkfile = new File(name);
        return checkfile.exists();
    }

    public void setCachePolicy(int policy)
                    throws IOException {}

    // --------------------------------------------------------------------------------
    // Instrumentation
    // --------------------------------------------------------------------------------

    @Override
    public void update_read_time(long msec) {
        logical.update_read_time(msec);
    }

    @Override
    public void update_write_time(long msec) {
        logical.update_write_time(msec);
    }

    @Override
    public void increment_read_count() {
        logical.increment_read_count();
    }

    @Override
    public void increment_write_count() {
        logical.increment_write_count();
    }

    //------------------------------------------------------------------------
    // End Instrumentation
    //------------------------------------------------------------------------

    @Override
    public long length()
                    throws IOException {
        int len = physical.size();
        int answer = 0;
        for (int i = 0; i < len; i++) {
            RandomAccessFile raf = (RandomAccessFile) physical.get(i);
            if (raf != null) {
                answer += raf.length();
            }
        }
        return answer;
    }

    @Override
    public void close()
                    throws IOException {
        int len = physical.size();
        int answer = 0;
        for (int i = 0; i < len; i++) {
            RandomAccessFile raf = (RandomAccessFile) physical.get(i);
            if (raf != null) {
                raf.close();
                physical.set(i, null);
            }
        }
    }

    //
    // Flush all dirty pages to disk
    //
    @Override
    public void flush()
                    throws IOException {}

    @Override
    public void seek(long loc)
                    throws IOException {
        if (seek != loc) {
            seek = loc;
            volume = seek / MAX_FILESIZE; // ArrayList only accepts integer indexes.
            offset = seek % MAX_FILESIZE;

            final RandomAccessFile raf = getVolume(volume);
            final MultivolumeRAFWrapper mrw = this;
            IOException ie = (IOException) AccessController.doPrivileged(
                                                                       new PrivilegedAction() {
                                                                           @Override
                                                                           public Object run() {
                                                                               try {
                                                                                   raf.seek(offset);
                                                                               } catch (IOException ioe) {
                                                                                   com.ibm.ws.ffdc.FFDCFilter.processException(ioe,
                                                                                                                               "com.ibm.ws.cache.persistent.filemgr.MultivolumeRAFWrapper",
                                                                                                                               "234",
                                                                                                                               mrw);
                                                                                   return ioe;
                                                                               }
                                                                               return null;
                                                                           }
                                                                       });
            if (ie != null)
                throw ie;
        }
    }

    private void incrementSeekBy(long val)
                    throws IOException {
        //
        // This method does an internal increment of the seek counter.  It is only
        // valid to call it from within this file, and only after a physical read or
        // write has updated the real seek.  If the seek does not case a volume-fault
        // it only updats the internal seek var (because the physical read/write has
        // updated the op sys seek).
        //
        // If it does cause a volume-fault, it invokes our own "seek" method which gets 
        // the next volume and does a physical seek.
        //
        long newoffset = offset + val;
        if (newoffset >= MAX_FILESIZE) {
            seek(seek + val);
        } else {
            offset = newoffset;
            seek = (volume * (MAX_FILESIZE)) + offset;
        }
    }

    void makeVolume(long vol)
                    throws IOException {
        String fn = base_filename + "." + vol;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "makeVolume(): fileName=" + fn);
        }
        File file = new File(fn);
        RandomAccessFile raf = new RandomAccessFile(fn, mode);
        if (physical.size() < (vol + 1)) {
            physical.setSize((int) vol + 1);
        }
        physical.set((int) vol, raf);
    }

    RandomAccessFile getVolume(long vol)
                    throws IOException {
        //if (this.diskCacheSizeInfo != null && this.diskCacheSizeInfo.checkAddVolume(this.type, (int)vol) == false) {
        //    throw new IOException(com.ibm.ws.cache.HTODDynacache.DISK_CACHE_IN_GB_OVER_LIMIT_MSG);
        //}

        if (physical.size() <= vol) {
            makeVolume(vol);
        }

        RandomAccessFile answer = (RandomAccessFile) physical.get((int) vol);
        if (answer == null) {
            makeVolume(vol);
            answer = (RandomAccessFile) physical.get((int) vol);
        }

        return answer;
    }

    @Override
    public int read()
                    throws IOException {
        increment_read_count();
        proftimer.reset();
        int answer = getVolume(volume).read();
        incrementSeekBy(1);
        update_read_time(proftimer.elapsed());
        return answer;
    }

    @Override
    public int read(byte[] v)
                    throws IOException {
        increment_read_count();
        proftimer.reset();
        int answer = internalRead(v, 0, v.length);
        update_read_time(proftimer.elapsed());
        return answer;
    }

    @Override
    public int read(byte[] v, int off, int len)
                    throws IOException {
        increment_read_count();
        proftimer.reset();
        int answer = internalRead(v, off, len);
        update_read_time(proftimer.elapsed());
        return answer;
    }

    protected int internalRead(byte[] v, int off, int len)
                    throws IOException {
        int start = off;

        long spaceavailable = MAX_FILESIZE - offset;
        int current = (int) Math.min(spaceavailable, len);
        int answer = 0;

        while (current > 0) { // still something to read?
            answer += getVolume(volume).read(v, start, current); // read current segment

            len = len - current; // remaining bytes
            start = start + current; // current offset in v
            incrementSeekBy(current); // update seek by bytes just read
                                      // and maybe page in a volume

            spaceavailable = MAX_FILESIZE - offset;
            current = (int) Math.min(spaceavailable, len);
        }

        return answer;
    }

    @Override
    public int readInt()
                    throws IOException {
        increment_read_count();
        proftimer.reset();

        int len = 4;
        int answer = 0;
        if ((offset + len) <= MAX_FILESIZE) {
            answer = getVolume(volume).readInt();
            incrementSeekBy(len);
        } else {
            answer = (int) readIntOverVolume(len);
        }

        update_read_time(proftimer.elapsed());
        return answer;
    }

    @Override
    public long readLong()
                    throws IOException {
        increment_read_count();
        proftimer.reset();

        int len = 8;
        long answer = 0;
        if ((offset + len) <= MAX_FILESIZE) {
            answer = getVolume(volume).readLong();
            incrementSeekBy(len);
        } else {
            answer = readIntOverVolume(len);
        }

        update_read_time(proftimer.elapsed());
        return answer;
    }

    @Override
    public short readShort()
                    throws IOException {
        increment_read_count();
        proftimer.reset();

        int len = 2;
        short answer = 0;
        if ((offset + len) <= MAX_FILESIZE) {
            answer = getVolume(volume).readShort();
            incrementSeekBy(len);
        } else {
            answer = (short) readIntOverVolume(len);
        }
        update_read_time(proftimer.elapsed());
        return answer;
    }

    protected long readIntOverVolume(int numbytes)
                    throws IOException {
        long answer = 0;
        long count = (MAX_FILESIZE - offset);
        byte[] buf = new byte[numbytes];
        internalRead(buf, 0, numbytes);
        int shf = (numbytes - 1) * 8;
        for (int i = 0; i < numbytes; i++) {
            answer = answer | (buf[i] << shf);
            shf++;
        }

        return answer;
    }

    @Override
    public void write(byte[] v)
                    throws IOException {
        proftimer.reset();
        internalWrite(v, 0, v.length);
        increment_write_count();
        update_write_time(proftimer.elapsed());
    }

    @Override
    public void write(byte[] v, int off, int len)
                    throws IOException {
        proftimer.reset();
        internalWrite(v, 0, len);
        increment_write_count();
        update_write_time(proftimer.elapsed());
    }

    protected void internalWrite(byte[] v, int off, int len)
                    throws IOException {

        //
        // This method is sepparated out to make instrumentation easier from
        // the various routines that invoke it.
        //
        int start = off;

        long spaceavailable = MAX_FILESIZE - offset;
        int current = (int) Math.min(spaceavailable, len);

        while (current > 0) { // still something to write?
            final MultivolumeRAFWrapper mrw = this;
            final byte[] fv = v;
            final int fstart = start;
            final int fcurrent = current;
            IOException ie = (IOException) AccessController.doPrivileged(
                                                                       new PrivilegedAction() {
                                                                           /**
                                                                            * Performs the computation. This method will be called by
                                                                            * <code>AccessController.doPrivileged</code> after enabling privileges.
                                                                            * 
                                                                            * @return a class-dependent value that may represent the results of the
                                                                            *         computation. Each class that implements
                                                                            *         <code>PrivilegedAction</code>
                                                                            *         should document what (if anything) this value represents.
                                                                            * @see AccessController#doPrivileged(PrivilegedAction)
                                                                            * @see AccessController#doPrivileged(PrivilegedAction, AccessControlContext)
                                                                            */
                                                                           @Override
                                                                           public Object run() {
                                                                               try {
                                                                                   mrw.getVolume(volume).write(fv, fstart, fcurrent); // write current segment
                                                                               } catch (IOException ioe) {
                                                                                   com.ibm.ws.ffdc.FFDCFilter.processException(ioe,
                                                                                                                               "com.ibm.ws.cache.persistent.filemgr.MultivolumeRAFWrapper",
                                                                                                                               "463",
                                                                                                                               mrw);
                                                                                   return ioe;
                                                                               }
                                                                               return null;
                                                                           }
                                                                       });
            if (ie != null) {
                throw ie;
            }

            len = len - current; // remaining bytes
            start = start + current; // current offset in v
            incrementSeekBy(current); // update seek by bytes just written.  This
                                      // also switches volume if needed. cool.

            spaceavailable = MAX_FILESIZE - offset;
            current = (int) Math.min(spaceavailable, len);
        }
    }

    @Override
    public void write(int v)
                    throws IOException {
        increment_write_count();
        proftimer.reset();
        final MultivolumeRAFWrapper mrw = this;
        final int fv = v;
        IOException ie = (IOException) AccessController.doPrivileged(
                                                                   new PrivilegedAction() {
                                                                       @Override
                                                                       public Object run() {
                                                                           try {
                                                                               mrw.getVolume(mrw.volume).write(fv);
                                                                           } catch (IOException ioe) {
                                                                               com.ibm.ws.ffdc.FFDCFilter.processException(ioe,
                                                                                                                           "com.ibm.ws.cache.persistent.filemgr.MultivolumeRAFWrapper",
                                                                                                                           "499",
                                                                                                                           mrw);
                                                                               return ioe;
                                                                           }
                                                                           return null;
                                                                       }
                                                                   });

        if (ie != null) {
            throw ie;
        }
        incrementSeekBy(1);
        update_write_time(proftimer.elapsed());
    }

    @Override
    public void writeInt(int v)
                    throws IOException {
        increment_write_count();
        proftimer.reset();
        int len = 4;
        if (offset + len <= MAX_FILESIZE) {
            final MultivolumeRAFWrapper mrw = this;
            final int fv = v;
            IOException ie = (IOException) AccessController.doPrivileged(
                                                                       new PrivilegedAction() {
                                                                           @Override
                                                                           public Object run() {
                                                                               try {
                                                                                   mrw.getVolume(mrw.volume).writeInt(fv);
                                                                               } catch (IOException ioe) {
                                                                                   com.ibm.ws.ffdc.FFDCFilter.processException(ioe,
                                                                                                                               "com.ibm.ws.cache.persistent.filemgr.MultivolumeRAFWrapper",
                                                                                                                               "499",
                                                                                                                               mrw);
                                                                                   return ioe;
                                                                               }
                                                                               return null;
                                                                           }
                                                                       });
            incrementSeekBy(len);
        } else {
            writeIntOverVolume(v, len);
        }
        update_write_time(proftimer.elapsed());
    }

    @Override
    public void writeLong(long v)
                    throws IOException {
        increment_write_count();
        proftimer.reset();
        int len = 8;
        if (offset + len <= MAX_FILESIZE) {
            final MultivolumeRAFWrapper mrw = this;
            final long fv = v;
            IOException ie = (IOException) AccessController.doPrivileged(
                                                                       new PrivilegedAction() {
                                                                           @Override
                                                                           public Object run() {
                                                                               try {
                                                                                   mrw.getVolume(mrw.volume).writeLong(fv);
                                                                               } catch (IOException ioe) {
                                                                                   com.ibm.ws.ffdc.FFDCFilter.processException(ioe,
                                                                                                                               "com.ibm.ws.cache.persistent.filemgr.MultivolumeRAFWrapper",
                                                                                                                               "545",
                                                                                                                               mrw);
                                                                                   return ioe;
                                                                               }
                                                                               return null;
                                                                           }
                                                                       });

            if (ie != null) {
                throw ie;
            }
            incrementSeekBy(len);
        } else {
            writeIntOverVolume(v, len);
        }
        update_write_time(proftimer.elapsed());
    }

    @Override
    public void writeShort(short v)
                    throws IOException {
        increment_write_count();
        proftimer.reset();
        int len = 2;
        if (offset + len <= MAX_FILESIZE) {
            final MultivolumeRAFWrapper mrw = this;
            final short fv = v;
            IOException ie = (IOException) AccessController.doPrivileged(
                                                                       new PrivilegedAction() {
                                                                           @Override
                                                                           public Object run() {
                                                                               try {
                                                                                   mrw.getVolume(mrw.volume).writeShort(fv);
                                                                               } catch (IOException ioe) {
                                                                                   com.ibm.ws.ffdc.FFDCFilter.processException(ioe,
                                                                                                                               "com.ibm.ws.cache.persistent.filemgr.MultivolumeRAFWrapper",
                                                                                                                               "545",
                                                                                                                               mrw);
                                                                                   return ioe;
                                                                               }
                                                                               return null;
                                                                           }
                                                                       });

            if (ie != null) {
                throw ie;
            }
            incrementSeekBy(len);
        } else {
            writeIntOverVolume(v, len);
        }
        update_write_time(proftimer.elapsed());
    }

    protected void writeIntOverVolume(long v, int numbytes)
                    throws IOException {

        byte[] bytes = new byte[numbytes];
        int shift = (numbytes - 1) * 8; // number of bits to shift when
                                        // "bytifying" v
        for (int i = 0; i < numbytes; i++) {
            bytes[i] = (byte) ((v & (0xFF << shift)) >> shift);
            shift = shift - 8;
        }

        internalWrite(bytes, 0, numbytes);
    }

}
