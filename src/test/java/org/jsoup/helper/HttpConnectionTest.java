package org.jsoup.helper;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.MultiLocaleExtension.MultiLocaleTest;
import org.jsoup.integration.ParseTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.jsoup.helper.HttpConnection.Response.fixHeaderEncoding;
import static org.junit.jupiter.api.Assertions.*;

public class HttpConnectionTest {
    /* most actual network http connection tests are in integration */

    @Test public void canCreateEmptyConnection() {
        HttpConnection con = new HttpConnection();
        assertEquals(Connection.Method.GET, con.request().method());
        assertThrows(IllegalArgumentException.class, () -> {
            URL url = con.request().url();
        });
    }

    @Test public void throwsExceptionOnResponseWithoutExecute() {
        assertThrows(IllegalArgumentException.class, () -> {
            Connection con = HttpConnection.connect("http://example.com");
            con.response();
        });
    }

    @Test public void throwsExceptionOnParseWithoutExecute() {
        assertThrows(IllegalArgumentException.class, () -> {
            Connection con = HttpConnection.connect("http://example.com");
            con.response().parse();
        });
    }

    @Test public void throwsExceptionOnBodyWithoutExecute() {
        assertThrows(IllegalArgumentException.class, () -> {
            Connection con = HttpConnection.connect("http://example.com");
            con.response().body();
        });
    }

    @Test public void throwsExceptionOnBodyAsBytesWithoutExecute() {
        assertThrows(IllegalArgumentException.class, () -> {
            Connection con = HttpConnection.connect("http://example.com");
            con.response().bodyAsBytes();
        });
    }

    @MultiLocaleTest
    public void caseInsensitiveHeaders(Locale locale) {
        Locale.setDefault(locale);

        Connection.Response res = new HttpConnection.Response();
        res.header("Accept-Encoding", "gzip");
        res.header("content-type", "text/html");
        res.header("refErrer", "http://example.com");

        assertTrue(res.hasHeader("Accept-Encoding"));
        assertTrue(res.hasHeader("accept-encoding"));
        assertTrue(res.hasHeader("accept-Encoding"));
        assertTrue(res.hasHeader("ACCEPT-ENCODING"));

        assertEquals("gzip", res.header("accept-Encoding"));
        assertEquals("gzip", res.header("ACCEPT-ENCODING"));
        assertEquals("text/html", res.header("Content-Type"));
        assertEquals("http://example.com", res.header("Referrer"));

        res.removeHeader("Content-Type");
        assertFalse(res.hasHeader("content-type"));

        res.removeHeader("ACCEPT-ENCODING");
        assertFalse(res.hasHeader("Accept-Encoding"));

        res.header("ACCEPT-ENCODING", "deflate");
        assertEquals("deflate", res.header("Accept-Encoding"));
        assertEquals("deflate", res.header("accept-Encoding"));
    }

    @Test public void headers() {
        Connection con = HttpConnection.connect("http://example.com");
        Map<String, String> headers = new HashMap<>();
        headers.put("content-type", "text/html");
        headers.put("Connection", "keep-alive");
        headers.put("Host", "http://example.com");
        con.headers(headers);
        assertEquals("text/html", con.request().header("content-type"));
        assertEquals("keep-alive", con.request().header("Connection"));
        assertEquals("http://example.com", con.request().header("Host"));
    }

    @Test public void sameHeadersCombineWithComma() {
        Map<String, List<String>> headers = new HashMap<>();
        List<String> values = new ArrayList<>();
        values.add("no-cache");
        values.add("no-store");
        headers.put("Cache-Control", values);
        HttpConnection.Response res = new HttpConnection.Response();
        res.processResponseHeaders(headers);
        assertEquals("no-cache, no-store", res.header("Cache-Control"));
    }

