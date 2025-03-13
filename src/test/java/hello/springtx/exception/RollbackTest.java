package hello.springtx.exception;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
public class RollbackTest {
    @Slf4j
    static class RollbackService{
        @Transactional
        public void runtimeException(){
            log.info("runtime exception method");
            throw new RuntimeException();
        }
        @Transactional
        public void checkedException() throws MyException{
            log.info("checked exception");
            throw new MyException();
        }
        @Transactional(rollbackFor = MyException.class)
        public void rollbackFor() throws MyException{
            log.info("rollback for");
            throw new MyException();
        }
    }

    @Autowired
    RollbackService rollbackService;

    @TestConfiguration
    static class RollbackTestConfig{
        @Bean
        public RollbackService rollbackService(){
            return new RollbackService();
        }
    }

    static class MyException extends Exception{}

    @Test
    void runtimeException(){
        Assertions.assertThatThrownBy(() -> rollbackService.runtimeException())
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void checkedException(){
        Assertions.assertThatThrownBy(() -> rollbackService.checkedException())
                .isInstanceOf(MyException.class);
    }

    @Test
    void rollbackFor(){
        Assertions.assertThatThrownBy(() -> rollbackService.rollbackFor())
                .isInstanceOf(MyException.class);
    }

}
