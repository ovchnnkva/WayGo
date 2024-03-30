package ru.project.waygo.dto.subscription;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionTransactionDTO {
    private long id;
    private Date transactionDate;
    private boolean status;
}
