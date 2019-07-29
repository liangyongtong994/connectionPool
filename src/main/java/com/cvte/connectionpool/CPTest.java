package com.cvte.connectionpool;

import com.cvte.connectionpool.cp.ConnectionPool;
import com.cvte.connectionpool.cp.DBUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class CPTest {

    public static void main(String[] args){

        ConnectionPool connectionPool= DBUtils.getConnectionPoolInstance();
        /*try {
            ConnectionPool connectionPool= DBUtils.getConnectionPoolInstance();
            String sql="select * from task";
            Connection connection=connectionPool.getConnection();
            Statement statement=connection.createStatement();
            ResultSet resultSet=statement.executeQuery(sql);
            while (resultSet.next()){
                String taskname=resultSet.getNString("task_name");
                System.out.println("taskname:"+taskname);
            }
            resultSet.close();
            statement.close();
            connectionPool.releaseConnection(connection);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }*/
        RunTest runTest1=new RunTest("Thread-1");
        runTest1.start();
        RunTest2 runTest21=new RunTest2("wrong-thread");
        runTest21.start();
        RunTest runTest2=new RunTest("Thread-2");
        runTest2.start();
        RunTest runTest3=new RunTest("Thread-3");
        runTest3.start();
        RunTest runTest4=new RunTest("Thread-4");
        runTest4.start();
        RunTest runTest5=new RunTest("Thread-5");
        runTest5.start();
        try {
            Thread.sleep(8000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        connectionPool.destroy();
    }
}

class RunTest implements Runnable{

    private Thread t;
    private String threadName;
    RunTest(String name){
        threadName=name;
    }
    @Override
    public void run() {
        try {
            ConnectionPool connectionPool= DBUtils.getConnectionPoolInstance();
            String sql="select * from task";
            Connection connection=connectionPool.getConnection();
            Statement statement=connection.createStatement();
            ResultSet resultSet=statement.executeQuery(sql);
            while (resultSet.next()){
                String taskname=resultSet.getNString("task_name");
                System.out.println("taskname:"+taskname);
            }
            Thread.sleep(3000);
            resultSet.close();
            statement.close();
            connection.close();
            //connectionPool.releaseConnection(connection);
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
}

class RunTest2 implements Runnable{

    private Thread t;
    private String threadName;
    RunTest2(String name){
        threadName=name;
    }
    @Override
    public void run() {
        try {
            ConnectionPool connectionPool= DBUtils.getConnectionPoolInstance();
            String sql="select * from task";
            Connection connection=connectionPool.getConnection();
            Connection connection1=connectionPool.getConnection();
            connection1.close();
            connection.close();

            //throw new SQLException("连接失败");
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
}
