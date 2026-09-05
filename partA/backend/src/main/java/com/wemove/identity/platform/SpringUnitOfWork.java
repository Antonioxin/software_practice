package com.wemove.identity.platform;

import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class SpringUnitOfWork implements UnitOfWork {
    private final TransactionTemplate transactions;
    public SpringUnitOfWork(TransactionTemplate transactions) { this.transactions = transactions; }
    @Override public <T> T run(Supplier<T> work) { return transactions.execute(status -> work.get()); }
}
