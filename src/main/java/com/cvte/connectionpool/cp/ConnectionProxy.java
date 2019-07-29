package com.cvte.connectionpool.cp;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;

public class ConnectionProxy implements InvocationHandler {

    private Connection connection;
    private ConnectionPool connectionPool;

    void close() throws SQLException{
        connection.close();
    }
    ConnectionProxy(Connection connection,ConnectionPool connectionPool)
    {
        this.connection=connection;
        this.connectionPool=connectionPool;
    }

    public Connection getConnection() {
        Connection connection1 = (Connection) Proxy.newProxyInstance(connection.getClass().getClassLoader(), connection.getClass().getInterfaces(), this);
        return connection1;
    }
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if(method.getName().equals("close"))
        {
            connectionPool.releaseConnection(connection);
            return this.connection;
        }
        else {
            return method.invoke(this.connection, args);
        }
    }
}
