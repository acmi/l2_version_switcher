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

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class Helper {
    private String host;
    private String game;
    private int version;

    public Helper(String host, String game, int version) {
        this.host = host;
        this.game = game;
        this.version = version;
    }

    public String getHost() {
        return host;
    }

    public String getGame() {
        return game;
    }

    public int getVersion() {
        return version;
    }

    public String getBaseUrl() {
        return String.format("http://%s/%s/%d/Patch", host, game, version);
    }

    public String getFileInfoMapZipUrl() {
        return String.format("%s/FileInfoMap_%s_%d.dat.zip", getBaseUrl(), game, version);
    }

    public String getTorrentZipUrl() {
        return String.format("%s/Full_%s_%d.torrent.zip", getBaseUrl(), game, version);
    }

    public boolean isAvailable() throws IOException {
        URL url = new URL(this.getFileInfoMapZipUrl());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        return connection.getResponseCode() == 200;
    }

    public List<FileInfo> getFileInfoList() throws IOException {
        URL url = new URL(getFileInfoMapZipUrl());
        try (InputStream is = url.openConnection().getInputStream()) {
            return Util.getFileInfo(Util.getUnzipStream(is));
        }
    }

    public InputStream getDownloadStream(String path) throws IOException {
        path = path.replaceAll("\\\\", "/");
        String s = String.format("%s/Zip/%s.zip", getBaseUrl(), path);
        URL url = new URL(s);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        return connection.getResponseCode() == 200 ? connection.getInputStream() : new PartsInputStream(path);
    }

    private class PartsInputStream extends InputStream {
        private String path;
        private int part;
        private InputStream current;

        PartsInputStream(String path) throws IOException {
            this.path = path;
            this.current = nextStream();
        }

        public int read() throws IOException {
            int b;
            if ((b = current.read()) == -1) {
                closeCurrentStream();
                current = nextStream();
                return read();
            } else {
                return b;
            }
        }

        private InputStream nextStream() throws IOException {
            String s = String.format("%s/Zip/%s.z%02d", getBaseUrl(), path, ++part);
            URL url = new URL(s);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            if (connection.getResponseCode() == 200) {
                return connection.getInputStream();
            } else {
                throw new IOException("Server returned error: " + connection.getResponseCode());
            }
        }

        private void closeCurrentStream() {
            try {
                current.close();
            } catch (IOException ignore) {
            }
        }

        public void close() throws IOException {
            current.close();
        }
    }
}
