package org.example.client.util;

import org.example.util.JsonConverter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.Properties;

/**
 * Uploads images to Cloudinary using an <b>unsigned upload preset</b>.
 *
 * <p>Configuration is read from {@code /cloudinary.properties} on the
 * classpath (located in {@code client/src/main/resources/}). The file must
 * contain two keys:
 * <pre>
 *   cloudinary.cloud_name=your_cloud_name
 *   cloudinary.upload_preset=your_unsigned_preset_name
 * </pre>
 *
 * <p>Why unsigned? Putting an API secret inside a desktop client would let
 * anyone decompile the JAR and steal it. Unsigned upload presets are the
 * Cloudinary-recommended way for client-only uploads — you can lock the
 * preset down to a specific folder, file size and format via the
 * Cloudinary dashboard.</p>
 *
 * <p>This class uses only the JDK ({@link HttpURLConnection}, {@link Files})
 * so no extra Maven dependency is required.</p>
 */
public final class CloudinaryUploader {

    /** Lazily-loaded configuration. */
    private static volatile Config config;

    private CloudinaryUploader() {}

    /**
     * Uploads an image file to Cloudinary and returns the secure HTTPS URL.
     *
     * <p>This method blocks the calling thread for the duration of the HTTP
     * request. Callers from the JavaFX thread should wrap the call in a
     * background {@link Thread} or {@code javafx.concurrent.Task} so the UI
     * stays responsive.</p>
     *
     * @param file image file on the local disk (jpg/png/gif/webp)
     * @return Cloudinary {@code secure_url} of the uploaded image
     * @throws IOException if the file is unreadable, the network call fails,
     *                     or Cloudinary returns an error response
     */
    public static String upload(File file) throws IOException {
        if (file == null || !file.exists() || !file.isFile()) {
            throw new IOException("Image file not found: " + file);
        }
        Config cfg = loadConfig();
        String endpoint = "https://api.cloudinary.com/v1_1/"
                + cfg.cloudName + "/image/upload";

        String boundary = "----JavaBoundary" + System.currentTimeMillis();
        HttpURLConnection conn =
                (HttpURLConnection) new URL(endpoint).openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(60_000);
        conn.setRequestProperty("Content-Type",
                "multipart/form-data; boundary=" + boundary);

        try (OutputStream out = conn.getOutputStream()) {
            writePart(out, boundary,
                    "Content-Disposition: form-data; name=\"upload_preset\"",
                    cfg.uploadPreset);
            writePart(out, boundary,
                    "Content-Disposition: form-data; name=\"folder\"",
                    "bidding/products");
            // file part — must be raw bytes
            String mime = Files.probeContentType(file.toPath());
            if (mime == null) mime = "application/octet-stream";
            writeBytes(out, "--" + boundary + "\r\n");
            writeBytes(out, "Content-Disposition: form-data; name=\"file\"; filename=\""
                    + file.getName() + "\"\r\n");
            writeBytes(out, "Content-Type: " + mime + "\r\n\r\n");
            Files.copy(file.toPath(), out);
            writeBytes(out, "\r\n--" + boundary + "--\r\n");
            out.flush();
        }

        int status = conn.getResponseCode();
        try (InputStream is = status < 400
                ? conn.getInputStream() : conn.getErrorStream()) {
            String body = is == null ? "" :
                    new String(is.readAllBytes(), StandardCharsets.UTF_8);
            if (status >= 400) {
                throw new IOException("Cloudinary error " + status + ": " + body);
            }
            Map<?, ?> resp = JsonConverter.fromJson(body, Map.class);
            if (resp == null) {
                throw new IOException("Cloudinary returned empty body");
            }
            Object url = resp.get("secure_url");
            if (url == null) {
                throw new IOException("Cloudinary response has no secure_url: " + body);
            }
            return url.toString();
        }
    }

    // ===== Helpers =====================================================
    private static void writePart(OutputStream out, String boundary,
                                  String contentDisposition, String value)
            throws IOException {
        writeBytes(out, "--" + boundary + "\r\n");
        writeBytes(out, contentDisposition + "\r\n\r\n");
        writeBytes(out, value);
        writeBytes(out, "\r\n");
    }

    private static void writeBytes(OutputStream out, String s) throws IOException {
        out.write(s.getBytes(StandardCharsets.UTF_8));
    }

    private static Config loadConfig() throws IOException {
        Config local = config;
        if (local != null) return local;
        synchronized (CloudinaryUploader.class) {
            if (config == null) {
                Properties p = new Properties();
                try (InputStream in = CloudinaryUploader.class
                        .getResourceAsStream("/cloudinary.properties")) {
                    if (in == null) {
                        throw new IOException(
                                "Missing /cloudinary.properties on classpath. "
                              + "Copy cloudinary.properties to "
                              + "client/src/main/resources/cloudinary.properties "
                              + "and fill in your cloud_name + upload_preset.");
                    }
                    p.load(in);
                }
                String cloudName    = trim(p.getProperty("cloudinary.cloud_name"));
                String uploadPreset = trim(p.getProperty("cloudinary.upload_preset"));
                if (cloudName.isEmpty() || uploadPreset.isEmpty()) {
                    throw new IOException(
                            "cloudinary.properties is missing cloud_name "
                          + "and/or upload_preset");
                }
                config = new Config(cloudName, uploadPreset);
            }
            return config;
        }
    }

    private static String trim(String s) { return s == null ? "" : s.trim(); }

    private static final class Config {
        final String cloudName;
        final String uploadPreset;
        Config(String cloudName, String uploadPreset) {
            this.cloudName = cloudName;
            this.uploadPreset = uploadPreset;
        }
    }
}
