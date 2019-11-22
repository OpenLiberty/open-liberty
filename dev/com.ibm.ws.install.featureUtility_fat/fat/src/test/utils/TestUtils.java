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


