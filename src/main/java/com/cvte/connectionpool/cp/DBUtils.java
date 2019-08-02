package com.cvte.connectionpool.cp;

import java.io.IOException;
import java.sql.Connection;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class DBUtils {

    //private static PoolConfig poolConfig=new PoolConfig();
    private  ConcurrentHashMap<String,ConnectionPool> connectionPools=new ConcurrentHashMap<>();

    //配置数据库连接信息等
    private void createPools() {
        Properties properties=new Properties();
        try {
            properties.load(DBUtils.class.getClassLoader().getResourceAsStream("dbpool.properties"));
            String dbSource=properties.getProperty("dbSource");
            for(String db:dbSource.split(","))
            {
                PoolConfig poolConfig=new PoolConfig();
                poolConfig.setDbSource(db);
                poolConfig.setDriverName(properties.getProperty(db+".driverName"));
                poolConfig.setUrl(properties.getProperty(db+".url"));
                poolConfig.setUserName(properties.getProperty(db+".userName"));
                poolConfig.setPassword(properties.getProperty(db+".password"));
                if(properties.getProperty(db+".minPoolSize")!=null)
                {
                    poolConfig.setMinPoolSize(Integer.parseInt(properties.getProperty(db+".minPoolSize")));
                }
                if(properties.getProperty(db+".initialPoolSize")!=null)
                {
                    poolConfig.setInitialPoolSize(Integer.parseInt(properties.getProperty(db+".initialPoolSize")));
                }
                if(properties.getProperty(db+".maxPoolSize")!=null)
                {
                    poolConfig.setMaxPoolSize(Integer.parseInt(properties.getProperty(db+".maxPoolSize")));
                }
                if(properties.getProperty(db+".maxWaitTime")!=null)
                {
                    poolConfig.setMaxWaitTime(Integer.parseInt(properties.getProperty(db+".maxWaitTime")));
                }
                ConnectionPool connectionPool=new ConnectionPool(poolConfig);
                connectionPools.put(db,connectionPool);
            }
            /*poolConfig.setDriverName(properties.getProperty("jdbc.driverName"));
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
            }*/
            //Class.forName(poolConfig.getDriverName());
        }catch (IOException e)
        {
            e.printStackTrace();
        }//catch (ClassNotFoundException e){
         //   e.printStackTrace();
       // }
    }

    private DBUtils(){
        createPools();
    };
    //双重校验锁单例对象
    //private ConnectionPool connectionPool=new ConnectionPool(poolConfig);

    private volatile static DBUtils dbUtils;

    public static DBUtils getDbUtils(){
        if(dbUtils==null){
            synchronized (DBUtils.class){
                if(dbUtils==null){
                    System.out.println("对象创建成功了");
                    dbUtils=new DBUtils();
                }
            }
        }
        return dbUtils;
    }

    public Connection getConnection(String poolName){
        try {
            ConnectionPool connectionPool=connectionPools.get(poolName);
            return connectionPool.getConnection();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void destroyPool(String poolName) throws InterruptedException {
        ConnectionPool connectionPool=connectionPools.get(poolName);
        connectionPool.destroy();
        connectionPools.remove(poolName);
    }
   /*private static ConnectionPool connectionPool=null;
   public static ConnectionPool getConnectionPoolInstance(){
       if(connectionPool==null){
           connectionPool=new ConnectionPool(poolConfig);
       }
       return connectionPool;
   }*/

}
