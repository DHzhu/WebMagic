/**  
 * @Title: StartSpider.java
 * @Package szse
 * @Description: [TODO]
 * @author Zhuyj
 * @date 2016-4-15
 */
package szse;

import java.util.Properties;

import us.codecraft.webmagic.SzseSpider;
import us.codecraft.webmagic.downloader.selenium.SzseSeleniumDownloader;
import us.codecraft.webmagic.pipeline.SzseFilePipeline;
import us.codecraft.webmagic.processor.SzseProcessor;
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
			SzseSpider.create(new SzseProcessor())
			.addUrl(sConfig.getProperty("starUrl"))
			.addPipeline(new SzseFilePipeline(sConfig.getProperty("savePath")))
			.setDownloader(new SzseSeleniumDownloader()
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
