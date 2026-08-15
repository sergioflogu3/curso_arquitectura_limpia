package com.atlas.bank.atlas_bank.transaction.service.listener;

import com.atlas.bank.atlas_bank.transaction.service.event.TransactionExecuteEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NotificationListener {
    @EventListener
    public void onTransactionExecuted(TransactionExecuteEvent event){
        log.info("Enviando comprobante de {} por ${} - Transaction #{}",
                event.type(), event.amount(), event.transactionId());
    }
}
