/*******************************************************************************
 * Copyright (c) 2009, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.hpel.impl;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Timer;

import com.ibm.ws.logging.hpel.GenericOutputFile;

/**
 * Helper class to have unified way to use privileged operation independenty of the
 * execution environment - WebSphere or plain JVM.
 */
public abstract class AccessHelper {
	private static abstract class FlexibleAccessController {
		abstract <T> T doPrivileged(PrivilegedAction<T> action);
		abstract <T> T doPrivileged(PrivilegedExceptionAction<T> action)
				throws PrivilegedActionException;
	};
	
	private static FlexibleAccessController proxyController = null;
	
	private final static String WS_ACCESS_CONTROLLER = "com.ibm.ws.security.util.AccessController";
	private final static String DO_PRIVILEGED_METHOD = "doPrivileged";
	
	private static FlexibleAccessController getInstance() {
		if (proxyController == null) {
			try {
				Class<?> accessControllerClass = Class.forName(WS_ACCESS_CONTROLLER);
				if (accessControllerClass != null) {
					final Method method1 = accessControllerClass.getMethod(DO_PRIVILEGED_METHOD, new Class[]{PrivilegedAction.class});
					final Method method2 = accessControllerClass.getMethod(DO_PRIVILEGED_METHOD, new Class[]{PrivilegedExceptionAction.class});
					proxyController = new FlexibleAccessController() {
						@SuppressWarnings("unchecked")
						@Override
						<T> T doPrivileged(PrivilegedAction<T> action) {
							try {
								return (T)method1.invoke(null, new Object[] {action});
							} catch (InvocationTargetException ex) {
								if (ex.getCause() instanceof RuntimeException) {
									throw (RuntimeException)ex.getCause();
								}
								throw new RuntimeException(ex.getCause());
							} catch (Exception ex) {
								throw new RuntimeException(ex);
							}
						}
						@SuppressWarnings("unchecked")
						@Override
						<T> T doPrivileged(PrivilegedExceptionAction<T> action) throws PrivilegedActionException {
							try {
								return (T)method2.invoke(null, new Object[] {action});
							} catch (InvocationTargetException ex) {
								if (ex.getCause() instanceof PrivilegedActionException) {
									throw (PrivilegedActionException)ex.getCause();
								} else if (ex.getCause() instanceof RuntimeException) {
									throw (RuntimeException)ex.getCause();
								}
								throw new RuntimeException(ex.getCause());
							} catch (Exception ex) {
								throw new RuntimeException(ex);
							}
						}
					};
				}
			} catch (ClassNotFoundException ex) {
				// Fall through to use Java implementation.
			} catch (NoSuchMethodException ex) {
				// Fall through to use Java implementation.
			}
			if (proxyController == null) {
				proxyController = new FlexibleAccessController() {
					@Override
					<T> T doPrivileged(PrivilegedAction<T> action) {
						return AccessController.doPrivileged(action);
					}
					@Override
					<T> T doPrivileged(PrivilegedExceptionAction<T> action) throws PrivilegedActionException {
						return AccessController.doPrivileged(action);
					}
				};
			}
		}
		return proxyController;
	}

	/**
	 * Using privileged java security, determine if the specified File denotes a file (as
	 * opposed to a directory).
	 * <p>
	 * @param file a <code>File</code> object whose abstract pathname corresponds
	 *  to the pathname to check.
	 * @return true if and only if the abstract pathname corresponds to a file. Otherwise
	 * false. If a SecurityException is encountered, the exception is logged and absorbed
	 * and false is returned.
	 */
	static boolean isFile(final File file)
	{
		try {
			Boolean isFile = getInstance().doPrivileged(
					new PrivilegedAction<Boolean>() {
						public Boolean run() {
							return Boolean.valueOf(file.isFile());
						}
					}
			);
			return isFile.booleanValue();
		}
		catch (SecurityException se) {
			// TODO: Add logging here but be careful since this code is used in logging logic itself
			//       and may result in an indefinite loop.
			return false;
		}
	}

