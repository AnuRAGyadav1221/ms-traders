package com.ecom.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ecom.model.ProductOrder;
import com.ecom.model.RefundAudit;

public interface RefundAuditRepository extends JpaRepository<RefundAudit, Long> {
    List<RefundAudit> findByProductOrder(ProductOrder order);
}
