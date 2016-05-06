package us.codecraft.webmagic.utils;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;

public class MongoManager {
	private static Log logger = LogFactory.getLog("us.codecraft.webmagic.utils.MongoManager");
	private static final String CONFIG_FILE = "mongo.properties";
	protected static Properties sConfig;
	
	protected static String host;
	protected static String user;
	protected static String dbName;
	protected static String pwd;
	protected static int port;
	
	protected static int timeOut;
	
	
	private static MongoClient mongo = null;
	
	private MongoManager() {
		
	}
	
	public static MongoDatabase getDB(){
		if (mongo == null) {
			init();
		}
		return mongo.getDatabase(dbName);
	}
	
	public static void closeDB(){
		if (mongo != null) {
			mongo.close();
		}
	}
	
	public static void init(){
		MongoClientOptions.Builder builder =  new MongoClientOptions.Builder();
		
		MongoClientOptions options = null;
		
		List<MongoCredential> credentialsList = new ArrayList<MongoCredential>();
		try {
			loadConfig();
			
			ServerAddress address = new ServerAddress(host, port);
	        MongoCredential credentials= MongoCredential.createCredential(user,dbName,pwd.toCharArray());
	        credentialsList.add(credentials);  
	        
	        //超时时间
			builder.connectTimeout(timeOut);
			
			options = builder.build();
			
	        mongo = new MongoClient(address,credentialsList,options);
	        logger.info("连接至：" + host + ":" + port + "@" + dbName);
		} catch (Exception e) {
			logger.error("数据库连接错误" + e.getMessage());
		}
	}
	
	private static void loadConfig() throws FileNotFoundException, IOException{
		sConfig = new Properties();
		sConfig.load(new FileReader(CONFIG_FILE));
		
		host = sConfig.getProperty("host");
		port = Integer.valueOf(sConfig.getProperty("port"));
		user = sConfig.getProperty("user");
		dbName = sConfig.getProperty("dbName");
		pwd = sConfig.getProperty("pwd");
		
		timeOut = Integer.valueOf(sConfig.getProperty("timeOut"));
	}
	
	public static void main(String[] args) throws FileNotFoundException, IOException{
		loadConfig();
	}
	
}