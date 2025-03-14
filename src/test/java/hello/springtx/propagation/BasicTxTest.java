package hello.springtx.propagation;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;

import javax.sql.DataSource;

@SpringBootTest
@Slf4j
public class BasicTxTest {
    @Autowired
    PlatformTransactionManager txManager;

    @TestConfiguration
    static class BasicTxTestConfig{
        @Bean
        public PlatformTransactionManager platformTransactionManager(DataSource dataSource){
            return new DataSourceTransactionManager(dataSource);
        }
    }

    @Test
    void commitTest(){
        log.info("transaction process starts!!!!");
        TransactionStatus ts = txManager.getTransaction(new DefaultTransactionAttribute());

        log.info("commit starts!!");
        txManager.commit(ts);
        log.info("commit ends!!");
    }

    @Test
    void rollbackTest(){
        log.info("transaction process starts!!!!");
        TransactionStatus ts = txManager.getTransaction(new DefaultTransactionAttribute());

        log.info("rollback starts!!");
        txManager.rollback(ts);
        log.info("rollback ends!!");
    }

    @Test
    void doubleCommitTest(){
        log.info("transaction process starts!!!!");
        TransactionStatus ts1 = txManager.getTransaction(new DefaultTransactionAttribute());

        log.info("commit 1 starts!!!");
        txManager.commit(ts1);
        log.info("commit 1 ends!!!");

        TransactionStatus ts2 = txManager.getTransaction(new DefaultTransactionAttribute());

        log.info("commit 2 starts!!!");
        txManager.commit(ts2);
        log.info("commit 2 ends!!!");

        log.info("ts1 = {}, ts2 = {}", ts1, ts2);
    }

    @Test
    void doubleCommitRollbackTest(){
        log.info("transaction process starts!!!!");

        TransactionStatus ts1 = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("ts 1 commit starts!!!");
        txManager.commit(ts1);
        log.info("ts 1 commit ends!!!");

        TransactionStatus ts2 = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("ts 2 rollback starts!!!");
        txManager.rollback(ts2);
        log.info("ts 2 rollback ends!!!");
    }


}
