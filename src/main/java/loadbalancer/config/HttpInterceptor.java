package loadbalancer.config;

import loadbalancer.Application;
import loadbalancer.misc.ConsistentHashing;
import loadbalancer.misc.NetworkUtils;
import loadbalancer.misc.StreamUtils;
import loadbalancer.model.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
public class HttpInterceptor implements HandlerInterceptor {

    private static Logger logger = LoggerFactory.getLogger(HttpInterceptor.class);
    public Map<String, Server> serverMap = Collections.synchronizedMap(new HashMap<>());

    @Autowired
    private ServletContext servletContext;
    private ConsistentHashing consistentHashing;

    private boolean consoleLogs = false;
    private int maxAgeStatic = 0, maxAgeCode = 0;

    public HttpInterceptor() throws Exception {
        maxAgeStatic = Integer.parseInt(Application.appConfig.getProperty("load-balancer.max-age.static"));
        maxAgeCode = Integer.parseInt(Application.appConfig.getProperty("load-balancer.max-age.code"));
        consoleLogs = Boolean.parseBoolean(Application.appConfig.getProperty("console-logs.enabled"));
        consistentHashing = new ConsistentHashing(400);
        String originsConfig = Application.appConfig.getProperty("server.origins").trim();
        if (!"".equals(originsConfig)) {
            String origins[] = originsConfig.split(",");
            for (int i = 0; i < origins.length; i++) {
                addServer(origins[i]);
            }
        }
    }

    private synchronized Server serverExecute(int op, String addr) {
        addr = addr.trim();
        if (addr.endsWith("/")) {
            addr = addr.substring(0, addr.length() - 1);
        }
        if (op == 0) {
            return serverMap.get(consistentHashing.getServer(addr));
        } else if (op == 1) {
            serverMap.put(addr, new Server(addr));
            consistentHashing.addServer(addr);
        } else if (op == 2) {
            serverMap.remove(addr);
            consistentHashing.removeServer(addr);
        }
        return null;
    }

    public void addServer(String addr) {
        serverExecute(1, addr);
    }

    public void deleteServer(String addr) {
        serverExecute(2, addr);
    }

    public Server getServer(String addr) {
        return serverExecute(0, addr);
    }

    public MediaType getMediaType(String fileName) {
        try {
            String mimeType = servletContext.getMimeType(fileName);
            MediaType mediaType = MediaType.parseMediaType(mimeType);
            return mediaType;
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        String params = NetworkUtils.getParams(request);
        String method = request.getMethod();
        String remoteAddr = request.getRemoteAddr();

        if (consoleLogs) {
            logger.info("Request received! " + request.getRemoteAddr() + ": " + request.getMethod() + " " + uri + NetworkUtils.getParams(request));
        }

        if ("".equals(uri) || "/".equals(uri)) {
            uri = "/load-balancer/index.html";
        }
        if (uri.startsWith("/load-balancer/")) {
            if (uri.startsWith("/load-balancer/api/")) {
                return true;
            } else {
                returnFile(request, response, uri);
                return false;
            }
        }

        try {
            Server server = getServer(uri);
            if (server != null) {
                server.exchange(request, response);
            } else {
                response.getOutputStream().write("No server available!".getBytes());
                response.getOutputStream().flush();
                response.setStatus(404);
            }
            return false;
        } catch (HttpClientErrorException ex) {
            response.getOutputStream().write(ex.getResponseBodyAsByteArray());
            response.getOutputStream().flush();
            response.setStatus(ex.getStatusCode().value());
            return false;
        } catch (ResourceAccessException ex) {
            response.getOutputStream().write(ex.getMessage().getBytes());
            response.getOutputStream().flush();
            response.setStatus(500);
            return false;
        }
    }

    private boolean returnFile(HttpServletRequest request, HttpServletResponse response, String uri) throws IOException {
        File file = new File(Application.resPath + uri);
        if (!uri.contains("/../") && file.exists()) {
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Methods", "GET,POST,DELETE,PUT,OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "*");
            response.setHeader("Access-Control-Allow-Credentials", "" + true);

            String nameLC = file.getName().trim().toLowerCase();
            int age = (nameLC.endsWith(".html") || nameLC.endsWith(".css") || nameLC.endsWith(".js")) ? maxAgeCode : maxAgeStatic;
            response.setHeader("Access-Control-Max-Age", "" + age);
            response.setHeader("Cache-Control", "max-age=" + age);

            response.setContentType("" + getMediaType(uri.substring(1)));
            StreamUtils.copy(new FileInputStream(file), response.getOutputStream(), true, true);
            response.setStatus(HttpServletResponse.SC_OK);
            return true;
        }
        //response.setContentType("" + getMediaType("test.html"));
        //response.getOutputStream().write("File not found!".getBytes());
        //response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return false;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception exception) throws Exception {
    }
}
