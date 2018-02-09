package feign;

import feign.assertj.MockWebServerAssertions;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Rule;
import org.junit.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

public class FeignFluentTest {

    @Rule
    public final MockWebServer server = new MockWebServer();

    @Test
    public void executeSecondaryMethodCall() throws Exception {
        server.enqueue(new MockResponse().setBody("foo"));

        StoreInterface api = Feign.builder()
                .target(StoreInterface.class, "http://localhost:" + server.getPort());

        assertThat(api.products("store_1").get("product_1"))
                .isEqualTo("foo");

        MockWebServerAssertions.assertThat(server.takeRequest())
                .hasMethod("GET")
                .hasPath("/api/stores/store_1/products/product_1");
    }

    @Test
    public void executeSecondaryMethodCallWithQueryParams() throws Exception {
        server.enqueue(new MockResponse().setBody("foo"));

        StoreInterface api = Feign.builder()
                .target(StoreInterface.class, "http://localhost:" + server.getPort());

        assertThat(api.employees("store_1").search("AnYFirsTName", "anyLastName"))
                .isEqualTo("foo");

        MockWebServerAssertions.assertThat(server.takeRequest())
                .hasMethod("GET")
                .hasPath("/api/stores/store_1/employees?filter%5Bfirst_name%5D=anyfirstname&filter%5Blast_name%5D=anylastname");
    }

    @Test
    public void executeSecondaryMethodCallWithRequestBody() throws Exception {
        server.enqueue(new MockResponse().setBody("foo"));

        StoreInterface api = Feign.builder()
                .target(StoreInterface.class, "http://localhost:" + server.getPort());

        api.products("store_1").post("product_1", new BigDecimal("42.1"), 2);
        MockWebServerAssertions.assertThat(server.takeRequest())
                .hasMethod("POST")
                .hasBody("{\"price\": \"42.1\", \"amount\": \"2\"}")
                .hasPath("/api/stores/store_1/products/product_1");
    }

    @Test
    public void executeTertiaryMethodCall() throws Exception {
        server.enqueue(new MockResponse().setBody("foo"));

        StoreInterface api = Feign.builder()
                .target(StoreInterface.class, "http://localhost:" + server.getPort());

        assertThat(api.products("store_1").skus("product_1").get("sku_1"))
                .isEqualTo("foo");

        MockWebServerAssertions.assertThat(server.takeRequest())
                .hasMethod("GET")
                .hasPath("/api/stores/store_1/products/product_1/sku/sku_1");
    }

    @Test
    public void executeTertiaryMethodCallWithExpander() throws Exception {
        server.enqueue(new MockResponse().setBody("foo"));

        StoreInterface api = Feign.builder()
                .target(StoreInterface.class, "http://localhost:" + server.getPort());

        assertThat(api.products("store_1").skus("product_1").list(20))
                .isEqualTo("foo");

        MockWebServerAssertions.assertThat(server.takeRequest())
                .hasMethod("GET")
                .hasPath("/api/stores/store_1/products/product_1/sku?limit=10");
    }

    interface StoreInterface {

        @RequestLine("GET /api/store/{id}")
        String get(@Param("id") String id);

        @Fluent
        ProductInterface products(@Param("store_id") String storeId);

        @Fluent
        EmployeeInterface employees(@Param("store_id") String storeId);
    }

    interface EmployeeInterface {

        @RequestLine("GET /api/stores/{store_id}/employees?filter[first_name]={first}&filter[last_name]={last}")
        String search(@Param(value = "first", expander = LowerCase.class) String firstName,
                      @Param(value = "last", expander = LowerCase.class) String lastName);

        class LowerCase implements Param.Expander {
            @Override
            public String expand(Object value) {
                return String.valueOf(value).toLowerCase();
            }
        }
    }

    interface ProductInterface {

        @RequestLine("GET /api/stores/{store_id}/products/{product_id}")
        String get(@Param("product_id") String key);

        @RequestLine("POST /api/stores/{store_id}/products/{product_id}")
        @Body("%7B\"price\": \"{price}\", \"amount\": \"{amount}\"%7D")
        void post(@Param("product_id") String key, @Param("price") BigDecimal price, @Param("amount") int amount);

        @Fluent
        SkuInterface skus(@Param("product_id") String productId);
    }

    interface SkuInterface {

        @RequestLine("GET /api/stores/{store_id}/products/{product_id}/sku/{sku_id}")
        String get(@Param(value = "sku_id") String skuId);

        @RequestLine("GET /api/stores/{store_id}/products/{product_id}/sku?limit={limit}")
        String list(@Param(value = "limit", expander = MaxLength.class) int limit);

        class MaxLength implements Param.Expander {
            @Override
            public String expand(Object value) {
                if (!(value instanceof Number)) {
                    return value.toString();
                }

                int v = ((Number) value).intValue();
                return v > 10 ? "10" : value.toString();
            }
        }
    }
}
