package hello.springtx.apply;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@SpringBootTest
@Slf4j
public class InternalCallV2Test {
    @Autowired
    private CallService callService;
    @Autowired
    private InternalService internalService;

    @TestConfiguration
    static class InternalCallV2TestConfig{
        @Bean
        public CallService callService(){
            return new CallService(internalService());
        }
        @Bean
        public InternalService internalService(){
            return new InternalService();
        }
    }

    @Slf4j
    @RequiredArgsConstructor
    static class CallService{
        private final InternalService internalService;
        void external(){
            log.info("external call");
            printTxInfo();
            internalService.internal();
        }
        void printTxInfo(){
            boolean traceActive
                    = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("trace active result = {}", traceActive);
        }

    }
    @Slf4j
    static class InternalService{
        @Transactional
        void internal(){
            log.info("internal call");
            printTxInfo();
        }
        void printTxInfo(){
            boolean traceActive
                    = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("trace active result = {}", traceActive);
        }
    }

    @Test
    void externalCallV2(){
        callService.external();
    }
}
