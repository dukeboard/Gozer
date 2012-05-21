package org.gozer.webserver.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.metadata.Metadata;
import org.sonatype.aether.resolution.MetadataRequest;
import org.sonatype.aether.resolution.MetadataResult;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created with IntelliJ IDEA.
 * User: sebastien
 * Date: 16/04/12
 */
public class ZipHelperTest {


    private static final Logger LOGGER = LoggerFactory.getLogger(ZipHelperTest.class);


    private ZipHelper helper;
    private List<MetadataResult> metadatas;
    private File outputArchive;
    private MetadataResult metadata;


    @Before
    public void init() throws FileNotFoundException {
        helper = new ZipHelper();

        File file = new File("org.gozer.server/src/test/resources", "test.txt");

        metadata = mock(MetadataResult.class);
        metadatas = new ArrayList<MetadataResult>();

        metadatas.add(metadata);

        outputArchive = new File("output-test.zip");

        OutputStream outputStream = new FileOutputStream("output-test.zip");

        helper.init(outputStream);

    }

    @Test(expected = RuntimeException.class)
    public void should_return_null_with_a_null_list() {
        helper.createZipFromMetadatas(null);
    }

    @Test(expected = RuntimeException.class)
    public void should_return_null_if_not_initialize() {
        ZipHelper zipHelper = new ZipHelper();
        zipHelper.createZipFromMetadatas(new ArrayList<MetadataResult>());
    }

    @Ignore
    @Test
    public void should_create_a_valid_zip() throws IOException  {
        //create a file output stream
        FileOutputStream zipFOS = new FileOutputStream("test.zip");

        //create a zip output stream from the file output stream created above.
        //zip output stream decorates file output stream
        ZipOutputStream zipoutStream = new ZipOutputStream(zipFOS);

        //creates an zip entry. You can create as many entries as you wish
        //Each entry represent a file with in the zip archive
        ZipEntry zipEntry = new ZipEntry("source1");

        //put the zip entry create above into the zip file
        zipoutStream.putNextEntry(zipEntry);

        //write some text into the zip entry created above.
        //Whatever is written will be added to last added zip entry
        zipoutStream.write(1);

        //close the zip entry.
        zipoutStream.closeEntry();

        //Finish the creation of zip file.
        zipoutStream.finish();

        //close the file output stream
        zipFOS.close();

        //close the zip output stream
        zipoutStream.close();

        assertTrue(isValid(new File("test.zip")));
    }

    @Ignore
    @Test
    public void should_create_a_zip_with_one_file() throws IOException {

        helper.createZipFromMetadatas(metadatas);

        assertTrue(outputArchive.exists());

        assertTrue(isValid(outputArchive));

    }


    @After
    public void deleteOutputFile() {
        outputArchive.delete();
    }

    private boolean isValid(final File file) throws IOException {
        ZipFile zipfile = null;
        try {
            zipfile = new ZipFile(file);
            return true;
        } finally {
            try {
                if (zipfile != null) {
                    zipfile.close();
                    zipfile = null;
                }
            } catch (IOException e) {
            }
        }
    }


}
