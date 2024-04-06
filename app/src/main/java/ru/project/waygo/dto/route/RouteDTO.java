package ru.project.waygo.dto.route;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.project.waygo.dto.point.PointDTO;
import ru.project.waygo.fragment.LocationFragment;

import java.util.List;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RouteDTO {
    private long id;
    private long length;
    private String city;
    private String description;
    private String routeName;
    private Set<RouteGradeDTO> routeGrades;
    private List<PointDTO> stopsOnRoute;

    public RouteDTO(LocationFragment fragment) {
        this.id = fragment.getLocationId();
        this.routeName = fragment.getName();
    }
}
