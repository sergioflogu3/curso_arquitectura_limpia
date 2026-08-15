package com.atlas.bank.atlas_bank.transaction.service.listener;

import com.atlas.bank.atlas_bank.transaction.service.event.TransactionExecuteEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AuditListener {
    @EventListener
    public void onTransactionExecuted(TransactionExecuteEvent event){
        log.info("Registrando la auditoria - {} de cuenta #{} a cuenta #{} por ${}"
                , event.type(), event.sourceAccountId(), event.targetAccountId(), event.amount());
    }
}
