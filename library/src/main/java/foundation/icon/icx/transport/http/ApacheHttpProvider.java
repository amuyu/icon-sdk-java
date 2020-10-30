package foundation.icon.icx.transport.http;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import foundation.icon.icx.Provider;
import foundation.icon.icx.Request;
import foundation.icon.icx.transport.jsonrpc.RpcConverter;
import foundation.icon.icx.transport.jsonrpc.RpcItem;
import foundation.icon.icx.transport.jsonrpc.RpcItemSerializer;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;

public class ApacheHttpProvider implements Provider {

    private final RequestConfig requestConfig;
    private final String url;

    public ApacheHttpProvider(RequestConfig requestConfig, String url) {
        this.requestConfig = requestConfig;
        this.url = url;
    }

    public ApacheHttpProvider(String url) {
        this(RequestConfig.DEFAULT, url);
    }

    @Override
    public <O> Request<O> request(foundation.icon.icx.transport.jsonrpc.Request request, RpcConverter<O> converter) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        SimpleModule module = new SimpleModule();
        module.addSerializer(RpcItem.class, new RpcItemSerializer());
        mapper.registerModule(module);
        try {
            String r = mapper.writeValueAsString(request);
            StringEntity requestEntity = new StringEntity(r, "utf-8");
            requestEntity.setContentType(new BasicHeader("Content-Type", "application/json"));
            HttpPost httpPost = new HttpPost(url);
            httpPost.setEntity(requestEntity);
            httpPost.setConfig(requestConfig);
            return new ApacheHttpCall<>(httpPost, converter);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert json-rpc request");
        }
    }

}
