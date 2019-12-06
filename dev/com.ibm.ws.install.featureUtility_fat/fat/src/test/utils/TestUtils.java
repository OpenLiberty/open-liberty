package test.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class TestUtils {
        public static boolean deleteFolder(File file) throws IOException {
                Path path = file.toPath();
                if (!path.toFile().exists()) {
                        return true;
                }
                Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                                Files.delete(file);
                                return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                                Files.delete(dir);
                                return FileVisitResult.CONTINUE;
                        }
                });
                return !file.exists();
        }

        public static void zipDirectory(String sourceFile, String outFile) throws IOException {
                FileOutputStream fos = new FileOutputStream(outFile);
                ZipOutputStream zipOut = new ZipOutputStream(fos);
                File fileToZip = new File(sourceFile);

                zipFile(fileToZip, fileToZip.getName(), zipOut);
                zipOut.close();
                fos.close();
        }

        private static void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
                if (fileToZip.isHidden()) {
                        return;
                }
                if (fileToZip.isDirectory()) {
                        if (fileName.endsWith("/")) {
                                zipOut.putNextEntry(new ZipEntry(fileName));
                                zipOut.closeEntry();
                        } else {
                                zipOut.putNextEntry(new ZipEntry(fileName + "/"));
                                zipOut.closeEntry();
                        }
                        File[] children = fileToZip.listFiles();
                        for (File childFile : children) {
                                zipFile(childFile, fileName + "/" + childFile.getName(), zipOut);
                        }
                        return;
                }
                FileInputStream fis = new FileInputStream(fileToZip);
                ZipEntry zipEntry = new ZipEntry(fileName);
                zipOut.putNextEntry(zipEntry);
                byte[] bytes = new byte[1024];
                int length;
                while ((length = fis.read(bytes)) >= 0) {
                        zipOut.write(bytes, 0, length);
                }
                fis.close();
        }

        public static void unzipFileIntoDirectory(ZipFile zipFile, File jiniHomeParentDir) {
                @SuppressWarnings("rawtypes")
                Enumeration files = zipFile.entries();
                File f = null;
                FileOutputStream fos = null;

                while (files.hasMoreElements()) {
                        try {
                                ZipEntry entry = (ZipEntry) files.nextElement();
                                InputStream eis = zipFile.getInputStream(entry);
                                byte[] buffer = new byte[1024];
                                int bytesRead = 0;

                                f = new File(jiniHomeParentDir.getAbsolutePath() + File.separator + entry.getName());

                                if (entry.isDirectory()) {
                                        f.mkdirs();
                                        continue;
                                } else {
                                        f.getParentFile().mkdirs();
                                        f.createNewFile();
                                }
                                f.setWritable(true);
                                f.setReadable(true);
                                f.setExecutable(true);

                                fos = new FileOutputStream(f);

                                while ((bytesRead = eis.read(buffer)) != -1) {
                                        fos.write(buffer, 0, bytesRead);
                                }
                        } catch (IOException e) {
                                e.printStackTrace();
                                continue;
                        } finally {
                                if (fos != null) {
                                        try {
                                                fos.close();
                                        } catch (IOException e) {
                                                // ignore
                                        }
                                }
                        }
                }
        }


}