    @Test public void multipleHeaders() {
        Connection.Request req = new HttpConnection.Request();
        req.addHeader("Accept", "Something");
        req.addHeader("Accept", "Everything");
        req.addHeader("Foo", "Bar");

        assertTrue(req.hasHeader("Accept"));
        assertTrue(req.hasHeader("ACCEpt"));
        assertEquals("Something, Everything", req.header("accept"));
        assertTrue(req.hasHeader("fOO"));
        assertEquals("Bar", req.header("foo"));

        List<String> accept = req.headers("accept");
        assertEquals(2, accept.size());
        assertEquals("Something", accept.get(0));
        assertEquals("Everything", accept.get(1));

        Map<String, List<String>> headers = req.multiHeaders();
        assertEquals(accept, headers.get("Accept"));
        assertEquals("Bar", headers.get("Foo").get(0));

        assertTrue(req.hasHeader("Accept"));
        assertTrue(req.hasHeaderWithValue("accept", "Something"));
        assertTrue(req.hasHeaderWithValue("accept", "Everything"));
        assertFalse(req.hasHeaderWithValue("accept", "Something for nothing"));

        req.removeHeader("accept");
        headers = req.multiHeaders();
        assertEquals("Bar", headers.get("Foo").get(0));
        assertFalse(req.hasHeader("Accept"));
        assertNull(headers.get("Accept"));
    }

    @Test void responseHeadersPreserveInsertOrder() throws IOException {
        // linkedhashmap preserves the response header order
        Connection.Response res = new HttpConnection.Response();
        res.addHeader("5", "5");
        res.addHeader("4", "4");
        res.addHeader("3", "3");
        res.addHeader("2", "2");
        res.addHeader("1", "1");

        String[] expected = {"5", "4", "3", "2", "1"};
        int i = 0;
        for (String key : res.headers().keySet()) {
            assertEquals(expected[i++], key);
        }

        assertInstanceOf(LinkedHashMap.class, res.headers());
    }

    @Test public void ignoresEmptySetCookies() {
        // prep http response header map
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Set-Cookie", Collections.emptyList());
        HttpConnection.Response res = new HttpConnection.Response();
        res.processResponseHeaders(headers);
        assertEquals(0, res.cookies().size());
    }

    @Test public void connectWithUrl() throws MalformedURLException {
        Connection con = HttpConnection.connect(new URL("http://example.com"));
        assertEquals("http://example.com", con.request().url().toExternalForm());
    }

    @Test public void throwsOnMalformedUrl() {
        assertThrows(IllegalArgumentException.class, () -> HttpConnection.connect("bzzt"));
    }

    @Test public void userAgent() {
        Connection con = HttpConnection.connect("http://example.com/");
        assertEquals(HttpConnection.DEFAULT_UA, con.request().header("User-Agent"));
        con.userAgent("Mozilla");
        assertEquals("Mozilla", con.request().header("User-Agent"));
    }

    @Test public void timeout() {
        Connection con = HttpConnection.connect("http://example.com/");
        assertEquals(30 * 1000, con.request().timeout());
        con.timeout(1000);
        assertEquals(1000, con.request().timeout());
    }

    @Test public void referrer() {
        Connection con = HttpConnection.connect("http://example.com/");
        con.referrer("http://foo.com");
        assertEquals("http://foo.com", con.request().header("Referer"));
    }

    @Test public void method() {
        Connection con = HttpConnection.connect("http://example.com/");
        assertEquals(Connection.Method.GET, con.request().method());
        con.method(Connection.Method.POST);
        assertEquals(Connection.Method.POST, con.request().method());
    }

    @Test public void throwsOnOddData() {
        assertThrows(IllegalArgumentException.class, () -> {
            Connection con = HttpConnection.connect("http://example.com/");
            con.data("Name", "val", "what");
        });
    }

    @Test public void data() {
        Connection con = HttpConnection.connect("http://example.com/");
        con.data("Name", "Val", "Foo", "bar");
        Collection<Connection.KeyVal> values = con.request().data();
        Object[] data =  values.toArray();
        Connection.KeyVal one = (Connection.KeyVal) data[0];
        Connection.KeyVal two = (Connection.KeyVal) data[1];
        assertEquals("Name", one.key());
        assertEquals("Val", one.value());
        assertEquals("Foo", two.key());
        assertEquals("bar", two.value());
    }

