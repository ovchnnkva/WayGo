package ru.project.utils;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import ru.project.waygo.dto.point.PointDTO;
import ru.project.waygo.dto.route.RouteDTO;

public class IntentExtraUtils {

    public static String getPointsExtra(List<PointDTO> points) {
        return points
                .stream()
                .map(point -> point.getId() + ":" + point.getPointName() + ":" + point.getDescription())
                .collect(Collectors.joining(","));
    }

    public static String getRoutesExtra(List<RouteDTO> points) {
        return points
                .stream()
                .map(point -> point.getId() + ":" + point.getRouteName())
                .collect(Collectors.joining(","));
    }

    /**
     * [0] - id
     * [1] - name
     * [2] - description
     * @param extra в формате id:name:description
     * @return DTO для отображения данных о точках в деталях
     */
    public static List<PointDTO> getPointsFromExtra(@NotNull String extra) {
        List<String> data = Arrays.stream(extra.split(",")).collect(Collectors.toList());

        return data.stream()
                .map(d -> PointDTO.builder()
                        .id(Long.parseLong(d.split(":")[0]))
                        .pointName(d.split(":")[1])
                        .description(d.split(":")[2])
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * [0] - id
     * [1] - name
     * @param extra в формате id:name
     * @return DTO для отображение данных о маршруте в деталях точки
     */
    public static List<RouteDTO> getRoutesFromExtra(@NotNull String extra) {
        if(extra.isEmpty()) new RouteDTO();

        List<String> data = Arrays.stream(extra.split(",")).collect(Collectors.toList());

        return data.stream()
                .map(d -> RouteDTO.builder()
                .id(Long.parseLong(d.split(":")[0]))
                .routeName(d.split(":")[1])
                .build())
                .collect(Collectors.toList());
    }
}
