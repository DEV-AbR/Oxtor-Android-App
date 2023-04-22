package teamarmada.oxtor.Repository;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

public class FunctionsRepository {

    public static final String TAG=FunctionsRepository.class.getSimpleName();
    private static FunctionsRepository functionsRepository=null;
    private final FirebaseFunctions functions;
    public static final String SHARE_BY_EMAIL="https://asia-south1-oxtor-910e3.cloudfunctions.net/shareByEmail";
    public static final String CHECK_USERNAME="https://asia-south1-oxtor-910e3.cloudfunctions.net/checkUsername";

    private FunctionsRepository(){
        functions=FirebaseFunctions.getInstance();
    }

    public synchronized static FunctionsRepository getInstance(){
        if(functionsRepository==null)
            functionsRepository=new FunctionsRepository();
        return functionsRepository;
    }

    public Task<HttpsCallableResult> shareByEmail(HashMap<String,Object> payload)  {
        try{
            URL url=new URL(SHARE_BY_EMAIL);
            return functions.getHttpsCallableFromUrl(url).call(payload);
        }catch (MalformedURLException e){
            return functions.getHttpsCallable("shareByEmail").call(payload);
        }
    }

    public Task<HttpsCallableResult> updateUsername(HashMap<String,Object> payload)  {
        try{
            URL url=new URL(CHECK_USERNAME);
            return functions.getHttpsCallableFromUrl(url).call(payload);
        }catch (MalformedURLException e){
            return functions.getHttpsCallable("checkUsername").call(payload);
        }
    }


}
