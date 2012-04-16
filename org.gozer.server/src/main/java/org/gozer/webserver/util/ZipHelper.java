package org.gozer.webserver.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.metadata.Metadata;
import org.sonatype.aether.resolution.MetadataResult;

import java.io.*;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created with IntelliJ IDEA.
 * User: sebastien
 * Date: 16/04/12
 * Time: 19:15
 * To change this template use File | Settings | File Templates.
 */
public class ZipHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZipHelper.class);


    public void init(OutputStream outputStream) {
        this.outputStream = new ZipOutputStream(outputStream);
    }

    private ZipOutputStream outputStream;



    public void addMetadaToZip(Metadata metadata) {
        addFileToZip(metadata.getGroupId()+File.separator+metadata.getArtifactId()+File.separator, metadata.getFile());
    }

    public ZipOutputStream addFileToZip(String pathIntoZip, File file) {
        // Create a buffer for reading the files
        byte[] buf = new byte[1024];

//        if (files == null) {
//            LOGGER.debug("returning a null outputStream");
//
//            return null;
//        }

        FileInputStream in = null;

        try {

            // Compress the files
            LOGGER.debug("file path : {}", file.getAbsolutePath());
            LOGGER.debug("path into zip : {}", pathIntoZip);

            in = new FileInputStream(file);

            // Add ZIP entry to output stream.
            ZipEntry zipEntry = new ZipEntry(pathIntoZip+file.getName());

            outputStream.putNextEntry(zipEntry);

            // Transfer bytes from the file to the ZIP file
            int len;
            while ((len = in.read(buf)) > 0) {
                outputStream.write(buf, 0, len);
            }

            // Complete the entry
            outputStream.closeEntry();

            return outputStream;

        } catch (IOException e) {
            LOGGER.error("Error in creating Zip",e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    LOGGER.error("Error closing file {}", file.getAbsolutePath());
                }
            }
        }

        return null;

    }

//    public ZipOutputStream createZipFromMetadatas(Collection<File> files) {
//        // Create a buffer for reading the files
//        byte[] buf = new byte[1024];
//        LOGGER.debug("files : {}",files);
//
//        if (files == null || files.isEmpty()) {
//            LOGGER.debug("returning a null outputStream");
//
//            return null;
//        }
//
//        try {
//
//            // Compress the files
//            for (File file : files) {
//
//                addFileToZip(file);
//            }
//
//            outputStream.finish();
//
//            return outputStream;
//
//        } catch (IOException e) {
//            LOGGER.error("Error in creating Zip",e);
//        }
//
//        return null;
//
//    }





    /**
     *  createZipFromMetadatas do not close outputStream
     * @param results
     * @return       the outputStream with all files
     */
    public ZipOutputStream createZipFromMetadatas(Collection<MetadataResult> results) {

        if (outputStream == null) {
            throw new RuntimeException("Helper is not initialized : outputStream is null");
        }

        try {

            // Compress the files
            for (MetadataResult result : results) {

                Metadata metadata = result.getMetadata();

                addMetadaToZip(metadata);
            }
            outputStream.finish();

        } catch (IOException e) {
            LOGGER.error("Error in creating Zip",e);
        }


        return outputStream;
    }

}
