/**  
 * @Title: SzseSeleniumDownloader.java
 * @Package us.codecraft.webmagic.downloader.selenium
 * @Description: [TODO]
 * @author Zhuyj
 * @date 2016-4-15
 */
package us.codecraft.webmagic.downloader.selenium;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.downloader.Downloader;
import us.codecraft.webmagic.selector.PlainText;

/**
 * ClassName: SzseSeleniumDownloader 
 * @Description: [TODO]
 * @author Zhuyj
 * @date 2016-4-15
 */
public class SzseSeleniumDownloader implements Downloader, Closeable {

	private volatile WebDriverPool webDriverPool;

	private Log logger = LogFactory.getLog(getClass());

	private int sleepTime = 0;

	private int poolSize = 1;
	
	private String pageNum;
	
	private int pageSize = 10;
	

	@SuppressWarnings("unused")
	private static final String DRIVER_PHANTOMJS = "phantomjs";
	
	private String content;

	
	public SzseSeleniumDownloader setPageSize(int pageSize) {
		this.pageSize = pageSize;
		return this;
	}

	/**
	 * Constructor without any filed. Construct PhantomJS browser
	 * 
	 * @author bob.li.0718@gmail.com
	 */
	public SzseSeleniumDownloader() {
		
	}

	/**
	 * set sleep time to wait until load success
	 *
	 * @param sleepTime sleepTime
	 * @return this
	 */
	public SzseSeleniumDownloader setSleepTime(int sleepTime) {
		this.sleepTime = sleepTime;
		return this;
	}

	@Override
	public Page download(Request request, Task task) {
		checkInit();
		WebDriver webDriver;
		try {
			webDriver = webDriverPool.get();
		} catch (InterruptedException e) {
			logger.warn("interrupted", e);
			return null;
		}
		
		try {
			Thread.sleep(sleepTime);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		logger.info("----------------------------------------------------------------");
		logger.info("start processing page:" + request.getUrl());
		
		Page page = new Page();
		page.setUrl(new PlainText(request.getUrl()));
		page.setRequest(request);
		
		String url = request.getUrl();
		if(url.matches("(?i).*?\\/([^\\/]*?)\\.(pdf|doc(x)?|xls(x)?|rar)$")){
        	page.setRawText("");
        	webDriverPool.returnToPool(webDriver);
        	return page;
        }
		
		//System.out.println(request.getUrl());
		webDriver.get(request.getUrl());
		WebDriver.Options manage = webDriver.manage();
		Site site = task.getSite();
		if (site.getCookies() != null) {
			for (Map.Entry<String, String> cookieEntry : site.getCookies()
					.entrySet()) {
				Cookie cookie = new Cookie(cookieEntry.getKey(),
						cookieEntry.getValue());
				manage.addCookie(cookie);
			}
		}
		
		content = webDriver.getPageSource();
		page.setRawText(content);
		
		int pageCount = 1;
		
		//取内容页中正文部分
		if(url.matches("(?i).*?\\/([^\\/]*?)\\.(s)?htm(l)?$")){
			page.setRawText(content);
			try{	
				//掐头去尾并删除按钮部分
				String total = page.getHtml().xpath("div[@class='content']").toString();
				Document doc = Jsoup.parse(total);
				doc.getElementById("hideBtn").remove();
				page.setRawText(doc.outerHtml());
			}
			catch(Exception e){
				logger.error("fail to extract content!");
			}
				
			webDriverPool.returnToPool(webDriver);
			return page;
			
		}
		
		while(pageCount < pageSize){
			try{
				WebElement btnElement = webDriver.findElement(By.xpath("//input[@class='cls-navigate-next']"));
				String clickStr = btnElement.getAttribute("onClick");
				if(btnElement != null && clickStr != null){
					pageNum = clickStr.replaceAll("(?i).*?\\&PAGENUM\\=(\\d+)", "$1");
					btnElement.findElement(By.xpath("..")).click();
					btnElement.click();
					new WebDriverWait(webDriver, 60).until(new ExpectedCondition<Boolean>() {
					    @Override
					    public Boolean apply(WebDriver driver) {
					        Boolean result = false;
					        try {
					        	WebElement btnElementC = driver.findElement(By.xpath("//input[@class='cls-navigate-next']"));
								String clickStrC = btnElementC.getAttribute("onClick");
								if(clickStrC == null || !clickStrC.replaceAll("(?i).*?\\&PAGENUM\\=(\\d+)", "$1").equals(pageNum)){
									content += driver.getPageSource();
									result = true;
								}
					        } catch(Exception e){        
					        }
					        return result;
					    }
					});
				}
			}catch(Exception e){
				
			}
			pageCount ++;
		}
		
		page.setRawText(content);
		webDriverPool.returnToPool(webDriver);
		return page;
	}

	private void checkInit() {
		if (webDriverPool == null) {
			synchronized (this) {
				webDriverPool = new WebDriverPool(poolSize);
			}
		}
	}

	@Override
	public void setThread(int thread) {
		this.poolSize = thread;
	}

	@Override
	public void close() throws IOException {
		webDriverPool.closeAll();
	}
}