package guru.sfg.beer.order.service.services.testcomponents;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.brewery.model.events.ValidateOrderRequest;
import guru.sfg.brewery.model.events.ValidateOrderResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class BeerOrderValidationListener {

    private final JmsTemplate jmsTemplate;


    @JmsListener(destination = JmsConfig.VALIDATE_ORDER_QUEUE)
    public void list(Message message) {

        boolean isValid = true;
        boolean sendResponse = true;

        // parsed to validateOrderRequest using Jackson
        ValidateOrderRequest validateOrderRequest = (ValidateOrderRequest) message.getPayload();

        System.out.println("I ran! ###########################################################");

        //condition to fail validation


        if(validateOrderRequest.getBeerOrderDto().getCustomerRef() != null){
            if(validateOrderRequest.getBeerOrderDto().getCustomerRef().equals("fail-validation")){
                isValid = false;
            }else if( validateOrderRequest.getBeerOrderDto().getCustomerRef().equals("dont-validate")){
                sendResponse = false;
            }
        }

        if(sendResponse){
            jmsTemplate.convertAndSend(JmsConfig.VALIDATE_ORDER_RESPONSE_QUEUE,
                    ValidateOrderResult.builder()
                            .isValid(isValid)
                            .orderId(validateOrderRequest
                                    .getBeerOrderDto()
                                    .getId())
                            .build());
        }
    }

}
