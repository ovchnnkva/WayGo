package ru.project.waygo.dto.point;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Objects;

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
    private List<String> photo;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PointDTO pointDTO = (PointDTO) o;
        return id == pointDTO.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
