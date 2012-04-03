/**
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE, Version 3, 29 June 2007;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.gnu.org/licenses/lgpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gozer.webserver.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by IntelliJ IDEA.
 * User: duke
 * Date: 05/12/11
 * Time: 22:06
 * To change this template use File | Settings | File Templates.
 */
public class FileNIOHelper {

    private static Logger logger = LoggerFactory.getLogger(FileNIOHelper.class);


    public static void copyFileToStream(InputStream sourceFile, OutputStream dest) throws IOException {
        ReadableByteChannel source = null;
        WritableByteChannel destination = null;
        try {
            source = Channels.newChannel(sourceFile);
            destination = Channels.newChannel(dest);
            fastChannelCopy(source, destination);
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }

    public static void fastChannelCopy(final ReadableByteChannel src, final WritableByteChannel dest) throws IOException {
        final ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);
        while (src.read(buffer) != -1) {
            // prepare the buffer to be drained
            buffer.flip();
            // write to the channel, may block
            dest.write(buffer);
            // If partial transfer, shift remainder down
            // If buffer is empty, same as doing clear()
            buffer.compact();
        }
        // EOF will leave buffer in fill state
        buffer.flip();
        // make sure the buffer is fully drained.
        while (buffer.hasRemaining()) {
            dest.write(buffer);
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

    public static void unzipToTempDir(InputStream inputWarST, File outputDir, List<String> inclusions, List<String> exclusions) {
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
                    for (String ex : exclusions) {
                        filtered = filtered || targetFile.getName().endsWith(ex.trim());
                    }
                    for (String in : inclusions) {
                        // logger.debug("Check for incluseion => "+targetFile.getName()+"-"+in.trim()+"="+targetFile.getName().trim().equals(in.trim()));
                        if (targetFile.getName().endsWith(in.trim()) || targetFile.getName().equals(in.trim())) {
                            filtered = false;
                        }
                    }
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


    public static void addStringToFile(String data, File outputFile) {
        try {
            logger.debug("trying to add \"{}\" into {}", data, outputFile);
            StringBuilder stringBuilder = new StringBuilder();
            if (outputFile.exists()) {
                InputStream reader = new DataInputStream(new FileInputStream(outputFile));
                ByteArrayOutputStream writer = new ByteArrayOutputStream();

                byte[] bytes = new byte[2048];
                int length = reader.read(bytes);
                while (length != -1) {
                    writer.write(bytes, 0, length);
                    length = reader.read(bytes);

                }
                writer.flush();
                writer.close();
                reader.close();
                stringBuilder.append(new String(writer.toByteArray(), "UTF-8"));
            }
            boolean added = false;
            if (!stringBuilder.toString().contains(data)) {
                stringBuilder.append(data).append("\n");
                added = true;
            }

            if (added){ copyFile(new ByteArrayInputStream(stringBuilder.toString().getBytes()), outputFile); }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
