package hello.springtx.apply;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@SpringBootTest
public class TestBasicTest {
    @Autowired
    BasicService basicService;

    @TestConfiguration
    static class TxApplyBasicConfig{
        @Bean
        public BasicService basicService(){
            return new BasicService();
        }
    }
    static class BasicService{
        @Transactional
        public void tx(){
            log.info("tx method starts");
            boolean traceActive
                    = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("trace active result = {}", traceActive);
        }
        public void nonTx(){
            log.info("nonTx method starts");
            boolean traceActive
                    = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("trace active result = {}", traceActive);
        }
    }

    @Test
    void proxyCheck(){
        log.info("basic service = {}", basicService.getClass());
        Assertions.assertThat(AopUtils.isAopProxy(basicService)).isTrue();
    }

    @Test
    void txTest(){
        basicService.tx();
        basicService.nonTx();
    }
}
