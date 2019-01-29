package creating_object;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import http.Api;

import java.util.Optional;

public class StringToObject {

    public <T> T createResponseObjectWithUnwrapRootValue(String response, Class cl){

        try {
            return new ObjectMapper().enable(DeserializationFeature.UNWRAP_ROOT_VALUE)
                    .readerFor(cl).readValue(response);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public <T> T createResponseObj(String response, Class cl){

        if (response != null && !response.isEmpty()) {
            try {
                return new ObjectMapper().readerFor(cl).readValue(Optional.of(response).get());

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return null;
    }

    public <T> T createResponseObjectWithUnwrapRootValue(String response, Api api){

        try {
            return new ObjectMapper().enable(DeserializationFeature.UNWRAP_ROOT_VALUE)
                    .readerFor(api.getClass()).readValue(response);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T createResponseObj(String response, Api api){

        if (response != null && !response.isEmpty()) {
            try {
                return new ObjectMapper().readerFor(api.getClassForResponse()).readValue(Optional.of(response).get());

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return null;
    }
}
