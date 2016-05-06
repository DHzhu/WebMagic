package us.codecraft.webmagic.utils;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class LoadConfig {
	protected static Properties sConfig = new Properties();;
	private static final String CONFIG_FILE = "config.properties";
	
	public static void initConfig() throws FileNotFoundException, IOException{
		sConfig.load(new FileReader(CONFIG_FILE));
	}
	
	public static Properties getConfig() throws FileNotFoundException, IOException{
		if(sConfig.isEmpty()){
			initConfig();
		}
		return sConfig;
	}
}
