package com.github.zxbu.webdavteambition;

import com.github.zxbu.webdavteambition.client.AliYunDriverClient;
import com.github.zxbu.webdavteambition.config.AliYunDriveProperties;
import com.github.zxbu.webdavteambition.filter.ErrorFilter;
import com.github.zxbu.webdavteambition.sevlet.AliYunDriverServlet;
import com.github.zxbu.webdavteambition.sevlet.LocalStorageServlet;
import com.github.zxbu.webdavteambition.store.AliYunDriverFileSystemStore;
import net.sf.webdav.LocalFileSystemStore;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

@SpringBootApplication
public class WebdavTeambitionApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebdavTeambitionApplication.class, args);
    }

    @Bean
    public ServletRegistrationBean<AliYunDriverServlet> myServlet(Map<String, AliYunDriverClient> aliYunDriverClientMap){
        String[] mappings = aliYunDriverClientMap.keySet().stream().map(i -> "/" + i + "/*").toArray(String[]::new);
        ServletRegistrationBean<AliYunDriverServlet> servletRegistrationBean = new ServletRegistrationBean<>(new AliYunDriverServlet(), mappings);
        Map<String, String> inits = new LinkedHashMap<>();
        inits.put("ResourceHandlerImplementation", AliYunDriverFileSystemStore.class.getName());
//        inits.put("ResourceHandlerImplementation", LocalFileSystemStore.class.getName());
        inits.put("rootpath", "./");
        inits.put("storeDebug", "1");
        servletRegistrationBean.setInitParameters(inits);
        return servletRegistrationBean;
    }

    @Bean
    public ServletRegistrationBean<LocalStorageServlet> localServlet(Map<String, AliYunDriverClient> aliYunDriverClientMap, AliYunDriveProperties aliYunDriveProperties){
        ServletRegistrationBean<LocalStorageServlet> servletRegistrationBean = new ServletRegistrationBean<>(new LocalStorageServlet(), "/*");
        Map<String, String> inits = new LinkedHashMap<>();
        inits.put("ResourceHandlerImplementation", LocalFileSystemStore.class.getName());
//        inits.put("ResourceHandlerImplementation", LocalFileSystemStore.class.getName());
        inits.put("rootpath", aliYunDriveProperties.getWorkDir()+"localStore/");
        inits.put("storeDebug", "1");
        servletRegistrationBean.setInitParameters(inits);
        init(aliYunDriverClientMap,aliYunDriveProperties);
        return servletRegistrationBean;
    }
    @Bean
    public FilterRegistrationBean disableSpringBootErrorFilter() {
        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean();
        filterRegistrationBean.setFilter(new ErrorFilter());
        filterRegistrationBean.setEnabled(true);
        return filterRegistrationBean;
    }
    private void init(Map<String, AliYunDriverClient> aliYunDriverClientMap, AliYunDriveProperties aliYunDriveProperties) {
        for (String s : aliYunDriverClientMap.keySet()) {
            File temp = new File(aliYunDriveProperties.getWorkDir()+"localStore" + File.separator + s);
            if (!temp.exists()) {
                temp.mkdirs();
            }
        }
    }

}
