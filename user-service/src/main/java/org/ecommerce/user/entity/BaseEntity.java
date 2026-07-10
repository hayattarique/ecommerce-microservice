package org.ecommerce.user.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Data
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    @CreatedBy
    protected String createdBy;

    @CreatedDate
    protected LocalDateTime createdAt;

    @LastModifiedBy
    protected String updatedBy;

    @LastModifiedDate
    protected LocalDateTime updatedAt;

    protected LocalDateTime deletedAt;

}