    @Test public void cookie() {
        Connection con = HttpConnection.connect("http://example.com/");
        con.cookie("Name", "Val");
        assertEquals("Val", con.request().cookie("Name"));
    }

    @Test public void inputStream() {
        Connection.KeyVal kv = HttpConnection.KeyVal.create("file", "thumb.jpg", ParseTest.inputStreamFrom("Check"));
        assertEquals("file", kv.key());
        assertEquals("thumb.jpg", kv.value());
        assertTrue(kv.hasInputStream());

        kv = HttpConnection.KeyVal.create("one", "two");
        assertEquals("one", kv.key());
        assertEquals("two", kv.value());
        assertFalse(kv.hasInputStream());
    }

    @Test public void requestBody() {
        Connection con = HttpConnection.connect("http://example.com/");
        con.requestBody("foo");
        assertEquals("foo", con.request().requestBody());
    }

    @Test public void encodeUrl() throws MalformedURLException {
        URL url1 = new URL("https://test.com/foo%20bar/%5BOne%5D?q=white+space#frag");
        URL url2 = new UrlBuilder(url1).build();
        assertEquals("https://test.com/foo%20bar/%5BOne%5D?q=white+space#frag", url2.toExternalForm());
    }

    @Test public void encodeUrlSupplementary() throws MalformedURLException {
        URL url1 = new URL("https://example.com/tools/test💩.html"); // = "/tools/test\uD83D\uDCA9.html"
        URL url2 = new UrlBuilder(url1).build();
        assertEquals("https://example.com/tools/test%F0%9F%92%A9.html", url2.toExternalForm());
    }

    @Test void encodedUrlDoesntDoubleEncode() throws MalformedURLException {
        URL url1 = new URL("https://test.com/foo%20bar/%5BOne%5D?q=white+space#frag%20ment");
        URL url2 = new UrlBuilder(url1).build();
        URL url3 = new UrlBuilder(url2).build();
        assertEquals("https://test.com/foo%20bar/%5BOne%5D?q=white+space#frag%20ment", url2.toExternalForm());
        assertEquals("https://test.com/foo%20bar/%5BOne%5D?q=white+space#frag%20ment", url3.toExternalForm());
    }

    @Test void urlPathIsPreservedDoesntDoubleEncode() throws MalformedURLException {
        URL url1 = new URL("https://test.com/[foo] bar+/%5BOne%5D?q=white space#frag ment");
        URL url2 = new UrlBuilder(url1).build();
        URL url3 = new UrlBuilder(url2).build();
        assertEquals("https://test.com/%5Bfoo%5D%20bar+/%5BOne%5D?q=white+space#frag%20ment", url2.toExternalForm());
        assertEquals("https://test.com/%5Bfoo%5D%20bar+/%5BOne%5D?q=white+space#frag%20ment", url3.toExternalForm());
    }

    @Test void connectToEncodedUrl() {
        Connection connect = Jsoup.connect("https://example.com/a%20b%20c?query+string");
        URL url = connect.request().url();
        assertEquals("https://example.com/a%20b%20c?query+string", url.toExternalForm());
    }

    @Test void encodedUrlPathIsPreserved() {
        // https://github.com/jhy/jsoup/issues/1952
        Connection connect = Jsoup.connect("https://example.com/%2B32");
        URL url = connect.request().url();
        assertEquals("https://example.com/%2B32", url.toExternalForm());
    }

    @Test void urlPathPlusIsPreserved() {
        // https://github.com/jhy/jsoup/issues/1952
        Connection connect = Jsoup.connect("https://example.com/123+456");
        URL url = connect.request().url();
        assertEquals("https://example.com/123+456", url.toExternalForm());
    }

    @Test public void noUrlThrowsValidationError() throws IOException {
        HttpConnection con = new HttpConnection();
        boolean threw = false;
        try {
            con.execute();
        } catch (IllegalArgumentException e) {
            threw = true;
            assertEquals("URL not set. Make sure to call #url(...) before executing the request.", e.getMessage());
        }
        assertTrue(threw);
    }

