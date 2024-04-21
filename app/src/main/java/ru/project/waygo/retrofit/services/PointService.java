package ru.project.waygo.retrofit.services;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import ru.project.waygo.dto.ar.ArMetaInfoDTO;
import ru.project.waygo.dto.point.PointDTO;

public interface PointService {
    @GET("api/point/all")
    Call<List<PointDTO>> getAll();

    @GET("api/point/all")
    Call<List<PointDTO>> getByCity(@Query("city") String city);

    @GET("api/point/{locationId}")
    Call<PointDTO> getById(@Path("id") Long id);

    @GET("api/point")
    Call<PointDTO> getByCityAndName(@Query("city") String city, @Query("pointName") String pointName);

    @GET("api/point/ar")
    Call<ArMetaInfoDTO> getArMetaInfo(@Query("pointId") Long id);

    @GET("api/point/audio")
    Call<String> getAudio(@Query("pointId") long pointId);

    @GET("api/point/coordinates")
    Call<List<PointDTO>> getCoordinatesByCityName(@Query("city") String city);
}
