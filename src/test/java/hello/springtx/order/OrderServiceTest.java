package hello.springtx.order;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Slf4j
class OrderServiceTest {
    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderRepository orderRepository;

    @Test
    void complete() throws NotEnoughMoneyException {
        Order order = new Order();
        order.setUsername("normal");

        orderService.order(order);

        Order findOrder = orderRepository.findById(order.getId()).get();
        Assertions.assertThat(findOrder.getPayStatus()).isEqualTo("complete");
    }
    @Test
    void runtimeException(){
        Order order = new Order();
        order.setUsername("exception");

        Assertions.assertThatThrownBy(() -> orderService.order(order))
                .isInstanceOf(RuntimeException.class);

        Optional<Order> orderOptional = orderRepository.findById(order.getId());
        Assertions.assertThat(orderOptional.isEmpty()).isTrue();
    }

    @Test
    void notEnoughMoney(){
        Order order = new Order();
        order.setUsername("notEnough");
        try{
            orderService.order(order);
            fail("not enough money exception has to occur");//해당 코드 도달 시 오류 무조건 오류 발생 상태여야 함
        }catch (NotEnoughMoneyException e){
            log.info("not enough money order log = {}", order);
        }

        Order findOrder = orderRepository.findById(order.getId()).get();
        Assertions.assertThat(findOrder.getPayStatus()).isEqualTo("wait");
    }

}