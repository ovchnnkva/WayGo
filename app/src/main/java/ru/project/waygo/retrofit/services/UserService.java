package ru.project.waygo.retrofit.services;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import ru.project.waygo.dto.point.PointCheckInDTO;
import ru.project.waygo.dto.route.RouteCheckInDTO;
import ru.project.waygo.dto.route.RouteGradeDTO;
import ru.project.waygo.dto.user.UserDTO;

public interface UserService {
    @GET("api/user/{locationId}")
    Call<UserDTO> getById(@Path("id") long id);

    @POST("api/user/estimate")
    Call<Void> createRateRoute(@Query("userId") long userId, @Query("routeId") long routeId, @Query("dto") RouteGradeDTO dto);

    @POST("api/user/route/chekin")
    Call<Void> createCheckInOnRoute(@Query("userId") long userId, @Query("routeId") long routeId, @Query("dto") RouteCheckInDTO dto);

    @POST("api/user/point/checkin")
    Call<Void> createCheckInOnPoint(@Query("userId") long userId, @Query("routeId") long routeId, @Query("dto") PointCheckInDTO dto);

    @POST("api/user/points/favorite")
    Call<Void> createFavoritePoint(@Query("userId") long userId, @Query("locationId") long pointId);

    @POST("api/user")
    Call<Void> createUser(@Query("dto") UserDTO dto);
}
