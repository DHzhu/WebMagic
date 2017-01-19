/**  
 * @Title: StartSpider.java
 * @Package szse
 * @Description: [TODO]
 * @author Zhuyj
 * @date 2016-4-15
 */
package custom;

import java.util.Properties;

import us.codecraft.webmagic.PicSpider;
import us.codecraft.webmagic.downloader.selenium.PicSeleniumDownloader;
import us.codecraft.webmagic.pipeline.PicFilePipeline;
import us.codecraft.webmagic.processor.PicProcessor;
import us.codecraft.webmagic.utils.LoadConfig;

/**
 * ClassName: StartSpider 
 * @Description: [TODO]
 * @author Zhuyj
 * @date 2016-4-15
 */
public class StartSpider {
	@SuppressWarnings("resource")
	public static void main(String[] args) {
		try {
			Properties sConfig = LoadConfig.getConfig();
			PicSpider.create(new PicProcessor())
			.addUrl(sConfig.getProperty("starUrl"))
			.addPipeline(new PicFilePipeline(sConfig.getProperty("savePath")))
			.setDownloader(new PicSeleniumDownloader()
								.setPageSize(Integer.valueOf(sConfig.getProperty("deepPageSize")))
								.setSleepTime(Integer.valueOf(sConfig.getProperty("sleepTime"))))
			.thread(Integer.valueOf(sConfig.getProperty("threadNum")))
			.run();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 
    }
}
