package com.cvte.connectionpool.cp;

import java.io.IOException;
import java.sql.Connection;
import java.util.Properties;

public class DBUtils {

    private static PoolConfig poolConfig=new PoolConfig();

    //配置数据库连接信息等
    static {
        Properties properties=new Properties();
        try {
            properties.load(DBUtils.class.getClassLoader().getResourceAsStream("dbpool.properties"));
            poolConfig.setDriverName(properties.getProperty("jdbc.driverName"));
            poolConfig.setUrl(properties.getProperty("jdbc.url"));
            poolConfig.setUserName(properties.getProperty("jdbc.userName"));
            poolConfig.setPassword(properties.getProperty("jdbc.password"));
            if(properties.getProperty("cp.minPoolSize")!=null)
            {
                poolConfig.setMinPoolSize(Integer.parseInt(properties.getProperty("cp.minPoolSize")));
            }
            if(properties.getProperty("cp.initialPoolSize")!=null)
            {
                poolConfig.setInitialPoolSize(Integer.parseInt(properties.getProperty("cp.initialPoolSize")));
            }
            if(properties.getProperty("cp.maxPoolSize")!=null)
            {
                poolConfig.setMaxPoolSize(Integer.parseInt(properties.getProperty("cp.maxPoolSize")));
            }
            if(properties.getProperty("cp.maxWaitTime")!=null)
            {
                poolConfig.setMaxWaitTime(Integer.parseInt(properties.getProperty("cp.maxWaitTime")));
            }
            //Class.forName(poolConfig.getDriverName());
        }catch (IOException e)
        {
            e.printStackTrace();
        }//catch (ClassNotFoundException e){
         //   e.printStackTrace();
       // }
    }

    private DBUtils(){};
    //双重校验锁单例对象
    private volatile static ConnectionPool connectionPool;
    public static ConnectionPool getConnectionPoolInstance(){
        if(connectionPool==null){
            synchronized (ConnectionPool.class){
                if(connectionPool==null){
                    System.out.println("对象创建成功了");
                    connectionPool=new ConnectionPool(poolConfig);
                }
            }
        }
        return connectionPool;
    }
   /*private static ConnectionPool connectionPool=null;
   public static ConnectionPool getConnectionPoolInstance(){
       if(connectionPool==null){
           connectionPool=new ConnectionPool(poolConfig);
       }
       return connectionPool;
   }*/

}
