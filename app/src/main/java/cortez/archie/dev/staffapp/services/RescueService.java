package cortez.archie.dev.staffapp.services;

import cortez.archie.dev.staffapp.models.Center;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * Created by Administrator on 10/16/2017.
 */

public interface RescueService {

    @GET("centers/{id}/")
    Call<Center> getCenterInfo(@Path("id") String id);

}
