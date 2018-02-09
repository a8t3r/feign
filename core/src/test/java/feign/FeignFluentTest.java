package feign;

import feign.assertj.MockWebServerAssertions;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class FeignFluentTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();
    @Rule
    public final MockWebServer server = new MockWebServer();

    @Test
    public void executeSecondaryMethodCall() throws Exception {
        server.enqueue(new MockResponse().setBody("foo"));

        FluentMethodInterface api = Feign.builder()
                .target(FluentMethodInterface.class, "http://localhost:" + server.getPort());

        assertThat(api.subKeyOf("secondary").get("key"))
                .isEqualTo("foo");

        MockWebServerAssertions.assertThat(server.takeRequest())
                .hasPath("/api/secondary/key");
    }

    interface FluentMethodInterface {

        @RequestLine("GET /api/{primary}")
        String get(@Param("primary") String key);

        @Fluent
        SecondaryMethodInterface subKeyOf(@Param("primary") String key);
    }

    interface SecondaryMethodInterface {

        @RequestLine("GET /api/{primary}/{key}")
        String get(@Param("key") String key);
    }
}
