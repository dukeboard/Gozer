package org.gozer.webserver.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.metadata.Metadata;
import org.sonatype.aether.resolution.MetadataResult;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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



    public void createZip(Collection<File> files) {

    }

    /**
     *  createZip do not close outputStream
     * @param results
     * @param outputStream
     * @return       the outputStream with all files
     */
    public ZipOutputStream createZip(String repo, Collection<MetadataResult> results, ZipOutputStream outputStream) {

        // Create a buffer for reading the files
        byte[] buf = new byte[1024];
        try {

            // Compress the files
            for (MetadataResult result : results) {

                LOGGER.info("absolute path {}", result.getMetadata().getFile().getCanonicalPath());

                Metadata metadata = result.getMetadata();

                String relativeDirectoryFromRepoRoot = result.getMetadata().getFile().getParentFile().getAbsolutePath().replace(repo, "");

                File file = new File(relativeDirectoryFromRepoRoot, metadata.getFile().getName());

                LOGGER.info("file name in zip : {}",file.getAbsolutePath());


//                outputStream.putNextEntry(new ZipEntry(metadata.getGroupId()+File.separator));
//                outputStream.putNextEntry(new ZipEntry(metadata.getGroupId()+File.separator+metadata.getArtifactId()));



                FileInputStream in = new FileInputStream(metadata.getFile());

                // Add ZIP entry to output stream.
                outputStream.putNextEntry(new ZipEntry(metadata.getGroupId()+"-"+metadata.getArtifactId()+"-"+file.getName()));

                // Transfer bytes from the file to the ZIP file
                int len;
                while ((len = in.read(buf)) > 0) {
                    outputStream.write(buf, 0, len);
                }

                // Complete the entry
                outputStream.closeEntry();
                in.close();
            }

        } catch (IOException e) {
            LOGGER.error("Error in creating Zip",e);
        }

        return outputStream;
    }

}
