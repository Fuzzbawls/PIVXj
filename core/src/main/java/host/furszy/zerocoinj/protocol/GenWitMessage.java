package host.furszy.zerocoinj.protocol;

import com.zerocoinj.core.CoinDenomination;
import org.pivxj.core.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Random;

public class GenWitMessage extends Message {

    private BloomFilter bloomFilter;
    private int startHeight;
    private CoinDenomination den;
    private int requestNum;

    public GenWitMessage(NetworkParameters params, byte[] payload){
        super(params,payload,0);
    }

    public GenWitMessage(
            NetworkParameters params,
            int startHeight,
            CoinDenomination den,
            int elements,
            double falsePositiveRate,
            long randomNonce
    ) {
        super(params);
        this.requestNum = 10;
        this.startHeight = startHeight;
        this.den = den;
        this.bloomFilter = new BloomFilter(elements, falsePositiveRate, randomNonce);
    }

    public Integer getRequestNum() {
        return requestNum;
    }

    public void insert(BigInteger data){
        this.bloomFilter.insert(Utils.serializeBigInteger(data));
    }

    public boolean contains(BigInteger data){
        return this.bloomFilter.contains(Utils.serializeBigInteger(data));
    }

    @Override
    protected void parse() throws ProtocolException {
        bloomFilter = new BloomFilter(params, payload);
        cursor = bloomFilter.getMessageSize();
        startHeight = (int) readUint32();
        den = CoinDenomination.fromValue((int) readUint32());
        requestNum = (int) readUint32();
    }

    @Override
    public byte[] bitcoinSerialize() {
        try(ByteArrayOutputStream buf = new ByteArrayOutputStream()){
            bloomFilter.bitcoinSerialize(buf);
            Utils.uint32ToByteStreamLE(startHeight, buf);
            Utils.uint32ToByteStreamLE(den.getDenomination(), buf);
            Utils.uint32ToByteStreamLE(requestNum, buf);
            return buf.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