	/**
	 * Using privileged java security, determine if the abstract pathname for the specified
	 * file is a directory.
	 * <p>
	 * @param file a <code>File</code> object whose abstract pathname is to be checked.
	 * @return true if and only if the abstract pathname is a directory. Otherwise false. If a
	 * SecurityException is encountered, the exception is logged and absorbed and false is returned.
	 */
	static boolean isDirectory(final File file)
	{
		try {
			Boolean exists = getInstance().doPrivileged(
					new PrivilegedAction<Boolean>() {
						public Boolean run() {
							return Boolean.valueOf(file.isDirectory());
						}
					}
			);
			return exists.booleanValue();
		}
		catch (SecurityException se) {
			// TODO: Add logging here but be careful since this code is used in logging logic itself
			//       and may result in an indefinite loop.
			return false;
		}
	}

	   /**
	   * Using privileged java security determine the length of an existing file.
	   * <p>
	   * @param fileToCheck a non-null <code>File</code> object whose abstract pathname
	   *  corresponds to the physical file to check. The caller must guarantee that passed
	   *  object is non null and that the physical file exists.
	   * @return the length, in bytes of the file. 0 is returned if the file does not exist.
	   *  If a SecurityException is encountered, the exception is logged, absorbed and 0 is
	   *  returned.
	   */
	  static long getFileLength(final File fileToCheck)
	  {
	    try {
	      Long size = getInstance().doPrivileged(
	        new PrivilegedAction<Long>() {
	          public Long run() {
	            return Long.valueOf(fileToCheck.length());
	          }
	        }
	      );
	      return size.longValue();
	    }
	    catch (SecurityException se) {
			// TODO: Add logging here but be careful since this code is used in logging logic itself
			//       and may result in an indefinite loop.
	    	return 0L;
	    }
	  }

	  /**
	   * Using privileged java security, create the directory named by the abstract pathname in the
	   * specified <code>File</code>, including any intermediate and nonexistent directories.
	   * <p>
	   * @param dirToMake a non-null <code>File</code> object whose abstract pathname represents
	   *  the fully qualified directory to create.
	   * @return true is returned if and only if all necessary directories were created. Otherwise
	   *  false is returned. If a SecurityException is encountered, the exception is logged and
	   *  absorbed and false is returned.
	   */
	  static boolean makeDirectories(final File dirToMake)
	  {
	    try {
	      Boolean result = getInstance().doPrivileged(
	        new PrivilegedAction<Boolean>() {
	          public Boolean run() {
	            return Boolean.valueOf(dirToMake.mkdirs());
	          }
	        }
	      );
	      return result.booleanValue();
	    }
	    catch (SecurityException se) {
	    	// TODO: Add logging here but be careful since this code is used in logging logic itself
	    	//       and may result in an indefinite loop.
	    	return false;
	    }
	  }

	  static boolean canMakeDirectories(final File dirToMake) throws IllegalArgumentException {
		  if (dirToMake == null) {
			  throw new IllegalArgumentException("Parameter dirToMake cannot be null");
		  }
		  try {
			  Boolean result = getInstance().doPrivileged(
					  new PrivilegedAction<Boolean>() {
						  public Boolean run() {
							  File curDir = dirToMake.getAbsoluteFile();
							  do {
								  if (curDir.isDirectory()) {
									  if (curDir.canWrite()) {
										  return Boolean.TRUE;
									  }
									  throw new IllegalArgumentException("Cannot create directories in \"" + dirToMake + "\" since \"" + curDir + "\" is not a writable directory");
								  } else if (curDir.exists()) {
									  throw new IllegalArgumentException("Cannot create directories in \"" + dirToMake + "\" since \"" + curDir + "\" exists and is not a directory");
								  }
								  curDir = curDir.getParentFile();
							  } while (curDir != null);
							  throw new IllegalArgumentException("Cannot create directories in \"" + dirToMake + "\" since none of its parents exists");
						  }
					  }
			  );
			  return result.booleanValue();
		  }
		  catch (SecurityException se) {
			  // TODO: Add logging here but be careful since this code is used in logging logic itself
			  //       and may result in an indefinite loop.
			  throw new IllegalArgumentException("Cannot create directories in \"" + dirToMake + "\" due to security issue: " + se.getMessage(), se);
		  }
	  }
	  
	/**
	 * Using privileged security, create a <code>FileInputStream</code> over the
	 * specified file.
	 * <p>
	 * @param file the name of the file. The caller must guarantee this is non-null
	 * @return a FileInputStream object
	 * @exception IOException an exception was encountered while attempting to create
	 *  the FileInputStream.
	 */
	static FileInputStream createFileInputStream(final File file) throws IOException
	{
		try {
			FileInputStream fs = getInstance().doPrivileged(
					new PrivilegedExceptionAction<FileInputStream>() {
						public FileInputStream run() throws IOException {
							return new FileInputStream(file);
						}
					}
			);
			return fs;
		}
		catch (PrivilegedActionException ex) {
			throw new IOException("Unable to create FileInputStream over file "+file.getName(), ex);
		}
	}

