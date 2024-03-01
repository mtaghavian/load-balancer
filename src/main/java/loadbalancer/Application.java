package loadbalancer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import loadbalancer.config.HttpInterceptor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

@SpringBootApplication(exclude = ErrorMvcAutoConfiguration.class)
@EnableScheduling
public class Application implements ApplicationContextAware {

    public static final String appName = "Load-Balancer",
            config = "./application.properties",
            resPath = "./res",
            tmpPath = "./tmp",
            miscPath = "./misc";
    public static final Properties appConfig = new Properties();
    public static Logger logger = LoggerFactory.getLogger(Application.class);
    private static ConfigurableApplicationContext context;

    public static void main(String[] args) throws IOException {
        File configFile = new File(config);
        if (!configFile.exists()) {
            System.err.println("Config file not found!");
            return;
        }
        FileInputStream is = new FileInputStream(configFile);
        appConfig.load(is);
        is.close();

        new File(tmpPath).mkdir();

        context = SpringApplication.run(Application.class, args);
    }

    public static ApplicationContext getContext() {
        return context;
    }

    public static <T> T getBean(Class<T> requiredType) {
        return context.getBean(requiredType);
    }

    @Override
    public void setApplicationContext(ApplicationContext ac) throws BeansException {
        context = (ConfigurableApplicationContext) ac;
    }

    @Configuration
    public class InterceptorConfig extends WebMvcConfigurerAdapter {

        @Autowired
        private HttpInterceptor serviceInterceptor;

        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            registry.addInterceptor(serviceInterceptor);
        }
    }

}
