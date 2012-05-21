package org.gozer.diff;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for calculating SHA1 sums of files as strings.
 * @author evan
 *
 */
public class SHA1 {

    private static final String SHA1_STR = "SHA-1";

    private static MessageDigest md = null;

    private static final int BYTES_TO_READ = 1024;

    public static synchronized byte[] getSHA1(InputStream is)  {
        try {
            md = MessageDigest.getInstance(SHA1_STR);
        } catch (NoSuchAlgorithmException ne) {
            throw new RuntimeException(ne);
        }
        byte readArray[] = new byte[BYTES_TO_READ];
        byte digest[];

        int totalBytesRead = 0;
        int bytesRead = 0;

        try {
            while ((bytesRead = is.read(readArray, 0, BYTES_TO_READ)) > 0) {
                totalBytesRead += bytesRead;
                md.update(readArray, 0, bytesRead);
            }

            digest = md.digest();
        } catch (IOException ie) {
            throw new RuntimeException("IO Exception: "+ie.getLocalizedMessage(),ie);
        }

        return digest;
    }

}