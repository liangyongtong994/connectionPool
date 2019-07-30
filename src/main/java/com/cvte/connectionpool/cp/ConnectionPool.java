package com.cvte.connectionpool.cp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


//数据库连接池的管理类。包括数据库连接池的初始化、销毁、获取连接、释放连接
public class ConnectionPool {

    private PoolConfig poolConfig;
    private int count;
    private LinkedList<Connection> freeConnection = new LinkedList<>();
    private LinkedList<Connection> useConnection = new LinkedList<>();
    private static ThreadLocal<Connection> threadLocal = new ThreadLocal<>();
    private ScheduledExecutorService scheduledExecutorService;

    public ConnectionPool(PoolConfig poolConfig){
        this.poolConfig=poolConfig;
        System.out.println("开始创建数据库连接池");
        init();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                checkPool();
            }
        };
        scheduledExecutorService= Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(runnable,poolConfig.getCheckTime(),poolConfig.getCheckTime(), TimeUnit.SECONDS);
    }

    //检查数据库连接池中的空闲连接是否达到最小空闲连接数
    private synchronized void checkPool(){
        if(count<poolConfig.getMaxPoolSize())
        {
            while (freeConnection.size()<poolConfig.getMinPoolSize())
            {
                try {
                    freeConnection.add(getNewConnetion());
                    System.out.println("创建一条新连接用来补充连接池");
                    count++;
                    if(count==poolConfig.getMaxPoolSize())
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
                count++;
                System.out.println("创建了数据库连接池中第"+count+"条连接");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    //清空数据库连接池
    public synchronized void destroy()
    {
        scheduledExecutorService.shutdown();
        for(Connection connection:freeConnection)
        {
            try {
                //System.out.println(connection.isClosed());
                ((MyConnection)connection).realClose();
                //System.out.println(connection.isClosed());

            }catch (SQLException e)
            {
                e.printStackTrace();
            }
        }
        for(Connection connection:useConnection)
        {
            try {
                ((MyConnection)connection).realClose();
                //System.out.println(connection.isClosed());

            }catch (SQLException e)
            {
                e.printStackTrace();
            }
        }
        System.out.println("清空数据库连接池");
        freeConnection.clear();
        useConnection.clear();
        count=0;
    }

    //从数据库连接池中获取连接
    public synchronized Connection getConnection() throws InterruptedException {
        Thread thread=Thread.currentThread();
        Connection connection=null;
        if(threadLocal.get()!=null)
        {
            connection=threadLocal.get();
            if(isEnable(connection))
            {
                //useConnection.add(connection);
                System.out.println(thread.getName()+"已经占用一条连接了，不用重复请求");
            }
            else {
                count--;
                System.out.println(thread.getName()+"的连接不可用了，重新获取");
                connection=getConnection();
            }
        }
        else {
            try {
                if(freeConnection.size() > 0)
                {
                    connection=freeConnection.remove(0);
                    if(isEnable(connection))
                    {
                        useConnection.add(connection);
                        System.out.println(thread.getName()+"拿走了池里的空闲连接，还剩"+freeConnection.size()+"条空闲连接,和"+useConnection.size()+"条正使用连接");
                    }
                    else {
                        count--;
                        connection=getConnection();
                    }
                }
                else{
                    if(count<poolConfig.getMaxPoolSize())
                    {
                        connection=getNewConnetion();
                        count++;
                        System.out.println("创建了一条全新的连接");
                        if(isEnable(connection))
                        {
                            useConnection.add(connection);
                            System.out.println(thread.getName()+"创建了一条全新的连接，还剩"+freeConnection.size()+"条空闲连接,和"+useConnection.size()+"条正使用连接");
                        }
                        else {
                            count--;
                            connection=getConnection();
                        }
                    }
                    else {
                        wait(poolConfig.getMaxWaitTime());
                        connection=getConnection();
                    }
                }
                /*if(isEnable(connection))
                {
                    useConnection.add(connection);
                    System.out.println(thread.getName()+"拿走了池里的空闲连接，还剩"+freeConnection.size()+"条空闲连接,和"+useConnection.size()+"条正使用连接");
                }
                else {
                    count--;
                    connection=getConnection();
                }*/
            }catch (SQLException e)
            {
                e.printStackTrace();
            }
            threadLocal.set(connection);
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
        Thread thread=Thread.currentThread();
        if(threadLocal.get()!=null) {
            if (isEnable(connection)) {
                if (count <= poolConfig.getMaxPoolSize()) {
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
                count--;
            }
        }
    }
}
