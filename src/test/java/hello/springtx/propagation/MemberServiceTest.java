package hello.springtx.propagation;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.Assert;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Slf4j
class MemberServiceTest {
    @Autowired
    private MemberService memberService;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private LogRepository logRepository;

    @Test
    void outerTxOff_success(){
        String username = "outerTxOff_success";
        memberService.joinV1(username);

        Assertions.assertTrue(memberRepository.find(username).isPresent());
        Assertions.assertTrue(logRepository.find(username).isPresent());
    }

    @Test
    void outerTxOff_fail(){
        String username = "log exception test";
        org.assertj.core.api.Assertions.assertThatThrownBy(()
                -> memberService.joinV1(username)).isInstanceOf(RuntimeException.class);

        Assertions.assertTrue(memberRepository.find(username).isPresent());
        Assertions.assertTrue(logRepository.find(username).isEmpty());
    }
}