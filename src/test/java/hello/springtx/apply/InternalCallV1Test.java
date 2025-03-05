package hello.springtx.apply;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@SpringBootTest
public class InternalCallV1Test {
    @Autowired
    private CallService callService;

    @TestConfiguration
    static class InternalCallV1TestConfig{
        @Bean
        public CallService callService(){
            return new CallService();
        }
    }

    @Slf4j
    static class CallService{
        void external(){
            log.info("external call");
            printTxInfo();
            internal();
        }
        @Transactional
        void internal(){
            log.info("internal call");
            printTxInfo();
        }
        void printTxInfo(){
            boolean txActive
                    = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("trace active result = {}", txActive);
            boolean readOnly
                    = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
            log.info("read only result = {}", readOnly);
        }
    }

    @Test
    void printProxy(){
        log.info("callService class = {}", callService.getClass());
    }
    @Test
    void internalCall(){
        callService.internal();
    }
    @Test
    void externalCall(){
        callService.external();
    }
}
