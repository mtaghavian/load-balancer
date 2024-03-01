package loadbalancer.config;

import loadbalancer.misc.NetworkUtils;
import loadbalancer.misc.StreamUtils;
import loadbalancer.misc.SystemUtils;
import loadbalancer.model.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.*;

@RestController
@RequestMapping("/load-balancer/api")
public class GeneralController {

    @Autowired
    private ServletContext servletContext;

    @Autowired
    private HttpInterceptor httpInterceptor;

    public MediaType getMediaType(String fileName) {
        try {
            String mimeType = servletContext.getMimeType(fileName);
            MediaType mediaType = MediaType.parseMediaType(mimeType);
            return mediaType;
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    @PostMapping("/samplePost")
    public String samplePost(HttpServletRequest request, HttpServletResponse response, @RequestBody String payload) throws IOException {
        return "postResult";
    }

    @GetMapping("/getServerList")
    public List<Server> getServerList(HttpServletRequest request, HttpServletResponse response) throws IOException {
        List<Server> servers = new ArrayList<>(httpInterceptor.serverMap.values());
        Collections.sort(servers);
        return servers;
    }

    @GetMapping("/clearLogs")
    public String clearLogs(HttpServletRequest request, HttpServletResponse response) throws IOException {
        List<Server> servers = new ArrayList<>(httpInterceptor.serverMap.values());
        for (int i = 0; i < servers.size(); i++) {
            servers.get(i).clearLog();
        }
        return "Logs cleared!";
    }

    @PostMapping("/getRoutedUris")
    public String getRoutedUris(HttpServletRequest request, HttpServletResponse response, @RequestBody String server) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (String s : httpInterceptor.serverMap.get(server).getRoutedUrls()) {
            sb.append(s + "\n");
        }
        return sb.toString();
    }

    @PostMapping("/deleteServer")
    public String deleteServer(HttpServletRequest request, HttpServletResponse response, @RequestBody String server) throws IOException {
        httpInterceptor.deleteServer(server);
        return "Server deleted! Server: " + server + "\n";
    }

    @PostMapping("/addServer")
    public String addServer(HttpServletRequest request, HttpServletResponse response, @RequestBody String server) throws IOException {
        httpInterceptor.addServer(server);
        return "Server added! Server: " + server + "\n";
    }

    @GetMapping("/listRoutedUris")
    public String listRoutedUris(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, Set<String>> map = new HashMap<>();
        for (String server : httpInterceptor.serverMap.keySet()) {
            List<String> routedUrls = httpInterceptor.serverMap.get(server).getRoutedUrls();
            for (int i = 0; i < routedUrls.size(); i++) {
                String routedUrl = routedUrls.get(i);
                Set<String> set = map.get(routedUrl);
                if (set == null) {
                    set = new HashSet<>();
                    set.add(server);
                } else {
                    set.add(server);
                }
                map.put(routedUrl, set);
            }
        }
        List<String> keys = new ArrayList<>(map.keySet());
        Collections.sort(keys);
        StringBuilder sb = new StringBuilder();
        for (String url : keys) {
            sb.append(url + " " + map.get(url) + "\n");
        }
        return sb.toString();
    }

    @PostMapping("/execute")
    public String execute(HttpServletRequest request, HttpServletResponse response, @RequestBody(required = true) String cmd) throws IOException {
        return SystemUtils.executeSingleCommand(null, cmd);
    }

    @GetMapping("/systemInfo")
    public String systemInfo(HttpServletRequest request, HttpServletResponse response) throws IOException {
        StringBuilder sb = new StringBuilder();
        Runtime runtime = Runtime.getRuntime();
        String lineBreak = "\n";
        String separator = "=";
        sb.append("availableProcessors" + separator + runtime.availableProcessors()).append(lineBreak);
        sb.append("freeMemory" + separator + runtime.freeMemory()).append(lineBreak);
        sb.append("maxMemory" + separator + runtime.maxMemory()).append(lineBreak);
        sb.append("totalMemory" + separator + runtime.totalMemory()).append(lineBreak);
        File file = new File(".");
        sb.append("getFreeSpace" + separator + file.getFreeSpace()).append(lineBreak);
        sb.append("getUsableSpace" + separator + file.getUsableSpace()).append(lineBreak);
        sb.append("getTotalSpace" + separator + file.getTotalSpace()).append(lineBreak);
        for (Object prop : System.getProperties().keySet()) {
            sb.append(prop + separator + System.getProperty(prop.toString())).append(lineBreak);
        }
        return sb.toString();
    }

    @GetMapping("/clientIP")
    public String clientIP(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        return StreamUtils.getUserAddressDesc(req);
    }

    @GetMapping("/hostname")
    public String hostname(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        return NetworkUtils.getHostname();
    }

    @GetMapping("/serverIP")
    public String serverIP(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        StringBuilder sb = new StringBuilder();
        String lineBreak = "\n";
        ArrayList<NetworkInterface> list = Collections.list(NetworkInterface.getNetworkInterfaces());
        for (int i = 0; i < list.size(); i++) {
            NetworkInterface adapter = list.get(i);
            sb.append("Interface #" + i + lineBreak);
            sb.append("Display name: " + adapter.getDisplayName() + lineBreak);
            sb.append("Name: " + adapter.getName() + lineBreak);
            sb.append("Index: " + adapter.getIndex() + lineBreak);
            Enumeration<InetAddress> inetAddresses = adapter.getInetAddresses();
            ArrayList<InetAddress> addressList = Collections.list(inetAddresses);
            for (int j = 0; j < addressList.size(); j++) {
                sb.append("Address[" + j + "] " + addressList.get(j).getHostName() + "=" + addressList.get(j).getHostAddress() + lineBreak);
            }
            sb.append(lineBreak);
        }
        return sb.toString();
    }
}
