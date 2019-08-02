package com.cvte.connectionpool.cp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


//数据库连接池的管理类。包括数据库连接池的初始化、销毁、获取连接、释放连接
public class ConnectionPool {

    private PoolConfig poolConfig;
    private int connectionCount;//连接池中连接总数
    private LinkedList<Connection> freeConnection = new LinkedList<>();//用来存放空闲连接
    private LinkedList<Connection> useConnection = new LinkedList<>();//用来存放正在使用的连接
    private static ThreadLocal<Connection> threadLocal = new ThreadLocal<>();//存放当前线程请求到的连接
    private ScheduledExecutorService scheduledExecutorService;
    private boolean isClosed;
    private boolean isGetting=false;
    private boolean isClosing=false;

    //初始化创建数据库连接池
    public ConnectionPool(PoolConfig poolConfig){
        this.poolConfig=poolConfig;
        System.out.println("开始创建数据库连接池"+poolConfig.getDbSource());
        init();
        isClosed=false;
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                checkPool();
            }
        };
        scheduledExecutorService= Executors.newSingleThreadScheduledExecutor();
        //开启定时任务
        scheduledExecutorService.scheduleAtFixedRate(runnable,poolConfig.getCheckTime(),poolConfig.getCheckTime(), TimeUnit.MILLISECONDS);
    }

    //检查数据库连接池中的空闲连接是否达到最小空闲连接数
    private synchronized void checkPool(){
        if(connectionCount <poolConfig.getMaxPoolSize())
        {
            while (freeConnection.size()<poolConfig.getMinPoolSize())
            {
                try {
                    freeConnection.add(getNewConnetion());
                    System.out.println("创建一条新连接用来补充连接池");
                    connectionCount++;
                    if(connectionCount >=poolConfig.getMaxPoolSize())
                    {
                        break;
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //创建一条新的连接
    private Connection getNewConnetion() throws SQLException {
        Connection connection=null;
        connection= DriverManager.getConnection(poolConfig.getUrl(),poolConfig.getUserName(),poolConfig.getPassword());
        //ConnectionProxy connectionProxy=new ConnectionProxy(connection,this);
        //connection=connectionProxy.getConnection();
        MyConnection myConnection=new MyConnection(connection,this);
        return myConnection;
    }

    //初始化数据库连接池
    private void init(){
        for (int i=0;i<poolConfig.getInitialPoolSize();i++){
            Connection connection;
            try {
                connection=getNewConnetion();
                freeConnection.add(connection);
                connectionCount++;
                System.out.println("创建了数据库连接池中第"+ connectionCount +"条连接");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    //清空数据库连接池
    public synchronized void destroy() throws InterruptedException {
        scheduledExecutorService.shutdown();
        isClosed=true;
        while(isGetting)
        {
        }
        long startTime=System.currentTimeMillis();
        long nowTime;
        while (useConnection.size()>0)
        {
            nowTime=System.currentTimeMillis();
            if(nowTime-startTime>5000)
            {
                break;
            }
            wait(1000);
        }
        while(isClosing)
        {
        }
        for(Connection connection:freeConnection)
        {
            try {
                ((MyConnection)connection).realClose();

            }catch (SQLException e)
            {
                e.printStackTrace();
            }
        }
        System.out.println("清空"+poolConfig.getDbSource()+"数据库连接池");
        freeConnection.clear();
        useConnection.clear();
        connectionCount =0;
    }

    //从数据库连接池中获取连接
    public synchronized Connection getConnection() throws InterruptedException {
        Thread thread=Thread.currentThread();
        Connection connection=null;
        if(!isClosed) {
            if (threadLocal.get() != null) {
                connection = threadLocal.get();
                if (isEnable(connection)) {
                    //useConnection.add(connection);
                    System.out.println(thread.getName() + "已经占用一条连接了，不用重复请求");
                } else {
                    connectionCount--;
                    threadLocal.remove();
                    useConnection.remove(connection);
                    System.out.println(thread.getName() + "的连接不可用了，重新获取");
                    connection = getConnection();
                }
            } else {
                try {
                    if (freeConnection.size() > 0) {
                        isGetting=true;
                        connection = freeConnection.remove(0);
                        if (isEnable(connection)) {
                            useConnection.add(connection);
                            System.out.println(thread.getName() + "拿走了"+poolConfig.getDbSource()+"池里的空闲连接，还剩" + freeConnection.size() + "条空闲连接,和" + useConnection.size() + "条正使用连接");
                        } else {
                            connectionCount--;
                            System.out.println("连接异常，重新获取");
                            connection = getConnection();
                        }
                    } else {
                        if (connectionCount < poolConfig.getMaxPoolSize()) {
                            connection = getNewConnetion();
                            connectionCount++;
                            System.out.println("创建了一条全新的连接");
                            if (isEnable(connection)) {
                                useConnection.add(connection);
                                System.out.println(thread.getName() + "创建了一条全新的连接，还剩" + freeConnection.size() + "条空闲连接,和" + useConnection.size() + "条正使用连接");
                            } else {
                                connectionCount--;
                                System.out.println("连接异常，重新获取");
                                connection = getConnection();
                            }
                        } else {
                            wait(poolConfig.getMaxWaitTime());
                            connection = getConnection();
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                isGetting=false;
                threadLocal.set(connection);
            }
        }else
        {
            try {
                throw new SQLException("连接池已被关闭");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return connection;
    }

    //判断连接是否可用
    private boolean isEnable(Connection connection){
        if (connection==null){
            return false;
        }
        try {
            if (connection.isClosed()){
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    //释放连接
    public synchronized void releaseConnection(Connection connection)
    {
        System.out.println("开始关闭");
        Thread thread=Thread.currentThread();
        if(threadLocal.get()!=null) {
            isClosing=true;
            if (isEnable(connection)) {
                if (connectionCount <= poolConfig.getMaxPoolSize()) {
                    freeConnection.add(connection);
                } else {
                    try {
                        ((MyConnection) connection).realClose();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
            useConnection.remove(connection);
            threadLocal.remove();
            isClosing=false;
            System.out.println(thread.getName() + "连接用完了，放回空闲池，还剩" + freeConnection.size() + "条空闲连接,和" + useConnection.size() + "条正使用连接");
            notifyAll();
        }
        else {
            System.out.println(thread.getName()+"的连接已经回收过了，不必重新回收");
        }
    }

    private void checkInUse()
    {
        for(Connection connection:useConnection)
        {
            if(!isEnable(connection))
            {
                useConnection.remove(connection);
                connectionCount--;
            }
        }
    }
}
