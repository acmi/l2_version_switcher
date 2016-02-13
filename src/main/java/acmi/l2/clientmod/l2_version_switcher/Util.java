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

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.tukaani.xz.LZMAInputStream;

import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Util {
    public static List<FileInfo> getFileInfo(InputStream is) throws IOException {
        return new BufferedReader(new InputStreamReader(is, "utf-16"))
                .lines()
                .map(FileInfo::parse)
                .collect(Collectors.toList());
    }

    public static InputStream getUnzipStream(InputStream input) throws IOException {
        return new LZMAInputStream(input);
    }

    public static boolean hashEquals(File file, String hashString) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("sha-1");
            try (FileInputStream hashBytes = new FileInputStream(file)) {
                DigestInputStream dis = new DigestInputStream(hashBytes, md);
                IOUtils.copy(dis, NullOutputStream.NULL_OUTPUT_STREAM);
            }
            byte[] hash = md.digest();
            return Arrays.equals(hash, DatatypeConverter.parseHexBinary(hashString));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
