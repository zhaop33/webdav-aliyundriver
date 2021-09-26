package com.github.zxbu.webdavteambition.config;

import com.github.zxbu.webdavteambition.client.AliYunDriverClient;
import com.github.zxbu.webdavteambition.store.AliYunDriverClientService;
import com.github.zxbu.webdavteambition.store.VirtualTFileService;
import net.sf.webdav.LocalFileSystemStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(AliYunDriveProperties.class)
public class AliYunDriverAutoConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(AliYunDriverAutoConfig.class);

    @Autowired
    private AliYunDriveProperties aliYunDriveProperties;

    @Bean
    public Map<String,AliYunDriverClient> teambitionClient(ApplicationContext applicationContext) throws Exception {
        Map<String,AliYunDriverClient> clientMap = new HashMap<>();
        for (String s : aliYunDriveProperties.getRefreshToken().split(",")) {
            VirtualTFileService virtualTFileService = new VirtualTFileService();
            AliYunDriveProperties properties = new AliYunDriveProperties();
            BeanUtils.copyProperties(aliYunDriveProperties,properties);
            properties.setRefreshToken(s);
            AliYunDriverClient client = new AliYunDriverClient(properties);
            String clientName = getClientName(properties.getNickName(),properties.getUserName());
            clientMap.put(clientName,client);
            new AliYunDriverClientService(clientName,client,virtualTFileService);
        }
        return clientMap;
    }

    public static String getClientName(String nikeName,String userName) {
        return nikeName+"";
    }

}
