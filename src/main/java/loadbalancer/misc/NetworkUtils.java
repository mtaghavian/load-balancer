package loadbalancer.misc;

import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import javax.servlet.http.HttpServletRequest;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

public class NetworkUtils {

    public static String getParams(HttpServletRequest req) {
        Map<String, String[]> parameterMap = req.getParameterMap();
        StringBuilder sb = new StringBuilder();
        Object[] array = parameterMap.keySet().toArray();
        for (int i = 0; i < array.length; i++) {
            sb.append((i == 0 ? "?" : "&") + array[i] + "=");
            String v[] = parameterMap.get("" + array[i]);
            for (int j = 0; j < v.length; j++) {
                sb.append((j != 0 ? "," : "") + v[j]);
            }
        }
        return sb.toString();
    }

    public static String headerValueListToString(List<String> list) {
        if (list == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            sb.append((i == 0) ? list.get(i) : ("," + list.get(i)));
        }
        return sb.toString();
    }

    public static String getHostname() {
        return SystemUtils.executeSingleCommand(null, "hostname");
    }

//    public static RestTemplate restTemplate() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
//        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
//        SSLContext sslcontext = SSLContexts.custom().loadTrustMaterial(null, (chain, authType) -> true).build();
//        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext, new String[]{"TLSv1"}, null, new NoopHostnameVerifier());
//        CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
//        requestFactory.setHttpClient(httpClient);
//        return new RestTemplate(requestFactory);
//    }

}
