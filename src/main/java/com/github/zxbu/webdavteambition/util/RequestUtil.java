package com.github.zxbu.webdavteambition.util;

import com.github.zxbu.webdavteambition.store.AliYunDriverClientService;
import net.sf.webdav.ITransaction;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @className: RequestUtil
 * @description: 类描述
 * @author: zhaoyj-g
 * @date: 2021/9/26
 **/
public class RequestUtil {

    public static String parseUserInfo(ITransaction transaction) {
        String uri = transaction.getRequest().getRequestURI();
        try {
            uri = URLDecoder.decode(uri, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String[] arr = uri.split("/");
        if (arr.length <= 1) {
            return "";
        } else {
            return arr[1];
        }
    }

    public static AliYunDriverClientService getClientService(ITransaction transaction, Map<String,AliYunDriverClientService> aliYunDriverClientService) {
        return aliYunDriverClientService.get(parseUserInfo(transaction));
    }
}
