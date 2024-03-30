package ru.project.waygo.dto.subscription;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionPlanDTO {
    private long id;
    private String name;
    private double cost;
    private long duration;
}
