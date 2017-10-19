package cortez.archie.dev.staffapp.services;

import cortez.archie.dev.staffapp.models.Center;
import cortez.archie.dev.staffapp.models.CheckIn;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Created by Administrator on 10/16/2017.
 */

public interface RescueService {

    @GET("centers/{id}/")
    Call<Center> getCenterInfo(@Path("id") String id);

    @POST("people/{id}/check_in/")
    Call<ResponseBody> uploadOneCheckin(@Path("id") String id, @Body CheckIn checkIn);

}
