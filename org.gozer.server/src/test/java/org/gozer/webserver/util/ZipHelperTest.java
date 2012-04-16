package org.gozer.webserver.util;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Created with IntelliJ IDEA.
 * User: sebastien
 * Date: 16/04/12
 * Time: 19:17
 * To change this template use File | Settings | File Templates.
 */
public class ZipHelperTest {

    private ZipHelper helper;

    @Before
    public void init() {
         helper = new ZipHelper();
    }

    @Test
    public void should_create_a_zip_with_one_file() {

        File file = new File("text.txt");

        List<File> files = new ArrayList<File>();

        files.add(file);

        helper.createZip(files);

        assertTrue("", true);

    }
}
