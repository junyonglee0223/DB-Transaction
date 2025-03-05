package hello.springtx.apply;

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
public class TxLevelTest {
    @Autowired
    private LevelService levelService;

    @TestConfiguration
    public static class TxLevelConfig{
        @Bean
        public LevelService levelService(){
            return new LevelService();
        }
    }

    @Slf4j
    @Transactional(readOnly = true)
    public static class LevelService{
        @Transactional(readOnly = false)
        public void write(){
            log.info("write call");
            printTxInfo();
        }
        public void read(){
            log.info("read call");
            printTxInfo();
        }
        public void printTxInfo(){
            boolean txActive
                    = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("transactional active = {}", txActive);
            boolean readOnly
                    = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
            log.info("transaction readonly = {}", readOnly);
        }
    }

    @Test
    void orderTest(){
        levelService.write();
        levelService.read();
    }
}
