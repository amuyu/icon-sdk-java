package foundation.icon.icx.transport.http;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import foundation.icon.icx.Callback;
import foundation.icon.icx.Request;
import foundation.icon.icx.transport.jsonrpc.*;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

public class ApacheHttpCall<T> implements Request<T> {

    private final HttpClient httpClient;
    private final HttpPost httpPost;
    private final RpcConverter<T> converter;

    public ApacheHttpCall(HttpPost httpPost, RpcConverter<T> converter) {
        this.httpClient = HttpClients.createDefault();
        this.httpPost = httpPost;
        this.converter = converter;
    }

    @Override
    public T execute() throws IOException {
        return convertResponse(httpClient.execute(httpPost));
    }

    @Override
    public void execute(Callback<T> callback) {
        try {
            httpClient.execute(httpPost, new ResponseHandler<T>() {
                @Override
                public T handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
                    int status = response.getStatusLine().getStatusCode();
                    if (status >= 200 && status < 300) {
                        try {
                            T result = convertResponse(response);
                            callback.onSuccess(result);
                            return result;
                        } catch (IOException e) {
                            callback.onFailure(e);
                        }
                    } else {
                        throw new ClientProtocolException("Unexpected response status: " + status);
                    }
                    return null;
                }
            });
        } catch (IOException e) {
            callback.onFailure(e);
        }
    }

    // converts the response data from the okhttp response
    private T convertResponse(HttpResponse httpResponse) throws IOException {
        HttpEntity body = httpResponse.getEntity();
        if (body != null) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.registerModule(createDeserializerModule());
            String content = EntityUtils.toString(body);
            Response response = mapper.readValue(content, Response.class);
            if (converter == null) {
                throw new IllegalArgumentException("There is no converter for response:'" + content + "'");
            }
            if (response.getResult() != null) {
                return converter.convertTo(response.getResult());
            } else {
                throw response.getError();
            }
        } else {
            throw new RpcError(httpResponse.getStatusLine().getStatusCode(), httpResponse.getStatusLine().getReasonPhrase());
        }
    }

    private SimpleModule createDeserializerModule() {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(RpcItem.class, new RpcItemDeserializer());
        return module;
    }
}
