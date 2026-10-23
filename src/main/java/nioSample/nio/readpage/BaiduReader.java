package nioSample.nio.readpage;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;

public class BaiduReader {
    public String read(URI uri) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL).build();
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10)).GET().build();
        HttpResponse<String> response = client.send(request,HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) throw new IOException("HTTP " + response.statusCode());
        return response.body();
    }
    public void readHTMLContent() {
        try { System.out.println(read(URI.create("https://www.baidu.com/"))); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        catch (IOException e) { throw new java.io.UncheckedIOException(e); }
    }
    public static void main(String[] args) { new BaiduReader().readHTMLContent(); }
}
