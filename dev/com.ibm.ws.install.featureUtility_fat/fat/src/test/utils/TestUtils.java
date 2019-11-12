package test.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class TestUtils {

        public static void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
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


        public static void unzipFile(String fileZip, File destDir) throws IOException {
                byte[] buffer = new byte[1024];
                ZipInputStream zis = new ZipInputStream(new FileInputStream(fileZip));
                ZipEntry zipEntry = zis.getNextEntry();
                while (zipEntry != null) {
                        File newFile = newFile(destDir, zipEntry);
                        FileOutputStream fos = new FileOutputStream(newFile);
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                        }
                        fos.close();
                        zipEntry = zis.getNextEntry();
                }
                zis.closeEntry();
                zis.close();
        }


        public static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
                File destFile = new File(destinationDir, zipEntry.getName());

                String destDirPath = destinationDir.getCanonicalPath();
                String destFilePath = destFile.getCanonicalPath();

                if (!destFilePath.startsWith(destDirPath + File.separator)) {
                        throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
                }

                return destFile;
        }


}


