package org.pivxj.wallet;

import com.zerocoinj.JniBridge;
import com.zerocoinj.core.CoinDenomination;
import com.zerocoinj.core.ZCoin;
import com.zerocoinj.core.context.ZerocoinContext;
import host.furszy.zerocoinj.wallet.MultiWallet;
import org.junit.Assert;
import org.junit.Test;
import org.pivxj.core.*;
import org.pivxj.crypto.DeterministicKey;
import org.pivxj.params.MainNetParams;
import org.pivxj.params.UnitTestParams;
import org.pivxj.testing.TestWithWallet;
import org.spongycastle.util.encoders.Hex;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import static org.pivxj.wallet.DeterministicKeyChain.KeyChainType.BIP44_PIV;
import static org.pivxj.wallet.DeterministicKeyChain.KeyChainType.BIP44_ZPIV;

public class MultiWalletTest extends TestWithWallet {

    @Test
    public void pivAndZpivDerivationPath(){

        NetworkParameters params = MainNetParams.get();

        DeterministicSeed seed = new DeterministicSeed(
                Hex.decode("760a00eda285a842ad99626b61faebb6e36d80decae6665ac9c5f4c17db5185858d9fed30b6cd78a7daff4e07c88bf280cfc595620a4107613b50cab42a32f9b"),
                "",
                System.currentTimeMillis()
        );

        KeyChainGroup keyChainGroupPiv = new KeyChainGroup(params, seed, BIP44_PIV);
        KeyChainGroup keyChainGroupZpiv = new KeyChainGroup(params, seed, BIP44_ZPIV);

        Wallet pivWallet = new Wallet(params,keyChainGroupPiv);
        Wallet zPivWallet = new Wallet(params,keyChainGroupZpiv);


        DeterministicKey keyPiv = pivWallet.freshKey(KeyChain.KeyPurpose.RECEIVE_FUNDS);
        Assert.assertEquals("invalid PIV path", "M/44H/119H/0H/0/0", keyPiv.getPathAsString());

        DeterministicKey keyZpiv = zPivWallet.freshKey(KeyChain.KeyPurpose.RECEIVE_FUNDS);
        System.out.println(keyZpiv.getPathAsString());
        Assert.assertEquals("Invalid zPIV path", "M/44H/37361148H/0H/0/0", keyZpiv.getPathAsString());

        PeerGroup peerGroup = new PeerGroup(params);
        peerGroup.addWallet(pivWallet);
        peerGroup.addWallet(zPivWallet);
    }

    @Test
    public void generateZPIV(){
        try {
            NetworkParameters params = UnitTestParams.get();
            setUp();
            DeterministicSeed seed = new DeterministicSeed(
                    Hex.decode("760a00eda285a842ad99626b61faebb6e36d80decae6665ac9c5f4c17db5185858d9fed30b6cd78a7daff4e07c88bf280cfc595620a4107613b50cab42a32f9b"),
                    "",
                    System.currentTimeMillis()
            );
            MultiWallet multiWallet = new MultiWallet(params, new ZerocoinContext(new JniBridge()), seed);
            loadWallet(multiWallet, Coin.valueOf(2,0));
            SendRequest req = multiWallet.createMintRequest(Coin.valueOf(2, 0));


            Transaction tx = req.tx;
            tx.getConfidence().markBroadcastBy(new PeerAddress(PARAMS, InetAddress.getByAddress(new byte[]{1,2,3,4})));
            tx.getConfidence().markBroadcastBy(new PeerAddress(PARAMS, InetAddress.getByAddress(new byte[]{10,2,3,4})));
            multiWallet.commitTx(tx);

            // Confirmed tx
            sendMoneyToWallet(multiWallet.getPivWallet(),AbstractBlockChain.NewBlockType.BEST_CHAIN, tx);
            sendMoneyToWallet(multiWallet.getZpivWallet(),AbstractBlockChain.NewBlockType.BEST_CHAIN,tx);

            sendMoneyToWallet(multiWallet.getPivWallet(),AbstractBlockChain.NewBlockType.BEST_CHAIN);
            sendMoneyToWallet(multiWallet.getZpivWallet(),AbstractBlockChain.NewBlockType.BEST_CHAIN);
            sendMoneyToWallet(multiWallet.getPivWallet(),AbstractBlockChain.NewBlockType.BEST_CHAIN);
            sendMoneyToWallet(multiWallet.getZpivWallet(),AbstractBlockChain.NewBlockType.BEST_CHAIN);
            // More blocks..
            sendMoneyToWallet(multiWallet.getPivWallet(),AbstractBlockChain.NewBlockType.BEST_CHAIN);
            sendMoneyToWallet(multiWallet.getZpivWallet(),AbstractBlockChain.NewBlockType.BEST_CHAIN);
            sendMoneyToWallet(multiWallet.getPivWallet(),AbstractBlockChain.NewBlockType.BEST_CHAIN);
            sendMoneyToWallet(multiWallet.getZpivWallet(),AbstractBlockChain.NewBlockType.BEST_CHAIN);

            Transaction tx2 = multiWallet.getTransaction(tx.getHash());

            Assert.assertEquals("Invalid depth in blocks", 5, tx2.getConfidence().getDepthInBlocks());


        } catch (InsufficientMoneyException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Fail for: " + e.getMessage());
        }
    }