	  /**
	   * Using privileged java security, delete the file corresponding to the specified
	   * <code>File</code> object.
	   * <p>
	   * @param fileToDelete a non-null <code>File</code> object whose abstract pathname
	   *  corresponds to the physical file to delete. The caller must guarantee that passed
	   *  object is non null and that the physical file exists. 
	   * @return true if and only if the file was successfully deleted. Otherwise false. If
	   *  we do not have permission to delete this file the corresponding SecurityException is
	   *  logged and absorbed and false is returned.
	   */
	  static boolean deleteFile(final File fileToDelete)
	  {
	    try {
	      Boolean deleted = getInstance().doPrivileged(
	        new PrivilegedAction<Boolean>() {
	          public Boolean run() {
	            return Boolean.valueOf(fileToDelete.delete());
	          }
	        }
	      );
	      return deleted.booleanValue();
	    }
	    catch (SecurityException se) {
			// TODO: Add logging here but be careful since this code is used in logging logic itself
			//       and may result in an indefinite loop.
	      return false;
	    }
	  }

	/**
	 * Using privileged security, create a <code>FileOutputStream</code> over the
	 * specified file, using the appropriate truncate/append semantics.
	 * <p>
	 * @param file the name of the file. The caller must guarantee this is non-null
	 * @param append if true, append to the file if it exists, if false, truncate
	 *  the file (this is the default behavior).
	 * @return a FileOutputStream object
	 * @exception IOException an exception was encountered while attempting to create
	 *  the FileOutputStream.
	 */
	static FileOutputStream createFileOutputStream(final File file, final boolean append) throws IOException
	{
		try {
			FileOutputStream fs = getInstance().doPrivileged(
					new PrivilegedExceptionAction<FileOutputStream>() {
						public FileOutputStream run() throws IOException {
							if (file instanceof GenericOutputFile) {
								return ((GenericOutputFile)file).createOutputStream(append);
							} else {
								return new FileOutputStream(file, append);
							}
						}
					}
			);
			return fs;
		}
		catch (PrivilegedActionException ex) {
			throw new IOException("Unable to create FileOutputStream over file "+file.getName(), ex);
		}
	}

	static File[] listFiles(final File parent, final FileFilter fileFilter) {
		try {
			File[] result = (File[]) getInstance().doPrivileged(new PrivilegedAction<File[]>() {
				public File[] run() {
					return parent.listFiles(fileFilter);
				}
			});
			return result == null ? new File[0] : result;
		} catch (SecurityException se) {
			// TODO: Add logging here but be careful since this code is used in logging logic itself
			//       and may result in an indefinite loop.
			return new File[0];
		}
	}
	
	/**
	 * Returns free space on a file system containing the passed in file
	 */
	static long getFreeSpace(final File fileInFs) {
		try {
			long result = (long) getInstance().doPrivileged(new PrivilegedAction<Long>() {
				public Long run() {
					return fileInFs.getFreeSpace() ;
				}
			});
			return result ;
		} catch (SecurityException se) {
			return -1 ;
		}
	}
	
	/**
	 * Returns total space on a file system containing the passed in file
	 */
	static long getTotalSpace(final File fileInFs) {
		try {
			long result = (long) getInstance().doPrivileged(new PrivilegedAction<Long>() {
				public Long run() {
					return fileInFs.getTotalSpace() ;
				}
			});
			return result ;
		} catch (SecurityException se) {
			return -1 ;
		}
	}
	
	/**
	 * creates a new nameless non-daemon Timer
	 * @return new Timer instance or <code>null</code> on access violation.
	 */
	static Timer createTimer() {
		try {
			return getInstance().doPrivileged(new PrivilegedAction<Timer>() {
				@Override
				public Timer run() {
					return new Timer(true);			// 691649
				}
			});
		} catch (SecurityException se) {
			// TODO: Add logging here but be careful since this code is used in logging logic itself
			//       and may result in an indefinite loop.
			return null;
		}
	}
	  
}