    @Test public void handlesHeaderEncodingOnRequest() {
        Connection.Request req = new HttpConnection.Request();
        req.addHeader("xxx", "é");
    }

    @Test public void supportsInternationalDomainNames() throws MalformedURLException {
        String idn = "https://www.测试.测试/foo.html?bar";
        String puny = "https://www.xn--0zwm56d.xn--0zwm56d/foo.html?bar";

        Connection con = Jsoup.connect(idn);
        assertEquals(puny, con.request().url().toExternalForm());

        HttpConnection.Request req = new HttpConnection.Request();
        req.url(new URL(idn));
        assertEquals(puny, req.url().toExternalForm());
    }

    @Test void supportsIdnWithPort() throws MalformedURLException {
        String idn = "https://www.测试.测试:9001/foo.html?bar";
        String puny = "https://www.xn--0zwm56d.xn--0zwm56d:9001/foo.html?bar";

        Connection con = Jsoup.connect(idn);
        assertEquals(puny, con.request().url().toExternalForm());

        HttpConnection.Request req = new HttpConnection.Request();
        req.url(new URL(idn));
        assertEquals(puny, req.url().toExternalForm());
    }

    @Test public void validationErrorsOnExecute() throws IOException {
        Connection con = new HttpConnection();
        boolean urlThrew = false;
        try {
            con.execute();
        } catch (IllegalArgumentException e) {
            urlThrew = e.getMessage().contains("URL");
        }
        assertTrue(urlThrew);
    }

    @Test void testMalformedException() {
        boolean threw = false;
        try {
            Jsoup.connect("jsoup.org/test");
        } catch (IllegalArgumentException e) {
            threw = true;
            assertEquals("The supplied URL, 'jsoup.org/test', is malformed. Make sure it is an absolute URL, and starts with 'http://' or 'https://'. See https://jsoup.org/cookbook/extracting-data/working-with-urls", e.getMessage());
        }
        assertTrue(threw);
    }

    @Test void setHeaderWithUnicodeValue() {
        Connection connect = Jsoup.connect("https://example.com");
        String value = "/foo/我的";
        connect.header("Key", value);

        String actual = connect.request().header("Key");
        assertEquals(value, actual);
    }

    @Test void setAuth() throws MalformedURLException {
        Connection con = Jsoup.newSession();

        assertNull(con.request().auth());

        RequestAuthenticator auth1 = new RequestAuthenticator() {
            @Override public PasswordAuthentication authenticate(Context auth) {
                return auth.credentials("foo", "bar");
            }
        };

        RequestAuthenticator auth2 = new RequestAuthenticator() {
            @Override public PasswordAuthentication authenticate(Context auth) {
                return auth.credentials("qux", "baz");
            }
        };

        con.auth(auth1);
        assertSame(con.request().auth(), auth1);

        con.auth(auth2);
        assertSame(con.request().auth(), auth2);

        con.request().auth(auth1);
        assertSame(con.request().auth(), auth1);

        PasswordAuthentication creds = auth1.authenticate(
            new RequestAuthenticator.Context(new URL("http://example.com"), Authenticator.RequestorType.SERVER, "Realm"));
        assertNotNull(creds);
        assertEquals("foo", creds.getUserName());
        assertEquals("bar", new String(creds.getPassword()));
    }

    /* Tests for fixHeaderEncoding. We are handling two cases when a server sends a header in UTF8. The JVM will decode
    that in 8859 (per the RFC) and we'll get mojibake, and so we try to fix it. On Android, will be decoded correctly,
    so should not be modified. */
    static String mojibake(String input) {
        // simulate mojibake by encoding in UTF-8 and decoding in ISO-8859-1
        return new String(input.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"search.php?moji=我的", "latin=café", "🍕", "ascii"})
    void fixesHeaderEncodingIfRequired(String input) {
        // if the input was mojibaked, we fix it; otherwise is passed
        // https://github.com/jhy/jsoup/issues/2011
        assertEquals(input, fixHeaderEncoding(input));
        assertEquals(input, fixHeaderEncoding(mojibake(input)));
    }
}
