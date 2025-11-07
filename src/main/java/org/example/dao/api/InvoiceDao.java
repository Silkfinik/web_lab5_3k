package org.example.dao.api;

import org.example.entity.Invoice;
import java.util.List;

public interface InvoiceDao {
    List<Invoice> findBySubscriberId(int subscriberId);
    boolean pay(int invoiceId);
    Integer findSubscriberIdByInvoiceId(int invoiceId);
    List<Invoice> findUnpaid();
    Invoice add(Invoice invoice);
}