package com.cvte.connectionpool;

import com.cvte.connectionpool.cp.ConnectionPool;
import com.cvte.connectionpool.cp.DBUtils;
import com.cvte.connectionpool.cp.MyConnection;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CPTest {

    public static void main(String[] args) throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        //ConnectionPool connectionPool= DBUtils.getConnectionPoolInstance();
        DBUtils dbUtils=DBUtils.getDbUtils();
        long startTime=System.currentTimeMillis();
        //RunTest2 runTest2=new RunTest2("wrong-thread");
       // runTest2.start();
        for (int i=0;i<10;i++)
        {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    String sql="select * from task";
                    Connection connection=null;
                    try {
                        //connection=connectionPool.getConnection();
                        connection=dbUtils.getConnection("todo");
                        Statement statement=connection.createStatement();
                        ResultSet resultSet=statement.executeQuery(sql);
                        while (resultSet.next()){
                            String taskname=resultSet.getNString("task_name");
                            System.out.println("taskname:"+taskname);
                        }
                        //Thread.sleep(10000);
                        resultSet.close();
                        statement.close();
                        connection.close();
                    }  catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        dbUtils.destroyPool("todo");
        executorService.shutdown();
        while (!executorService.isTerminated()){

        }
        long endTime=System.currentTimeMillis();
        System.out.println("程序运行时间： "+(endTime-startTime)+"ms");
        /*try {
            dbUtils.destroyPool("todo");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
    }
}

/*class RunTest implements Runnable{

    private Thread t;
    private String threadName;
    RunTest(String name){
        threadName=name;
    }
    @Override
    public void run() {
        ConnectionPool connectionPool= DBUtils.getConnectionPoolInstance();
        String sql="select * from task";
        Connection connection=null;
        try {
            connection=connectionPool.getConnection();
            Statement statement=connection.createStatement();
            ResultSet resultSet=statement.executeQuery(sql);
            while (resultSet.next()){
                String taskname=resultSet.getNString("task_name");
                System.out.println("taskname:"+taskname);
            }
            //Thread.sleep(2000);
            resultSet.close();
            statement.close();
            connection.close();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void start(){
        if (t == null) {
            t = new Thread (this, threadName);
            t.start ();
        }
    }
}*/

class RunTest2 implements Runnable{

    private Thread t;
    private String threadName;
    RunTest2(String name){
        threadName=name;
    }
    @Override
    public void run() {
        //ConnectionPool connectionPool= DBUtils.getConnectionPoolInstance();
        DBUtils dbUtils=DBUtils.getDbUtils();
        Connection connection=null;
        try {

            String sql="select * from test";
            //connection=connectionPool.getConnection();
            connection=dbUtils.getConnection("test");
            ((MyConnection)connection).realClose();
            connection=dbUtils.getConnection("test");
            Statement statement=connection.createStatement();
            ResultSet resultSet=statement.executeQuery(sql);
            while (resultSet.next()){
                String taskname=resultSet.getNString("test_col");
                System.out.println("test_col:"+taskname);
            }
            //Thread.sleep(2000);
            resultSet.close();
            statement.close();
            connection.close();
            Thread.sleep(2000);
            dbUtils.destroyPool("test");

            //throw new SQLException("连接失败");
            //connection.close();


        } catch (SQLException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if(connection!=null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void start(){
        if (t == null) {
            t = new Thread (this, threadName);
            t.start ();
        }
    }
}
