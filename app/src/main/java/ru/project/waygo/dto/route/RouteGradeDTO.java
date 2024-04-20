package ru.project.waygo.dto.route;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RouteGradeDTO {
    private long id;
    private int grade;

    public RouteGradeDTO(int grade) {
        this.grade = grade;
    }
}
