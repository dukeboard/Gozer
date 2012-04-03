package org.gozer.client.aether;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 03/04/12
 * Time: 23:56
 */
public class GozerFileHelper {

    public static void createParentDirs(File file) throws IOException {
        File parent = file.getCanonicalFile().getParentFile();
        if (parent == null) {
            return;
        }
        parent.mkdirs();
        if (!parent.isDirectory()) {
            throw new IOException("Unable to create parent directories of " + file);
        }
    }

    public static void unzipToTempDir(InputStream inputWarST, File outputDir) {
        try {
            //FileInputStream inputWarST = new FileInputStream(inputWar);
            ZipInputStream zis = new ZipInputStream(inputWarST);
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    new File(outputDir.getAbsolutePath() + File.separator + entry.getName()).mkdirs();
                } else {
                    File targetFile = new File(outputDir + File.separator + entry.getName());
                    boolean filtered = false;
                    if (!filtered) {
                        createParentDirs(targetFile);
                        if (!targetFile.exists()) {
                            targetFile.createNewFile();
                        }
                        BufferedOutputStream outputEntry = new BufferedOutputStream(new FileOutputStream(targetFile));
                        byte[] buffer = new byte[1024];
                        int len = 0;
                        while (zis.available() > 0) {
                            len = zis.read(buffer);
                            if (len > 0) {
                                outputEntry.write(buffer, 0, len);
                            }
                        }
                        outputEntry.flush();
                        outputEntry.close();
                    }
                }
            }
            zis.close();
            inputWarST.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void copyFile(InputStream sourceFile, File destFile) throws IOException {
        if (!destFile.exists()) {
            destFile.createNewFile();
        }
        ReadableByteChannel source = null;
        FileChannel destination = null;
        try {
            source = Channels.newChannel(sourceFile);
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, sourceFile.available());
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }

}
