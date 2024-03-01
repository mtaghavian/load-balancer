package loadbalancer.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import loadbalancer.misc.NetworkUtils;
import loadbalancer.misc.StreamUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class Server implements Comparable<Server> {

    private String address;
    private long routedCount = 0, routedVolume = 0;
    private List<String> routedUrls = new ArrayList<>();

    @JsonIgnore
    private RestTemplate restTemplate = null;

    public Server(String address) {
        this.address = address;
    }

    public void exchange(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String uri = request.getRequestURI();
        String params = NetworkUtils.getParams(request);
        if (address.endsWith("/")) {
            address = address.substring(0, address.length() - 1);
        }
        if (restTemplate == null) {
            restTemplate = new RestTemplate();
        }

        HttpEntity<Resource> entity = new HttpEntity<Resource>(readHeaders(request));
        ResponseEntity<Resource> restResponse = restTemplate.exchange(address + uri + params, HttpMethod.GET, entity, Resource.class);
        writeHeaders(response, restResponse);
        response.setStatus(restResponse.getStatusCodeValue());
        if (restResponse.getBody() != null) {
            routedVolume += StreamUtils.copy(restResponse.getBody().getInputStream(), response.getOutputStream(), false, true);
        }
        routedCount++;
        routedUrls.add(uri);
    }

    private HttpHeaders readHeaders(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String header = headerNames.nextElement();
            headers.set(header, request.getHeader(header));
        }
        return headers;
    }

    private void writeHeaders(HttpServletResponse response, ResponseEntity<Resource> restResponse) {
        for (String header : restResponse.getHeaders().keySet()) {
            response.setHeader(header, NetworkUtils.headerValueListToString(restResponse.getHeaders().get(header)));
        }
    }

    @Override
    public int compareTo(Server o) {
        return address.compareTo(o.address);
    }

    public void clearLog() {
        routedCount = 0;
        routedVolume = 0;
        routedUrls.clear();
    }
}
