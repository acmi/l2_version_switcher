/*
 * Copyright (c) 2016 acmi
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package acmi.l2.clientmod.l2_version_switcher;

import org.apache.commons.io.IOCase;
import org.apache.commons.io.input.CountingInputStream;

import java.io.*;
import java.util.List;

import static org.apache.commons.io.FilenameUtils.separatorsToSystem;
import static org.apache.commons.io.FilenameUtils.wildcardMatch;
import static org.apache.commons.io.IOUtils.copy;

public class Main {
    public static void main(String[] args) {
        if (args.length != 3 && args.length != 4) {
            System.out.println("USAGE: l2_version_switcher.jar host game version <filter>");
            System.out.println("EXAMPLE: l2_version_switcher.jar " + L2.NCWEST_HOST + " " + L2.NCWEST_GAME + " 1 \"system\\*\"");
            System.out.println("         l2_version_switcher.jar " + L2.PLAYNC_TEST_HOST + " " + L2.PLAYNC_TEST_GAME + " 48");
            System.exit(0);
        }

        String host = args[0];
        String game = args[1];
        int version = Integer.parseInt(args[2]);
        String filter = args.length == 4 ? separatorsToSystem(args[3]) : null;
        Helper helper = new Helper(host, game, version);
        boolean available = false;

        try {
            available = helper.isAvailable();
        } catch (IOException e) {
            System.err.print(e.getClass().getSimpleName());
            if (e.getMessage() != null) {
                System.err.print(": " + e.getMessage());
            }

            System.err.println();
        }

        System.out.println(String.format("Version %d available: %b", version, available));
        if (!available) {
            System.exit(0);
        }

        List<FileInfo> fileInfoList = null;
        try {
            fileInfoList = helper.getFileInfoList();
        } catch (IOException e) {
            System.err.println("Couldn\'t get file info map");
            System.exit(1);
        }

        File l2Folder = new File(System.getProperty("user.dir"));
        for (FileInfo fi : fileInfoList) {
            String filePath = separatorsToSystem(fi.getPath());

            if (filter != null && !wildcardMatch(filePath, filter, IOCase.INSENSITIVE))
                continue;

            System.out.print(filePath + ": ");
            File file = new File(l2Folder, filePath);
            boolean update = true;

            try {
                if (file.exists() && file.length() == fi.getSize() && Util.hashEquals(file, fi.getHash())) {
                    update = false;
                }
            } catch (IOException ignore) {
            }

            if (update) {
                File folder = file.getParentFile();
                if (!folder.exists()) {
                    folder.mkdirs();
                }

                try (InputStream input = new CountingInputStream(new BufferedInputStream(helper.getDownloadStream(fi.getPath()))) {
                    @Override
                    protected synchronized void afterRead(int n) {
                        super.afterRead(n);

                        if (getByteCount() % 0x100000 == 0) {
                            System.out.print('.');
                        }
                    }
                };
                     OutputStream output = new BufferedOutputStream(new FileOutputStream(file))) {
                    copy(Util.getUnzipStream(input), output);
                    System.out.println("OK");
                } catch (IOException e) {
                    System.out.print("FAIL: ");
                    System.out.print(e.getClass().getSimpleName());
                    if (e.getMessage() != null) {
                        System.out.print(": " + e.getMessage());
                    }
                    System.out.println();
                }
            } else {
                System.out.println("OK");
            }
        }
    }
}
