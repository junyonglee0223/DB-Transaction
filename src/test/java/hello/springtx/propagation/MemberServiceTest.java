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

    /*
     * memberService @Transaction - OFF
     * memberRepository @Transaction ON
     * logRepository @Transaction - ON
     * */
    @Test
    void outerTxOff_success(){
        String username = "outerTxOff_success";
        memberService.joinV1(username);

        Assertions.assertTrue(memberRepository.find(username).isPresent());
        Assertions.assertTrue(logRepository.find(username).isPresent());
    }

    /*
    * memberService @Transaction - OFF
    * memberRepository @Transaction ON
    * logRepository @Transaction - ON   - exception occurs
    * */
    @Test
    void outerTxOff_fail(){
        String username = "log exception test";
        org.assertj.core.api.Assertions.assertThatThrownBy(()
                -> memberService.joinV1(username)).isInstanceOf(RuntimeException.class);

        Assertions.assertTrue(memberRepository.find(username).isPresent());
        Assertions.assertTrue(logRepository.find(username).isEmpty());
    }

    /*
     * memberService @Transaction - ON
     * memberRepository @Transaction OFF
     * logRepository @Transaction - OFF
     * */
    @Test
    void singleTx(){
        String username = "single tx test";
        memberService.joinV1(username);

        Assertions.assertTrue(memberRepository.find(username).isPresent());
        Assertions.assertTrue(logRepository.find(username).isPresent());
    }

    /*
     * memberService @Transaction - ON
     * memberRepository @Transaction ON
     * logRepository @Transaction - ON
     * */
    @Test
    void outerTxOn_success(){
        String username = "outerTxOn_success";
        memberService.joinV1(username);

        Assertions.assertTrue(memberRepository.find(username).isPresent());
        Assertions.assertTrue(logRepository.find(username).isPresent());
    }
    /*
     * memberService @Transaction - ON
     * memberRepository @Transaction ON
     * logRepository @Transaction - ON   - exception occurs
     * */
    @Test
    void outerTxOn_fail(){
        String username = "log exception outerTxOn_fail";
        //memberService.joinV1(username);
        org.assertj.core.api.Assertions.assertThatThrownBy(()
                -> memberService.joinV1(username)).isInstanceOf(RuntimeException.class);

        Assertions.assertTrue(memberRepository.find(username).isEmpty());
        Assertions.assertTrue(logRepository.find(username).isEmpty());
    }

    /*
     * memberService @Transaction - ON
     * memberRepository @Transaction ON
     * logRepository @Transaction - ON   - exception occurs
     * */
    @Test
    void recoverException_fail(){
        String username = "log exception recoverException_fail";
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                memberService.joinV2(username)).isInstanceOf(RuntimeException.class);

        Assertions.assertTrue(memberRepository.find(username).isEmpty());
        Assertions.assertTrue(logRepository.find(username).isEmpty());
    }

    /*
     * memberService @Transaction - ON
     * memberRepository @Transaction ON
     * logRepository @Transaction(REQUIRES_NEW) - ON   - exception occurs
     * */
    @Test
    void recoverException_success(){
        String username = "log exception recoverException_fail";
//        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
//                memberService.joinV2(username)).isInstanceOf(RuntimeException.class);

        memberService.joinV2(username);
        Assertions.assertTrue(memberRepository.find(username).isPresent());
        Assertions.assertTrue(logRepository.find(username).isEmpty());
    }
}