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

import java.util.StringTokenizer;

public class FileInfo {
    private String path;
    private int size;
    private String hash;

    public FileInfo(String path, int size, String hash) {
        this.path = path;
        this.size = size;
        this.hash = hash;
    }

    public String getPath() {
        return path;
    }

    public int getSize() {
        return size;
    }

    public String getHash() {
        return hash;
    }

    public String toString() {
        return String.format("%s:%d:%s:0", path, size, hash);
    }

    public static FileInfo parse(String line) {
        StringTokenizer st = new StringTokenizer(line, ":");
        return new FileInfo(st.nextToken(), Integer.parseInt(st.nextToken()), st.nextToken());
    }
}
