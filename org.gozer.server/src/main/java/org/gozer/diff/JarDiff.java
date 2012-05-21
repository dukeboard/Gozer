package org.gozer.diff;

import org.gozer.webserver.util.FileNIOHelper;

import java.io.*;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 21/05/12
 * Time: 21:33
 */
public class JarDiff {

    public static void main(String[] args) throws IOException {
        long l = System.currentTimeMillis();
        File oldAntLR = new File("/Users/duke/.m2/repository/org/kevoree/platform/org.kevoree.platform.standalone.gui/1.7.0/org.kevoree.platform.standalone.gui-1.7.0.jar");
        File newAntLR = new File("/Users/duke/.m2/repository/org/kevoree/platform/org.kevoree.platform.standalone.gui/1.7.1/org.kevoree.platform.standalone.gui-1.7.1.jar");
        DifferenceCalculator diffCalCl = new DifferenceCalculator(oldAntLR, newAntLR);
        Differences diff = diffCalCl.getDifferences();
        System.out.println(diff);
        System.out.println("Time " + (System.currentTimeMillis() - l));


        File f = new File("drop");
        File fout = new File("drop.jar");
        f.mkdirs();
        buildZipPatch(diff, f, newAntLR,fout);


    }

    public static void buildZipPatch(Differences diff, File outputDir, File lastVersionFile, File patchFile) {
        try {
            FileOutputStream fout = new FileOutputStream(patchFile);
            ZipOutputStream outputStream = new ZipOutputStream(fout);
            ZipFile zp = new ZipFile(lastVersionFile);
            Enumeration entries = zp.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();

                if (!entry.isDirectory() && (diff.getAdded().contains(entry.getName()) || diff.getChanged().contains(entry.getName()))) {
                    outputStream.putNextEntry(entry);
                    int len;
                    byte[] buffer = new byte[1024];
                    InputStream is = null;
                    try {
                        is = zp.getInputStream(entry);
                        while (is.available() > 0) {
                            len = is.read(buffer);
                            if (len > 0) {
                                outputStream.write(buffer, 0, len);
                            }
                        }
                    } finally {
                        if (is != null) {
                            is.close();
                        }
                    }
                    outputStream.closeEntry();
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
