package us.codecraft.webmagic.downloader.selenium;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import us.codecraft.webmagic.utils.LoadConfig;

import com.gargoylesoftware.htmlunit.BrowserVersion;

/**
 * @author code4crafter@gmail.com <br>
 *         Date: 13-7-26 <br>
 *         Time: 下午1:41 <br>
 */
@SuppressWarnings("unused")
class WebDriverPool {
	private Log logger = LogFactory.getLog(getClass());

	private final static int DEFAULT_CAPACITY = 5;

	private final int capacity;

	private final static int STAT_RUNNING = 1;

	private final static int STAT_CLODED = 2;

	private AtomicInteger stat = new AtomicInteger(STAT_RUNNING);

	/*
	 * new fields for configuring phantomJS
	 */
	private WebDriver mDriver = null;

	private boolean mAutoQuitDriver = true;

	private static final String DRIVER_HTMLUNIT = "htmlunit";
	private static final String DRIVER_PHANTOMJS = "phantomjs";

	protected static Properties sConfig;
	protected static DesiredCapabilities sCaps;

	/**
	 * Configure the GhostDriver, and initialize a WebDriver instance. This part
	 * of code comes from GhostDriver.
	 * https://github.com/detro/ghostdriver/tree/master/test/java/src/test/java/ghostdriver
	 * 
	 * @author bob.li.0718@gmail.com
	 * @throws IOException
	 */
	@SuppressWarnings("static-access")
	public void configure() throws IOException {
		// Read config file
		sConfig = LoadConfig.getConfig();

		String driver = sConfig.getProperty("driver", DRIVER_PHANTOMJS);
		ArrayList<String> cliArgsCap = new ArrayList<String>();
		//proxy
		String isUseProxy = sConfig.getProperty("proxy_type", "0");
				
		// Fetch PhantomJS-specific configuration parameters
		if (driver.equals(DRIVER_PHANTOMJS)) {
			// Prepare capabilities
			sCaps = new DesiredCapabilities();
			sCaps.setJavascriptEnabled(true);
			sCaps.setCapability("takesScreenshot", false);
			
			// "phantomjs_exec_path"
			if (sConfig.getProperty("exec_path") != null) {
				sCaps.setCapability(
						PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY,
						sConfig.getProperty("exec_path"));
			} else {
				throw new IOException(
						String.format(
								"Property '%s' not set!",
								PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY));
			}
			// "phantomjs_driver_path"
			if (sConfig.getProperty("driver_path") != null) {
				System.out.println("Test will use an external GhostDriver");
				sCaps.setCapability(
						PhantomJSDriverService.PHANTOMJS_GHOSTDRIVER_PATH_PROPERTY,
						sConfig.getProperty("driver_path"));
			} else {
				System.out.println("Test will use PhantomJS internal GhostDriver");
			}
			cliArgsCap.add("--web-security=false");
			cliArgsCap.add("--ssl-protocol=any");
			cliArgsCap.add("--ignore-ssl-errors=true");
			
			if(isUseProxy.equals("1")){
				cliArgsCap.add("--proxy=" + sConfig.getProperty("proxy_host") + ":" + sConfig.getProperty("proxy_port"));
				cliArgsCap.add("--proxy-type=http");
			}
			
			if(sConfig.getProperty("driver_logFile") != null){
				cliArgsCap.add("--webdriver-logfile=" + sConfig.getProperty("driver_logFile"));
			}
			
			sCaps.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS,
					cliArgsCap);
			
			// Control LogLevel for GhostDriver, via CLI arguments
			sCaps.setCapability(
					PhantomJSDriverService.PHANTOMJS_GHOSTDRIVER_CLI_ARGS,
					new String[] { "--logLevel="
							+ (sConfig.getProperty("driver_loglevel") != null ? sConfig
									.getProperty("driver_loglevel")
									: "INFO") });

		}
		// Fetch HtmlUnit-specific configuration parameters
		else if(driver.equals(DRIVER_HTMLUNIT)){
			// Prepare capabilities
			//sCaps = new DesiredCapabilities().htmlUnit();
			sCaps = new DesiredCapabilities().htmlUnitWithJs();
			sCaps.setCapability(CapabilityType.TAKES_SCREENSHOT, false);
			sCaps.setJavascriptEnabled(true);
			sCaps.setCapability(CapabilityType.BROWSER_NAME, "HtmlUnit");
			sCaps.setCapability(CapabilityType.ACCEPT_SSL_CERTS, "any");
			sCaps.setCapability(CapabilityType.VERSION, BrowserVersion.BEST_SUPPORTED);
			
			LoggingPreferences logPrefs = new LoggingPreferences();
	        logPrefs.enable(LogType.DRIVER, Level.parse(sConfig.getProperty("driver_loglevel","INFO")));
			sCaps.setCapability(CapabilityType.LOGGING_PREFS, logPrefs);
			
			if(isUseProxy.equals("1")){
				Proxy proxy = new Proxy(Proxy.Type.HTTP,new InetSocketAddress(sConfig.getProperty("proxy_host"),
						Integer.valueOf(sConfig.getProperty("proxy_port"))));
				sCaps.setCapability(CapabilityType.PROXY,proxy);;
			}
		}

		

