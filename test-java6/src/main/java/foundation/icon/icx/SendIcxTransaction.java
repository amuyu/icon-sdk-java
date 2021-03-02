package foundation.icon.icx;

import foundation.icon.icx.data.Address;
import foundation.icon.icx.data.Bytes;
import foundation.icon.icx.data.IconAmount;
import foundation.icon.icx.transport.http.ApacheHttpProvider;
import foundation.icon.icx.transport.http.HttpProvider;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.apache.http.client.config.RequestConfig;

import java.io.IOException;
import java.math.BigInteger;

public class SendIcxTransaction {

    public final String URL = "http://host.docker.internal:9000/api/v3";
//    public final String URL = "http://localhost:9000/api/v3";
    public final String PRIVATE_KEY_STRING =
            "592eb276d534e2c41a2d9356c0ab262dc233d87e4dd71ce705ec130a8d27ff0c";

    private IconService iconService;
    private Wallet wallet;

    public SendIcxTransaction() {
        // okhttpclient
//        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
//        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
//        OkHttpClient httpClient = new OkHttpClient.Builder()
//                .addInterceptor(logging)
//                .build();
//        iconService = new IconService(new HttpProvider(httpClient, URL));

        // apache
        RequestConfig requestConfig = RequestConfig.DEFAULT;
        iconService = new IconService(new ApacheHttpProvider(requestConfig, URL));
        wallet = KeyWallet.load(new Bytes(PRIVATE_KEY_STRING));
    }

    public void sendTransaction() throws IOException {
        BigInteger networkId = new BigInteger("3");
        Address fromAddress = wallet.getAddress();
        Address toAddress = new Address("hx4873b94352c8c1f3b2f09aaeccea31ce9e90bd31");

        BigInteger value = IconAmount.of("1", IconAmount.Unit.ICX).toLoop();
        BigInteger stepLimit = new BigInteger("1000000");
        long timestamp = System.currentTimeMillis() * 1000L;
        BigInteger nonce = new BigInteger("1");

        Transaction transaction = TransactionBuilder.newBuilder()
                .nid(networkId)
                .from(fromAddress)
                .to(toAddress)
                .value(value)
                .stepLimit(stepLimit)
                .timestamp(new BigInteger(Long.toString(timestamp)))
                .nonce(nonce)
                .build();

        SignedTransaction signedTransaction = new SignedTransaction(transaction, wallet);
        Bytes hash = iconService.sendTransaction(signedTransaction).execute();
        System.out.println("txHash:"+hash);
    }

    public static void main(String[] args) throws IOException {
        new SendIcxTransaction().sendTransaction();
    }
}
