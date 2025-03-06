package hello.springtx.apply;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@SpringBootTest
public class InitTxTest {
    @Autowired
    private InitClass initClass;

    @TestConfiguration
    static class InitTxTestConfig{
        @Bean
        public InitClass initClass(){
            return new InitClass();
        }
    }

    @Slf4j
    static class InitClass{
        @PostConstruct
        @Transactional
        void initV1(){
            boolean isActive
                    = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("init V1 transaction result = {}", isActive);
        }

        @Transactional
        @EventListener(ApplicationReadyEvent.class)
        void initV2(){
            boolean isActive
                    = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("init V2 transaction result = {}", isActive);
        }
    }

    @Test
    void initTestV1(){
        initClass.initV1();
    }
    @Test
    void initTestV2(){
        initClass.initV2();
    }
    @Test
    void initTest(){

    }
}
