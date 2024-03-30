package ru.project.waygo.dto.route;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.project.waygo.dto.point.PointDTO;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RouteDTO {
    private long id;
    private long length;
    private String city;
    private String description;
    private String routeName;
    private Set<RouteGradeDTO> routeGrades;
    private Set<PointDTO> stopsOnRoute;
}
