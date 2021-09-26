package com.github.zxbu.webdavteambition.store;

import com.github.zxbu.webdavteambition.model.FileType;
import com.github.zxbu.webdavteambition.model.PathInfo;
import com.github.zxbu.webdavteambition.model.result.TFile;
import com.github.zxbu.webdavteambition.util.RequestUtil;
import net.sf.webdav.*;
import net.sf.webdav.exceptions.WebdavException;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.*;

public class AliYunDriverFileSystemStore implements IWebdavStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(AliYunDriverFileSystemStore.class);

    private static Map<String,AliYunDriverClientService> aliYunDriverClientServiceMap = new HashMap<>();

    public AliYunDriverFileSystemStore(File file) {
    }

    public static void setBean(String key, AliYunDriverClientService aliYunDriverClientService) {
        AliYunDriverFileSystemStore.aliYunDriverClientServiceMap.put(key,aliYunDriverClientService);
    }




    @Override
    public void destroy() {
        LOGGER.info("destroy");

    }

    @Override
    public ITransaction begin(Principal principal, HttpServletRequest req, HttpServletResponse resp) {
        LOGGER.debug("begin");
        return new Transaction(principal, req, resp);
    }

    @Override
    public void checkAuthentication(ITransaction transaction) {
        LOGGER.debug("checkAuthentication");
    }

    @Override
    public void commit(ITransaction transaction) {
        LOGGER.debug("commit");
    }

    @Override
    public void rollback(ITransaction transaction) {
        LOGGER.debug("rollback");

    }

    @Override
    public void createFolder(ITransaction transaction, String folderUri) {
        LOGGER.info("createFolder {}", folderUri);
        AliYunDriverClientService aliYunDriverClientService = RequestUtil.getClientService(transaction,aliYunDriverClientServiceMap);
        aliYunDriverClientService.createFolder(folderUri);
    }

    @Override
    public void createResource(ITransaction transaction, String resourceUri) {
        LOGGER.info("createResource {}", resourceUri);

    }

    @Override
    public InputStream getResourceContent(ITransaction transaction, String resourceUri) {
        AliYunDriverClientService aliYunDriverClientService = RequestUtil.getClientService(transaction,aliYunDriverClientServiceMap);
        LOGGER.info("getResourceContent: {}", resourceUri);
        Enumeration<String> headerNames = transaction.getRequest().getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String s = headerNames.nextElement();
            LOGGER.debug("{} request: {} = {}",resourceUri,  s, transaction.getRequest().getHeader(s));
        }
        HttpServletResponse response = transaction.getResponse();
        long size = getResourceLength(transaction, resourceUri);
        Response downResponse = aliYunDriverClientService.download(resourceUri, transaction.getRequest(), size);
        response.setContentLengthLong(downResponse.body().contentLength());
        LOGGER.debug("{} code = {}", resourceUri, downResponse.code());
        for (String name : downResponse.headers().names()) {
            LOGGER.debug("{} downResponse: {} = {}", resourceUri, name, downResponse.header(name));
            response.addHeader(name, downResponse.header(name));
        }
        response.setStatus(downResponse.code());
        return downResponse.body().byteStream();

    }

    @Override
    public long setResourceContent(ITransaction transaction, String resourceUri, InputStream content, String contentType, String characterEncoding) {
        AliYunDriverClientService aliYunDriverClientService = RequestUtil.getClientService(transaction,aliYunDriverClientServiceMap);
        LOGGER.info("setResourceContent {}", resourceUri);
        HttpServletRequest request = transaction.getRequest();
        HttpServletResponse response = transaction.getResponse();

        long contentLength = request.getContentLength();
        if (contentLength < 0) {
            contentLength = Long.parseLong(Optional.ofNullable(request.getHeader("content-length"))
                    .orElse(request.getHeader("X-Expected-Entity-Length")));
        }
        aliYunDriverClientService.uploadPre(resourceUri, contentLength, content);

        if (contentLength == 0) {
            String expect = request.getHeader("Expect");

            // 支持大文件上传
            if ("100-continue".equalsIgnoreCase(expect)) {
                try {
                    response.sendError(100, "Continue");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return 0;
            }
        }
        return contentLength;
    }

    @Override
    public String[] getChildrenNames(ITransaction transaction, String folderUri) {
        AliYunDriverClientService aliYunDriverClientService = RequestUtil.getClientService(transaction,aliYunDriverClientServiceMap);
        LOGGER.info("getChildrenNames: {}", folderUri);
        TFile tFile = aliYunDriverClientService.getTFileByPath(folderUri);
        if (tFile.getType().equals(FileType.file.name())) {
            return new String[0];
        }
        Set<TFile> tFileList = aliYunDriverClientService.getTFiles(tFile.getFile_id());
        return tFileList.stream().map(TFile::getName).toArray(String[]::new);
    }



    @Override
    public long getResourceLength(ITransaction transaction, String path) {
        return getResourceLength2(transaction, path);
    }

    public long getResourceLength2(ITransaction transaction, String path) {
        AliYunDriverClientService aliYunDriverClientService = RequestUtil.getClientService(transaction,aliYunDriverClientServiceMap);
        LOGGER.info("getResourceLength: {}", path);
        TFile tFile = aliYunDriverClientService.getTFileByPath(path);
        if (tFile == null || tFile.getSize() == null) {
            return 384;
        }

        return tFile.getSize();
    }

    @Override
    public void removeObject(ITransaction transaction, String uri) {
        AliYunDriverClientService aliYunDriverClientService = RequestUtil.getClientService(transaction,aliYunDriverClientServiceMap);
        LOGGER.info("removeObject: {}", uri);
        aliYunDriverClientService.remove(uri);
    }

    @Override
    public boolean moveObject(ITransaction transaction, String destinationPath, String sourcePath) {
        AliYunDriverClientService aliYunDriverClientService = RequestUtil.getClientService(transaction,aliYunDriverClientServiceMap);
        LOGGER.info("moveObject, destinationPath={}, sourcePath={}", destinationPath, sourcePath);

        PathInfo destinationPathInfo = aliYunDriverClientService.getPathInfo(destinationPath);
        PathInfo sourcePathInfo = aliYunDriverClientService.getPathInfo(sourcePath);
        // 名字相同，说明是移动目录
        if (sourcePathInfo.getName().equals(destinationPathInfo.getName())) {
            aliYunDriverClientService.move(sourcePath, destinationPathInfo.getParentPath());
        } else {
            if (!destinationPathInfo.getParentPath().equals(sourcePathInfo.getParentPath())) {
                throw new WebdavException("不支持目录和名字同时修改");
            }
            // 名字不同，说明是修改名字。不考虑目录和名字同时修改的情况
            aliYunDriverClientService.rename(sourcePath, destinationPathInfo.getName());
        }
        return true;
    }

    @Override
    public StoredObject getStoredObject(ITransaction transaction, String uri) {
        AliYunDriverClientService aliYunDriverClientService = RequestUtil.getClientService(transaction,aliYunDriverClientServiceMap);
        LOGGER.info("getStoredObject: {}", uri);
        TFile tFile = aliYunDriverClientService.getTFileByPath(uri);
        if (tFile != null) {
            StoredObject so = new StoredObject();
            so.setFolder(tFile.getType().equalsIgnoreCase("folder"));
            so.setResourceLength(getResourceLength2(transaction, uri));
            so.setCreationDate(tFile.getCreated_at());
            so.setLastModified(tFile.getUpdated_at());
            return so;
        }

        return null;
    }


}
