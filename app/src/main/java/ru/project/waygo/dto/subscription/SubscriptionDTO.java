package ru.project.waygo.dto.subscription;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionDTO {
    private long id;
    private Date startDate;
    private Date endDate;
    private SubscriptionPlanDTO subscriptionPlan;
}
