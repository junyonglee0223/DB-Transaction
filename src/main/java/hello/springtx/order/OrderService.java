package hello.springtx.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class OrderService {
    private final OrderRepository orderRepository;

    @Transactional
    void order(Order order) throws NotEnoughMoneyException{
        log.info("order process starts!!!!");

        orderRepository.save(order);

        if(order.getUsername().equals("exception")){
            log.info("exception");
            throw new RuntimeException();
        }
        else if(order.getUsername().equals("notEnough")){
            order.setPayStatus("wait");
            log.info("not enough money exception");
            throw new NotEnoughMoneyException("not enough money");
        }
        else{
            order.setPayStatus("complete");
            log.info("complete pay process");
        }

        log.info("order process finish!!!!");
    }

}
