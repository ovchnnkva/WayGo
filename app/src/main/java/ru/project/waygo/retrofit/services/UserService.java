package ru.project.waygo.retrofit.services;

import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import ru.project.waygo.dto.point.PointCheckInDTO;
import ru.project.waygo.dto.point.PointDTO;
import ru.project.waygo.dto.route.RouteCheckInDTO;
import ru.project.waygo.dto.route.RouteDTO;
import ru.project.waygo.dto.route.RouteGradeDTO;
import ru.project.waygo.dto.user.UserDTO;

public interface UserService {
    @GET("api/user/{locationId}")
    Call<UserDTO> getById(@Path("id") long id);

    @GET("api/user")
    Call<UserDTO> getByUid(@Query("uuid") String uid);

    @POST("api/user/estimate")
    Call<Void> createRateRoute(@Query("userId") long userId, @Query("routeId") long routeId, @Body RouteGradeDTO dto);

    @PUT("api/user")
    Call<Void> updateUser(@Body UserDTO dto);

    @POST("api/user/route/chekin")
    Call<Void> createCheckInOnRoute(@Query("userId") long userId, @Query("routeId") long routeId, @Query("dto") RouteCheckInDTO dto);

    @POST("api/user/point/checkin")
    Call<Void> createCheckInOnPoint(@Query("userId") long userId, @Query("routeId") long routeId, @Query("dto") PointCheckInDTO dto);

    @POST("api/user/points/favourite")
    Call<Void> createFavoritePoint(@Query("userId") long userId, @Query("locationId") long pointId);

    @POST("api/user")
    Call<Void> createUser(@Body UserDTO dto);

    @GET("api/user/points/favourite")
    Call<Set<PointDTO>> getFavoritePoints(@Query("userId") long userId);

    @POST("api/user/points/favourite")
    Call<Void> saveFavoritePoint(@Query("userId") long userId, @Query("pointId") long pointId);

    @GET("api/user/routes/favourite")
    Call<Set<RouteDTO>> getFavoriteRoutes(@Query("userId") long userId);

    @POST("api/user/routes/favourite")
    Call<Void> saveFavoriteRoute(@Query("userId") long userId, @Query("routeId") long routeId);

    @DELETE("api/user/points/favourite")
    Call<Void> deleteFavoritePoint(@Query("userId") long userId, @Query("pointId") long pointId);

    @DELETE("api/user/routes/favourite")
    Call<Void> deleteFavoriteRoute(@Query("userId") long userId, @Query("routeId") long routeId);

    @GET("api/user/points/favourite/ids")
    Call<List<Long>> getFavoritePointsIds(@Query("userId") long userId);

    @GET("api/user/routes/favourite/ids")
    Call<List<Long>> getFavoriteRoutesIds(@Query("userId") long userId);
}
