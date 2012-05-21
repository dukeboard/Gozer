package org.gozer.diff;

import javolution.util.FastMap;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.io.*;

/**
 * Checks and compiles differences between two zip files.
 * It also has the ability to exclude entries from the comparison
 * based on a regular expression.
 *
 * @author Sean C. Sullivan
 */
public class DifferenceCalculator {

    private ZipFile file1;
    private ZipFile file2;

    /**
     * Constructor taking 2 filenames to compare
     *
     * @throws java.io.IOException
     */
    public DifferenceCalculator(String filename1, String filename2) throws IOException {
        this(new File(filename1), new File(filename2));
    }

    /**
     * Constructor taking 2 Files to compare
     *
     * @throws java.io.IOException
     */
    public DifferenceCalculator(File f1, File f2) throws IOException {
        this(new ZipFile(f1), new ZipFile(f2));
    }

    /**
     * Constructor taking 2 ZipFiles to compare
     */
    public DifferenceCalculator(ZipFile zf1, ZipFile zf2) {
        file1 = zf1;
        file2 = zf2;
    }


    /**
     * Opens the ZipFile and builds up a map of all the entries. The key is the name of
     * the entry and the value is the ZipEntry itself.
     *
     * @param zf The ZipFile for which to build up the map of ZipEntries
     * @return The map containing all the ZipEntries. The key being the name of the ZipEntry.
     * @throws java.io.IOException
     */
    protected Map<String, byte[]> buildZipEntryMap(ZipFile zf) throws IOException {
        Map<String, byte[]> zipEntryMap = new FastMap<String, byte[]>();
        try {
            Enumeration entries = zf.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                InputStream is = null;
                try {
                    is = zf.getInputStream(entry);
                    processZipEntry("", entry, is, zipEntryMap);
                } finally {
                    if (is != null) {
                        is.close();
                    }
                }
            }
        } finally {
            zf.close();
        }
        return zipEntryMap;
    }

    /**
     * Will place ZipEntries for a given ZipEntry into the given Map. More ZipEntries will result
     * if zipEntry is itself a ZipFile. All embedded ZipFiles will be processed with their names
     * prefixed onto the names of their ZipEntries.
     *
     * @param prefix      The prefix of the ZipEntry that should be added to the key. Typically used
     *                    when processing embedded ZipFiles. The name of the embedded ZipFile would be the prefix of
     *                    all the embedded ZipEntries.
     * @param zipEntry    The ZipEntry to place into the Map. If it is a ZipFile then all its ZipEntries
     *                    will also be placed in the Map.
     * @param is          The InputStream of the corresponding ZipEntry.
     * @param zipEntryMap The Map in which to place all the ZipEntries into. The key will
     *                    be the name of the ZipEntry.
     * @throws java.io.IOException
     */
    protected void processZipEntry(String prefix, ZipEntry zipEntry, InputStream is, Map<String, byte[]> zipEntryMap) throws IOException {
        String name = prefix + zipEntry.getName();
        byte[] sha1 = SHA1.getSHA1(is);
        zipEntryMap.put(name, sha1);
    }

    /**
     * Calculates all the differences between two zip files.
     * It builds up the 2 maps of ZipEntries for the two files
     * and then compares them.
     *
     * @param zf1 The first ZipFile to compare
     * @param zf2 The second ZipFile to compare
     * @return All the differences between the two files.
     * @throws java.io.IOException
     */
    protected Differences calculateDifferences(ZipFile zf1, ZipFile zf2) throws IOException {
        Map map1 = buildZipEntryMap(zf1);
        Map map2 = buildZipEntryMap(zf2);
        return calculateDifferences(map1, map2);
    }

    /**
     * Given two Maps of ZipEntries it will generate a Differences of all the
     * differences found between the two maps.
     *
     * @return All the differences found between the two maps
     */
    protected Differences calculateDifferences(Map<String, byte[]> m1, Map<String, byte[]> m2) {
        Differences d = new Differences();
        Set names1 = m1.keySet();
        Set names2 = m2.keySet();
        Set allNames = new HashSet();
        allNames.addAll(names1);
        allNames.addAll(names2);

        Iterator iterAllNames = allNames.iterator();
        while (iterAllNames.hasNext()) {
            String name = (String) iterAllNames.next();
            if (names1.contains(name) && (!names2.contains(name))) {
                d.fileRemoved(name);
            } else if (names2.contains(name) && (!names1.contains(name))) {
                d.fileAdded(name);
            } else if (names1.contains(name) && (names2.contains(name))) {

                if(!Arrays.equals(m1.get(name),m2.get(name))){
                    d.fileChanged(name);
                }

                //ZipEntry entry1 = (ZipEntry) m1.get(name);
                //ZipEntry entry2 = (ZipEntry) m2.get(name);
              //  if (!entriesMatch(entry1, entry2)) {
                 //   d.fileChanged(name);
               // }
            } else {
                throw new IllegalStateException("unexpected state");
            }
        }

        return d;
    }

    /**
     * @return all the differences found between the two zip files.
     * @throws java.io.IOException
     */
    public Differences getDifferences() throws IOException {
        Differences d = calculateDifferences(file1, file2);
        d.setFilename1(file1);
        d.setFilename2(file2);
        return d;
    }
}