		// Start appropriate Driver
		if (isUrl(driver)) {
			sCaps.setBrowserName("phantomjs");
			mDriver = new RemoteWebDriver(new URL(driver), sCaps);
		} else if (driver.equals(DRIVER_HTMLUNIT)) {
			mDriver = new HtmlUnitDriver(sCaps);
		} else if (driver.equals(DRIVER_PHANTOMJS)) {
			mDriver = new PhantomJSDriver(sCaps);
		}
	}

	/**
	 * check whether input is a valid URL
	 * 
	 * @author bob.li.0718@gmail.com
	 * @param urlString urlString
	 * @return true means yes, otherwise no.
	 */
	private boolean isUrl(String urlString) {
		try {
			new URL(urlString);
			return true;
		} catch (MalformedURLException mue) {
			return false;
		}
	}

	/**
	 * store webDrivers created
	 */
	private List<WebDriver> webDriverList = Collections
			.synchronizedList(new ArrayList<WebDriver>());

	/**
	 * store webDrivers available
	 */
	private BlockingDeque<WebDriver> innerQueue = new LinkedBlockingDeque<WebDriver>();

	public WebDriverPool(int capacity) {
		this.capacity = capacity;
	}

	public WebDriverPool() {
		this(DEFAULT_CAPACITY);
	}

	/**
	 * 
	 * @return
	 * @throws InterruptedException
	 */
	public WebDriver get() throws InterruptedException {
		checkRunning();
		WebDriver poll = innerQueue.poll();
		if (poll != null) {
			return poll;
		}
		if (webDriverList.size() < capacity) {
			synchronized (webDriverList) {
				if (webDriverList.size() < capacity) {

					// add new WebDriver instance into pool
					try {
						configure();
						innerQueue.add(mDriver);
						webDriverList.add(mDriver);
					} catch (IOException e) {
						e.printStackTrace();
					}

					// ChromeDriver e = new ChromeDriver();
					// WebDriver e = getWebDriver();
					// innerQueue.add(e);
					// webDriverList.add(e);
				}
			}

		}
		return innerQueue.take();
	}

	public void returnToPool(WebDriver webDriver) {
		checkRunning();
		innerQueue.add(webDriver);
	}

	protected void checkRunning() {
		if (!stat.compareAndSet(STAT_RUNNING, STAT_RUNNING)) {
			throw new IllegalStateException("Already closed!");
		}
	}

	public void closeAll() {
		boolean b = stat.compareAndSet(STAT_RUNNING, STAT_CLODED);
		if (!b) {
			throw new IllegalStateException("Already closed!");
		}
		for (WebDriver webDriver : webDriverList) {
			logger.info("Quit webDriver" + webDriver);
			webDriver.quit();
			webDriver = null;
		}
	}

}
