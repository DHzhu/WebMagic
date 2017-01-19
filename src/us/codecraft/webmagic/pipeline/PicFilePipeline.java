/**  
 * @Title: UserFilePipeline.java
 * @Package szse
 * @Description: [TODO]
 * @author Zhuyj
 * @date 2016-4-14
 */
package us.codecraft.webmagic.pipeline;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.annotation.ThreadSafe;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bson.Document;

import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.ResultItems;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.utils.FilePersistentBase;
import us.codecraft.webmagic.utils.HttpConstant;
import us.codecraft.webmagic.utils.MongoManager;

import com.google.common.collect.Sets;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

/**
 * ClassName: SzseFilePipeline 
 * @Description: [TODO]
 * @author Zhuyj
 * @date 2016-4-14
 */
@ThreadSafe
public class PicFilePipeline extends FilePersistentBase implements Pipeline {

    private Log logger = LogFactory.getLog(getClass());
    
    /**
     * create a FilePipeline with default path"/data/webmagic/"
     */
    public PicFilePipeline() {
        setPath("/data/webmagic/");
    }

    public PicFilePipeline(String path) {
        setPath(path);
    }

    @Override
    public void process(ResultItems resultItems, Task task) {
    	Request request = resultItems.getRequest();
    	String urlStr = request.getUrl();
        String path = this.path + PATH_SEPERATOR + task.getUUID() + PATH_SEPERATOR 
        		      + urlStr.substring(urlStr.indexOf(task.getUUID()) + task.getUUID().length(),urlStr.lastIndexOf("/")) + PATH_SEPERATOR;
        String fileName = resultItems.get("fileName");
        String fileType = resultItems.get("fileType");
        
        Site site = null;
        if (task != null) {
            site = task.getSite();
        }
        Set<Integer> acceptStatCode;
        Map<String, String> headers = null;
        if (site != null) {
            acceptStatCode = site.getAcceptStatCode();
            headers = site.getHeaders();
        } else {
            acceptStatCode = Sets.newHashSet(200);
        }
        
        CloseableHttpClient httpclient = null; 
        CloseableHttpResponse httpResponse = null;
        
        InputStream in = null;
        int statusCode=0;
        if(fileName != null){
        	try {
            	//附件直接下载、页面用之前下载的内容
        		if(fileType.matches("(?i)pdf|doc(x)?|xls(x)?|rar")){
        			httpclient = HttpClients.createDefault();
                	HttpUriRequest httpUriRequest = getHttpRequestBuilder(request, site, headers).build();
                	httpResponse = httpclient.execute(httpUriRequest);
                	statusCode = httpResponse.getStatusLine().getStatusCode();
                	if (statusAccept(acceptStatCode, statusCode)) {
                		HttpEntity entity = httpResponse.getEntity();  
                    	in = entity.getContent();	
                    } else {
                        logger.warn("code error " + statusCode + "\t" + request.getUrl());
                    }
            	}else{
            		in = new ByteArrayInputStream(((String) resultItems.get("content")).getBytes());
            	}
        		
        		FileOutputStream fout = new FileOutputStream(getFile(path + fileName + "." + fileType));
            	int l = -1;  
                byte[] tmp = new byte[1024];  
                while ((l = in.read(tmp)) != -1) {  
                    fout.write(tmp, 0, l); 
                }  
                fout.flush();  
                fout.close();
                
                //下载完成后修改状态
                MongoDatabase mongo = MongoManager.getDB();
        		MongoCollection<Document> collection = mongo.getCollection("spider");
        		//查询已存在且已下载列表页数据
                Document boL = new Document();
        		boL.put("fileUrl", java.net.URLDecoder.decode(urlStr,"UTF-8"));
        		boL.put("isScan", 0);
        		
        		Document bo = new Document();
        		bo.put("isScan", 1);
        		
        		collection.updateOne(boL, new Document().append("$set",bo));
        		
        		logger.info("Processing complete");
            } catch (Exception e) {
                logger.warn("write file error", e);
            }finally{
            	request.putExtra(Request.STATUS_CODE, statusCode);
            	try {
    				in.close();
    				if (httpResponse != null) {
                        //ensure the connection is released back to pool
                        EntityUtils.consume(httpResponse.getEntity());
                        httpclient.close();
                    }
    			} catch (IOException e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			}
            	logger.info("----------------------------------------------------------------");
            }   
        }
        
    }
    

    
    protected boolean statusAccept(Set<Integer> acceptStatCode, int statusCode) {
        return acceptStatCode.contains(statusCode);
    }

    @SuppressWarnings("deprecation")
	protected RequestBuilder getHttpRequestBuilder(Request request, Site site, Map<String, String> headers) throws UnsupportedEncodingException {
        RequestBuilder requestBuilder = selectRequestMethod(request).setUri(java.net.URLDecoder.decode(request.getUrl().toString(),"UTF-8"));
        if (headers != null) {
            for (Map.Entry<String, String> headerEntry : headers.entrySet()) {
                requestBuilder.addHeader(headerEntry.getKey(), headerEntry.getValue());
            }
        }
        RequestConfig defaultRequestConfig = RequestConfig.custom()
        		.setCookieSpec(CookieSpecs.BEST_MATCH)  
        		.setExpectContinueEnabled(true)  
        		.setStaleConnectionCheckEnabled(true)  
        		.setTargetPreferredAuthSchemes(Arrays.asList(AuthSchemes.NTLM, AuthSchemes.DIGEST))  
                .setConnectionRequestTimeout(site.getTimeOut())
                .setSocketTimeout(site.getTimeOut())
                .setConnectTimeout(site.getTimeOut())
                .build();
        RequestConfig requestConfig = RequestConfig.copy(defaultRequestConfig).build();
		if (site.getHttpProxy() != null) {
			HttpHost host = site.getHttpProxy();
			requestConfig = RequestConfig.copy(defaultRequestConfig)
				.setProxy(host)
				.setProxyPreferredAuthSchemes(Arrays.asList(AuthSchemes.BASIC))
				.build();
			request.putExtra(Request.PROXY, host);
		}
		requestBuilder.setConfig(requestConfig);
        return requestBuilder;
    }

    protected RequestBuilder selectRequestMethod(Request request) {
        String method = request.getMethod();
        if (method == null || method.equalsIgnoreCase(HttpConstant.Method.GET)) {
            //default get
            return RequestBuilder.get();
        } else if (method.equalsIgnoreCase(HttpConstant.Method.POST)) {
            RequestBuilder requestBuilder = RequestBuilder.post();
            NameValuePair[] nameValuePair = (NameValuePair[]) request.getExtra("nameValuePair");
            if (nameValuePair.length > 0) {
                requestBuilder.addParameters(nameValuePair);
            }
            return requestBuilder;
        } else if (method.equalsIgnoreCase(HttpConstant.Method.HEAD)) {
            return RequestBuilder.head();
        } else if (method.equalsIgnoreCase(HttpConstant.Method.PUT)) {
            return RequestBuilder.put();
        } else if (method.equalsIgnoreCase(HttpConstant.Method.DELETE)) {
            return RequestBuilder.delete();
        } else if (method.equalsIgnoreCase(HttpConstant.Method.TRACE)) {
            return RequestBuilder.trace();
        }
        throw new IllegalArgumentException("Illegal HTTP Method " + method);
    }
}