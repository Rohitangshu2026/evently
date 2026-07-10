package com.evently.domain;

import com.evently.domain.enums.TicketValidationMethodEnum;
import com.evently.domain.enums.TicketValidationStatusEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ticket_validations")
@Getter
@Setter
@NoArgsConstructor
public class TicketValidation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TicketValidationStatusEnum status;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 32)
    private TicketValidationMethodEnum method;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validated_by")
    private User validatedBy;

    @CreationTimestamp
    @Column(name = "validation_datetime", nullable = false, updatable = false)
    private Instant validationDateTime;

    @Override
    public boolean equals(Object o){
        if(this == o) return true;
        if(!(o instanceof TicketValidation other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode(){
        return getClass().hashCode();
    }
}
