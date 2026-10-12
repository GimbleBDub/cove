package bw.co.gimble.cove.integration;

import bw.co.gimble.cove.common.ApiException;
import bw.co.gimble.cove.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;

/** Downloads listing photos/videos from a company's own URLs, safely. */
@Component
@RequiredArgsConstructor
public class MediaImporter {

    private static final long MAX_BYTES = 200L * 1024 * 1024;
    private static final int MAX_REDIRECTS = 3;
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg", "image/png", "png", "image/webp", "webp",
            "video/mp4", "mp4", "video/quicktime", "mov", "video/webm", "webm");

    private final OutboundUrlPolicy urlPolicy;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    public MultipartFile download(String rawUrl) {
        URI uri = urlPolicy.require(rawUrl, false);
        try {
            for (int hop = 0; hop <= MAX_REDIRECTS; hop++) {
                HttpRequest request = HttpRequest.newBuilder(uri)
                        .timeout(Duration.ofSeconds(60))
                        .header("User-Agent", "Cove-MediaImport/1")
                        .GET().build();
                HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
                int code = response.statusCode();
                if (code >= 300 && code < 400) {
                    String location = response.headers().firstValue("Location").orElse(null);
                    response.body().close();
                    if (location == null) break;
                    // Every hop is re-checked so a redirect can't reach an internal host.
                    uri = urlPolicy.require(uri.resolve(location).toString(), false);
                    continue;
                }
                if (code != 200) {
                    response.body().close();
                    throw fail(rawUrl, "HTTP " + code);
                }
                byte[] bytes = readCapped(response.body());
                String type = response.headers().firstValue("Content-Type").orElse("").split(";")[0].trim().toLowerCase(Locale.ROOT);
                String name = fileName(uri, type);
                return new ByteArrayMultipartFile(name, type.isEmpty() ? null : type, bytes);
            }
            throw fail(rawUrl, "too many redirects");
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw fail(rawUrl, e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }

    private static byte[] readCapped(InputStream in) throws Exception {
        try (in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[64 * 1024];
            long total = 0;
            int read;
            while ((read = in.read(buffer)) != -1) {
                total += read;
                if (total > MAX_BYTES) throw new IllegalStateException("file is larger than 200MB");
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        }
    }

    private static String fileName(URI uri, String contentType) {
        String path = uri.getPath() == null ? "" : uri.getPath();
        String last = path.substring(path.lastIndexOf('/') + 1);
        if (last.contains(".")) return last;
        return "media." + EXTENSIONS.getOrDefault(contentType, "bin");
    }

    private static ApiException fail(String url, String why) {
        return ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Could not import media from " + url + ": " + why);
    }
}
