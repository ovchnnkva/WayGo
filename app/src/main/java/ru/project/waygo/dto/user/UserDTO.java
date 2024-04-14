package ru.project.waygo.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.project.waygo.dto.point.PointCheckInDTO;
import ru.project.waygo.dto.point.PointDTO;
import ru.project.waygo.dto.route.RouteCheckInDTO;
import ru.project.waygo.dto.route.RouteDTO;
import ru.project.waygo.dto.route.RouteGradeDTO;


import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDTO {
    private long id;
    private String uid;
    private String email;
    private String name;
    private Set<RouteGradeDTO> routeGrades;
    private Set<RouteCheckInDTO> routeCheckIns;
    private Set<PointCheckInDTO> pointCheckIns;
//    private Set<SubscriptionTransactionDTO> subscriptionTransactions;
//    private Subscription subscription;
    private Set<RouteDTO> favouriteRoutes;
    private Set<PointDTO> favouritePoints;
}
