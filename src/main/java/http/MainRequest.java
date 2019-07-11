package http;

import lombok.extern.java.Log;
import lombok.extern.log4j.Log4j;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

@Log
public class MainRequest {

    //private final Logger logger = LogManager.getLogger(this);

    private int statusCode;
    private Header[] headers;

    private int connectTimeout = 12000;
    private int socketTimeout = 12000;
    private String charset = "UTF-8";


    public String sendRequest(Api api, RequestObject object) {

        this.connectTimeout = api.getConnectTimeout();
        this.socketTimeout = api.getConnectTimeout();

        return sendRequest(api.getMethod().name(), api.getUrl(), api.getHeaders(), api.getCookie(),
                object.getRequestParameters(), object.getRequestObject());
    }





    public String sendRequest(String method,
                              String url,
                              Map<String, String> header,
                              Cookie cookie, Map<String, Object> parameters, String requestObject) {

        RequestBuilder requestBuilder = RequestBuilder.create(method);
        requestBuilder.setConfig(RequestConfig.custom().setConnectTimeout(connectTimeout)
                .setSocketTimeout(socketTimeout).build())
                .setUri(url);
        header.forEach(requestBuilder::addHeader);

        HttpClientBuilder httpClientBuilder = HttpClients.custom();
        BasicCookieStore cookieStore = null;

        if (cookie != null) {
            cookieStore = new BasicCookieStore();
            cookieStore.addCookie(cookie);
            httpClientBuilder.setDefaultCookieStore(cookieStore);
        }

        if (!method.equals("GET")) {

            if (requestObject != null) {
                requestBuilder.setEntity(new StringEntity(requestObject, charset));
            }

            if (parameters != null) {
                parameters.forEach((key, val) -> requestBuilder.addParameter(key, val.toString()));
            }

        } else {

            if (parameters != null && parameters.entrySet().size() == 1) {
                requestBuilder.setUri(url + "/" + parameters.values().stream().findFirst().orElse(""));

            } else if (parameters != null){
                parameters.forEach((key, value) -> requestBuilder.addParameter(key, value.toString()));

            } else {
                throw new RuntimeException("An error has occurred when GET request is formed");
            }
        }


        if (url.startsWith("https")) {
            try {
                httpClientBuilder.setSSLContext(new SSLContextBuilder()
                        .loadTrustMaterial(null, (certificate, authType) -> true).build())
                        .setSSLHostnameVerifier(new NoopHostnameVerifier());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        HttpUriRequest request = requestBuilder.build();
        log.info(creatingRequest(request, cookieStore,
                parameters != null ? parameters.toString() : "", requestObject));
        /*logger.info(creatingRequest(request, cookieStore,
                parameters != null ? parameters.toString() : "", requestObject));*/

        try (CloseableHttpClient client = httpClientBuilder.build()) {

            CloseableHttpResponse httpResponse = client.execute(request);
            String response = EntityUtils.toString(httpResponse.getEntity());

            log.info(creatingResponse(httpResponse, response));
            //logger.info(creatingResponse(httpResponse, response));

            this.statusCode = httpResponse.getStatusLine().getStatusCode();
            this.headers = httpResponse.getAllHeaders();

            return response;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private String creatingRequest(HttpUriRequest request, BasicCookieStore cookieStore, String parameters, String jsonObject) {

        return new StringBuilder().append("\n").append("REQUEST:").append("\n")
                .append("[Headers: ").append(Arrays.toString(request.getAllHeaders())).append("]")
                .append("\n")
                .append("[Cookies: ").append(cookieStore != null ?
                        new ArrayList<>(cookieStore.getCookies()): "").append("]")
                .append("\n")
                .append(request.getRequestLine())
                .append("\n")
                .append(parameters)
                .append("\n")
                .append(jsonObject).toString();
    }

    private String creatingResponse(CloseableHttpResponse httpResponse, String entity) {

        return new StringBuilder().append("\n").append("RESPONSE:").append("\n")
                .append(Arrays.toString(headers)).append("\n")
                .append(httpResponse.getStatusLine())
                .append("\n").append(entity).append("\n").toString();
    }

    public int getResponseCode() {
        return statusCode;
    }

    public String getCookieValue(String cookieName) {

        return Arrays.stream(headers)
                .filter(element -> element.getName().equals("Set-Cookie"))
                .flatMap(arrCookies -> Arrays.stream(arrCookies.getElements()))
                .filter(cookie -> cookie.getName().equals(cookieName))
                .map(HeaderElement::getValue)
                .findFirst()
                .orElse("No cookies found");
    }
}
