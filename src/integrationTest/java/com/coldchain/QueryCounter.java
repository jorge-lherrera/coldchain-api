package com.coldchain;

import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration(proxyBeanMethods = false)
class QueryCounter {

    private static final AtomicInteger STATEMENTS = new AtomicInteger();

    private static final String PREPARE = "prepareStatement";

    private static final String CREATE = "createStatement";

    static void reset() {
        STATEMENTS.set(0);
    }

    static int counted() {
        return STATEMENTS.get();
    }

    @Bean
    static BeanPostProcessor countEveryStatement() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String name) {
                return bean instanceof DataSource dataSource ? new Counting(dataSource) : bean;
            }
        };
    }

    private record Counting(DataSource delegate) implements DataSource {

        @Override
        public Connection getConnection() throws SQLException {
            return watch(delegate.getConnection());
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return watch(delegate.getConnection(username, password));
        }

        @Override
        public PrintWriter getLogWriter() throws SQLException {
            return delegate.getLogWriter();
        }

        @Override
        public void setLogWriter(PrintWriter writer) throws SQLException {
            delegate.setLogWriter(writer);
        }

        @Override
        public void setLoginTimeout(int seconds) throws SQLException {
            delegate.setLoginTimeout(seconds);
        }

        @Override
        public int getLoginTimeout() throws SQLException {
            return delegate.getLoginTimeout();
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            return delegate.getParentLogger();
        }

        @Override
        public <T> T unwrap(Class<T> type) throws SQLException {
            return delegate.unwrap(type);
        }

        @Override
        public boolean isWrapperFor(Class<?> type) throws SQLException {
            return delegate.isWrapperFor(type);
        }

        private static Connection watch(Connection connection) {
            InvocationHandler counting = (proxy, method, arguments) -> {
                if (method.getName().startsWith(PREPARE) || method.getName().equals(CREATE)) {
                    STATEMENTS.incrementAndGet();
                }
                return method.invoke(connection, arguments);
            };
            return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                    new Class<?>[] {Connection.class}, counting);
        }
    }
}
