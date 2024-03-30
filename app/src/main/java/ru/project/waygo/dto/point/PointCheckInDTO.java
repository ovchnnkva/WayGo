package ru.project.waygo.dto.point;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PointCheckInDTO {
    private long id;
    private Date date;
}
