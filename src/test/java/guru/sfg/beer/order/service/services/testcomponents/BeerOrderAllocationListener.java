package guru.sfg.beer.order.service.services.testcomponents;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.brewery.model.events.AllocateOrderRequest;
import guru.sfg.brewery.model.events.AllocateOrderResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class BeerOrderAllocationListener {

    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = JmsConfig.ALLOCATE_ORDER_QUEUE)
    public void listen(Message message) {
        AllocateOrderRequest allocateOrderRequest = (AllocateOrderRequest) message.getPayload();
        boolean pendingInventory = false;
        boolean allocationError = false;
        boolean sendResponse = true;

        // set pending inventory
        if (allocateOrderRequest.getBeerOrderDto().getCustomerRef() != null && allocateOrderRequest.getBeerOrderDto().getCustomerRef().equals("partial-allocation")) {
            pendingInventory = true;
        }

        // set allocation failed
        if (allocateOrderRequest.getBeerOrderDto().getCustomerRef() != null && allocateOrderRequest.getBeerOrderDto().getCustomerRef().equals("fail-allocation")) {
            allocationError = true;
        } else if (allocateOrderRequest.getBeerOrderDto().getCustomerRef() != null && allocateOrderRequest.getBeerOrderDto().getCustomerRef().equals("dont-allocate")) {
            sendResponse = false;
        }

        boolean finalPendingInventory = pendingInventory;
        allocateOrderRequest.getBeerOrderDto().getBeerOrderLines()
                .forEach(beerOrderLineDto -> {
                    if (finalPendingInventory) {
                        beerOrderLineDto.setQuantityAllocated(beerOrderLineDto.getOrderQuantity() - 1);
                    } else {
                        beerOrderLineDto.setQuantityAllocated(beerOrderLineDto.getOrderQuantity());
                    }
                });

        if (sendResponse) {
            jmsTemplate.convertAndSend(JmsConfig.ALLOCATE_ORDER_RESPONSE_QUEUE,
                    AllocateOrderResult.builder()
                            .beerOrderDto(allocateOrderRequest.getBeerOrderDto())
                            .pendingInventory(pendingInventory)
                            .allocationError(allocationError)
                            .build());
        }
    }
}
