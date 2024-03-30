package ru.project.waygo.dto.point;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PointDTO {
    private long id;
    private String pointName;
    private String address;
    private String city;
    private String description;
    private double longitude;
    private double latitude;
    private List<String> photos;
}