    private void loadWallet(MultiWallet multiWallet, Coin coin) {
        this.wallet = multiWallet.getPivWallet();

        sendMoneyToWallet(AbstractBlockChain.NewBlockType.BEST_CHAIN, Coin.valueOf(2,0),wallet.freshReceiveAddress());
        sendMoneyToWallet(multiWallet.getZpivWallet(),AbstractBlockChain.NewBlockType.BEST_CHAIN);
        sendMoneyToWallet(AbstractBlockChain.NewBlockType.BEST_CHAIN, Coin.valueOf(1,0),wallet.freshReceiveAddress());
        sendMoneyToWallet(multiWallet.getZpivWallet(),AbstractBlockChain.NewBlockType.BEST_CHAIN);

        System.out.println("wallet balance: " + wallet.getBalance());
        System.out.println("wallet unspendable: " + wallet.getBalance(Wallet.BalanceType.ESTIMATED));
    }


    /**
     * TODO: Create a test that
     * 1) Creates the zpiv wallet
     * 2) Serialize it.
     * 3) Deserialize it and try to validate if a previous valid zCoin is part of the wallet.
     */
    @Test
    public void recreateWalletTest(){
        try {
            NetworkParameters params = UnitTestParams.get();
            setUp();
            DeterministicSeed seed = new DeterministicSeed(
                    Hex.decode("760a00eda285a842ad99626b61faebb6e36d80decae6665ac9c5f4c17db5185858d9fed30b6cd78a7daff4e07c88bf280cfc595620a4107613b50cab42a32f9b"),
                    "",
                    System.currentTimeMillis()
            );
            MultiWallet multiWallet = new MultiWallet(params, new ZerocoinContext(new JniBridge()), seed);

            ZCoin myCoin = multiWallet.getZPivWallet().getWallet().getActiveKeyChain().getZcoins(1).get(0);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            new WalletProtobufSerializer().walletToProto(multiWallet).build().writeTo(outputStream);
            byte[] walletBytes = outputStream.toByteArray();
            outputStream.close();

            ByteArrayInputStream inputStream = new ByteArrayInputStream(walletBytes);
            MultiWallet multiWallet2 = new WalletProtobufSerializer().readMultiWallet(inputStream, false, null);

            ZCoin myCoin2 = multiWallet2.getZPivWallet().getWallet().getActiveKeyChain().getZcoins(1).get(0);

            Assert.assertEquals("Coins are not equals", myCoin,myCoin2);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Test create mint, save the wallet and restore it.
     * The minted zcoin needs to have the values saved.
     */

    @Test
    public void mintSaveRestoreTest(){
        try {
            NetworkParameters params = UnitTestParams.get();
            setUp();
            DeterministicSeed seed = new DeterministicSeed(
                    Hex.decode("760a00eda285a842ad99626b61faebb6e36d80decae6665ac9c5f4c17db5185858d9fed30b6cd78a7daff4e07c88bf280cfc595620a4107613b50cab42a32f9b"),
                    "",
                    System.currentTimeMillis()
            );
            MultiWallet multiWallet = new MultiWallet(params, new ZerocoinContext(new JniBridge()), seed);

            ZCoin myFirstCoin = multiWallet.getZPivWallet().getWallet().getActiveKeyChain().getZcoins(1).get(0);

            // load balance
            loadWallet(multiWallet, Coin.valueOf(2, 0));
            // mint
            SendRequest req = multiWallet.createMintRequest(Coin.valueOf(1, 0));
            Transaction tx = req.tx;
            tx.getConfidence().markBroadcastBy(new PeerAddress(PARAMS, InetAddress.getByAddress(new byte[]{1, 2, 3, 4})));
            tx.getConfidence().markBroadcastBy(new PeerAddress(PARAMS, InetAddress.getByAddress(new byte[]{10, 2, 3, 4})));
            multiWallet.commitTx(tx);

            // obtain the minted coin
            TransactionOutput mintOutput = null;
            for (TransactionOutput output : tx.getOutputs()) {
                if (output.isZcMint()){
                    mintOutput = output;
                }
            }
            ZCoin minteCoin = multiWallet.getZcoinAssociated(mintOutput.getScriptPubKey().getCommitmentValue());

            // Now check that both coins commitments are equals
            Assert.assertEquals("Coins are not equals", minteCoin.getCommitment(), myFirstCoin.getCommitment());

            Assert.assertSame("Minted coin denomination is not correct", minteCoin.getCoinDenomination(), CoinDenomination.ZQ_ONE);

            // Confirmed tx
            sendMoneyToWallet(multiWallet.getPivWallet(), AbstractBlockChain.NewBlockType.BEST_CHAIN, tx);
            sendMoneyToWallet(multiWallet.getZpivWallet(), AbstractBlockChain.NewBlockType.BEST_CHAIN, tx);
            // More blocks..
            sendMoneyToWallet(multiWallet.getPivWallet(), AbstractBlockChain.NewBlockType.BEST_CHAIN);
            sendMoneyToWallet(multiWallet.getPivWallet(), AbstractBlockChain.NewBlockType.BEST_CHAIN);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            new WalletProtobufSerializer().walletToProto(multiWallet).build().writeTo(outputStream);
            byte[] walletBytes = outputStream.toByteArray();
            outputStream.close();

            ByteArrayInputStream inputStream = new ByteArrayInputStream(walletBytes);
            MultiWallet multiWallet2 = new WalletProtobufSerializer().readMultiWallet(inputStream, false, null);

            ZCoin myCoinRestaured = multiWallet2.getZPivWallet().getWallet().getActiveKeyChain().getZcoins(1).get(0);

            Assert.assertEquals("Restored coin is not equal", minteCoin.getCommitment(), myCoinRestaured.getCommitment());
            Assert.assertSame("Restored minted coin denomination is not correct", myCoinRestaured.getCoinDenomination(), CoinDenomination.ZQ_ONE);

        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void moveMintFromPendingPoolToUnspentPool(){

        try {
            NetworkParameters params = UnitTestParams.get();
            setUp();
            DeterministicSeed seed = new DeterministicSeed(
                    Hex.decode("760a00eda285a842ad99626b61faebb6e36d80decae6665ac9c5f4c17db5185858d9fed30b6cd78a7daff4e07c88bf280cfc595620a4107613b50cab42a32f9b"),
                    "",
                    System.currentTimeMillis()
            );
            MultiWallet multiWallet = new MultiWallet(params, new ZerocoinContext(new JniBridge()), seed);
            loadWallet(multiWallet, Coin.valueOf(2, 0));
            SendRequest req = multiWallet.createMintRequest(Coin.valueOf(1, 0));


            Transaction tx = req.tx;
            tx.getConfidence().markBroadcastBy(new PeerAddress(PARAMS, InetAddress.getByAddress(new byte[]{1, 2, 3, 4})));
            tx.getConfidence().markBroadcastBy(new PeerAddress(PARAMS, InetAddress.getByAddress(new byte[]{10, 2, 3, 4})));
            multiWallet.commitTx(tx);

            Assert.assertTrue("Mint is not in pending pool",
                    multiWallet.getZpivWallet().getTransactionPool(WalletTransaction.Pool.PENDING).containsKey(tx.getHash()));

            sendMoneyToWallet(multiWallet.getPivWallet(), AbstractBlockChain.NewBlockType.BEST_CHAIN);
            sendMoneyToWallet(multiWallet.getZpivWallet(), AbstractBlockChain.NewBlockType.BEST_CHAIN);
            sendMoneyToWallet(multiWallet.getPivWallet(), AbstractBlockChain.NewBlockType.BEST_CHAIN);
            sendMoneyToWallet(multiWallet.getZpivWallet(), AbstractBlockChain.NewBlockType.BEST_CHAIN);
            // Confirmed tx
            sendMoneyToWallet(multiWallet.getPivWallet(), AbstractBlockChain.NewBlockType.BEST_CHAIN, tx);
            sendMoneyToWallet(multiWallet.getZpivWallet(), AbstractBlockChain.NewBlockType.BEST_CHAIN, tx);
            // More blocks..
            sendMoneyToWallet(multiWallet.getZpivWallet(), AbstractBlockChain.NewBlockType.BEST_CHAIN);
            sendMoneyToWallet(multiWallet.getZpivWallet(), AbstractBlockChain.NewBlockType.BEST_CHAIN);
            sendMoneyToWallet(multiWallet.getZpivWallet(), AbstractBlockChain.NewBlockType.BEST_CHAIN);
            sendMoneyToWallet(multiWallet.getZpivWallet(), AbstractBlockChain.NewBlockType.BEST_CHAIN);

            Assert.assertTrue("Mint is not in unspendable pool",
                    multiWallet.getZpivWallet().getTransactionPool(WalletTransaction.Pool.UNSPENT).containsKey(tx.getHash()));

            Assert.assertEquals("Invalid value sent to me", tx.getValueSentToMe(multiWallet.getZpivWallet()), Coin.COIN);
            Assert.assertEquals("Invalid value sent from me", tx.getValueSentFromMe(multiWallet.getPivWallet()), Coin.valueOf(2,0));

        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }


    }

    /**
     * Test to validate that the zPIV wallet understand mint txes and add them to the wallet balance
     */
    @Test
    public void addZpivToWalletBalanceTest() {
        try {
            NetworkParameters params = UnitTestParams.get();
            setUp();
            DeterministicSeed seed = new DeterministicSeed(
                    Hex.decode("760a00eda285a842ad99626b61faebb6e36d80decae6665ac9c5f4c17db5185858d9fed30b6cd78a7daff4e07c88bf280cfc595620a4107613b50cab42a32f9b"),
                    "",
                    System.currentTimeMillis()
            );
            MultiWallet multiWallet = new MultiWallet(params, new ZerocoinContext(new JniBridge()), seed);
            loadWallet(multiWallet, Coin.valueOf(2, 0));
            SendRequest req = multiWallet.createMintRequest(Coin.valueOf(1, 0));


            Transaction tx = req.tx;
            tx.getConfidence().markBroadcastBy(new PeerAddress(PARAMS, InetAddress.getByAddress(new byte[]{1, 2, 3, 4})));
            tx.getConfidence().markBroadcastBy(new PeerAddress(PARAMS, InetAddress.getByAddress(new byte[]{10, 2, 3, 4})));
            multiWallet.commitTx(tx);

            sendMoneyToWallet(multiWallet.getPivWallet(), AbstractBlockChain.NewBlockType.BEST_CHAIN);
            sendMoneyToWallet(multiWallet.getZpivWallet(), AbstractBlockChain.NewBlockType.BEST_CHAIN);
            sendMoneyToWallet(multiWallet.getPivWallet(), AbstractBlockChain.NewBlockType.BEST_CHAIN);
            sendMoneyToWallet(multiWallet.getZpivWallet(), AbstractBlockChain.NewBlockType.BEST_CHAIN);
            // Confirmed tx
            sendMoneyToWallet(multiWallet.getPivWallet(), AbstractBlockChain.NewBlockType.BEST_CHAIN, tx);
            sendMoneyToWallet(multiWallet.getZpivWallet(), AbstractBlockChain.NewBlockType.BEST_CHAIN, tx);
            // More blocks..
            sendMoneyToWallet(multiWallet.getPivWallet(), AbstractBlockChain.NewBlockType.BEST_CHAIN);
            sendMoneyToWallet(multiWallet.getPivWallet(), AbstractBlockChain.NewBlockType.BEST_CHAIN);
            sendMoneyToWallet(multiWallet.getZpivWallet(), AbstractBlockChain.NewBlockType.BEST_CHAIN);
            sendMoneyToWallet(multiWallet.getZpivWallet(), AbstractBlockChain.NewBlockType.BEST_CHAIN);

            Assert.assertTrue("Zpiv balance has not been increased" , multiWallet.getZpivWallet().getBalance().isGreaterThan(Coin.ZERO));


            // Now spend it and check if the wallet notice that

            //SendRequest sendRequest = multiWallet.createSpendRequest(multiWallet.getCurrentReceiveAddress(),Coin.COIN);
            //multiWallet.spendZpiv(sendRequest)


        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

}
