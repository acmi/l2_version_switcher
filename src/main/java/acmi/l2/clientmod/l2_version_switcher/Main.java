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

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.apache.commons.io.FilenameUtils.separatorsToSystem;
import static org.apache.commons.io.FilenameUtils.wildcardMatch;

public class Main {
    public static void main(String[] args) {
        if (args.length != 3 && args.length != 4) {
            System.out.println("USAGE: l2_version_switcher.jar host game version <--splash> <filter>");
            System.out.println("EXAMPLE: l2_version_switcher.jar " + L2.NCWEST_HOST + " " + L2.NCWEST_GAME + " 1 \"system\\*\"");
            System.out.println("         l2_version_switcher.jar " + L2.PLAYNC_TEST_HOST + " " + L2.PLAYNC_TEST_GAME + " 48");
            System.exit(0);
        }

        List<String> argsList = new ArrayList<>(Arrays.asList(args));
        String host = argsList.get(0);
        String game = argsList.get(1);
        int version = Integer.parseInt(argsList.get(2));
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

        boolean splash = argsList.remove("--splash");
        if (splash) {
            Optional<FileInfo> splashObj = fileInfoList.stream()
                    .filter(fi -> fi.getPath().contains("sp_32b_01.bmp"))
                    .findAny();
            if (splashObj.isPresent()) {
                try (InputStream is = new FilterInputStream(Util.getUnzipStream(helper.getDownloadStream(splashObj.get().getPath()))) {
                    @Override
                    public int read() throws IOException {
                        int b = super.read();
                        if (b >= 0)
                            b ^= 0x36;
                        return b;
                    }

                    @Override
                    public int read(byte[] b, int off, int len) throws IOException {
                        int r = super.read(b, off, len);
                        if (r >= 0) {
                            for (int i = 0; i < r; i++)
                                b[off + i] ^= 0x36;
                        }
                        return r;
                    }
                }) {
                    new DataInputStream(is).readFully(new byte[28]);
                    BufferedImage bi = ImageIO.read(is);

                    JFrame frame = new JFrame("Lineage 2 [" + version + "] " + splashObj.get().getPath());
                    frame.setContentPane(new JComponent() {
                        {
                            setPreferredSize(new Dimension(bi.getWidth(), bi.getHeight()));
                        }

                        @Override
                        protected void paintComponent(Graphics g) {
                            g.drawImage(bi, 0, 0, null);
                        }
                    });
                    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                    frame.pack();
                    frame.setLocationRelativeTo(null);
                    frame.setVisible(true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("Splash not found");
            }
            return;
        }

        String filter = argsList.size() > 3 ? separatorsToSystem(argsList.get(3)) : null;

        File l2Folder = new File(System.getProperty("user.dir"));
        List<FileInfo> toUpdate = fileInfoList
                .parallelStream()
                .filter(fi -> {
                    String filePath = separatorsToSystem(fi.getPath());

                    if (filter != null && !wildcardMatch(filePath, filter, IOCase.INSENSITIVE))
                        return false;
                    File file = new File(l2Folder, filePath);

                    try {
                        if (file.exists() && file.length() == fi.getSize() && Util.hashEquals(file, fi.getHash())) {
                            System.out.println(filePath + ": OK");
                            return false;
                        }
                    } catch (IOException e) {
                        System.out.println(filePath + ": couldn't check hash: " + e);
                        return true;
                    }

                    System.out.println(filePath + ": need update");
                    return true;
                })
                .collect(Collectors.toList());

        List<String> errors = Collections.synchronizedList(new ArrayList<>());
        ExecutorService executor = Executors.newFixedThreadPool(16);
        CompletableFuture[] tasks = toUpdate
                .stream()
                .map(fi -> CompletableFuture.runAsync(() -> {
                    String filePath = separatorsToSystem(fi.getPath());
                    File file = new File(l2Folder, filePath);

                    File folder = file.getParentFile();
                    if (!folder.exists()) {
                        if (!folder.mkdirs()) {
                            errors.add(filePath + ": couldn't create parent dir");
                            return;
                        }
                    }

                    try (InputStream input = Util.getUnzipStream(new BufferedInputStream(helper.getDownloadStream(fi.getPath())));
                         OutputStream output = new BufferedOutputStream(new FileOutputStream(file))) {
                        byte[] buffer = new byte[Math.min(fi.getSize(), 1 << 24)];
                        int pos = 0;
                        int r;
                        while ((r = input.read(buffer, pos, buffer.length - pos)) >= 0) {
                            pos += r;
                            if (pos == buffer.length) {
                                output.write(buffer, 0, pos);
                                pos = 0;
                            }
                        }
                        if (pos != 0) {
                            output.write(buffer, 0, pos);
                        }
                        System.out.println(filePath + ": OK");
                    } catch (IOException e) {
                        String msg = filePath + ": FAIL: " + e.getClass().getSimpleName();
                        if (e.getMessage() != null) {
                            msg += ": " + e.getMessage();
                        }
                        errors.add(msg);
                    }
                }, executor))
                .toArray(CompletableFuture[]::new);
        CompletableFuture
                .allOf(tasks)
                .thenRun(() -> {
                    for (String err : errors)
                        System.err.println(err);
                    executor.shutdown();
                });
    }
}
