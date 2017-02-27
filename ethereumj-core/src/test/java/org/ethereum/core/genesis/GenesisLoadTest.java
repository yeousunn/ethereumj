package org.ethereum.core.genesis;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import org.ethereum.config.BlockchainNetConfig;
import org.ethereum.config.SystemProperties;
import org.ethereum.config.blockchain.DaoHFConfig;
import org.ethereum.config.blockchain.Eip150HFConfig;
import org.ethereum.config.blockchain.FrontierConfig;
import org.ethereum.config.blockchain.HomesteadConfig;
import org.ethereum.core.Genesis;
import org.ethereum.util.blockchain.StandaloneBlockchain;

import static org.ethereum.util.FastByteComparisons.equal;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import java.io.File;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;



/**
 * Testing system exit
 * http://stackoverflow.com/questions/309396/java-how-to-test-methods-that-call-system-exit
 *
 * Created by Stan Reshetnyk on 17.09.16.
 */
public class GenesisLoadTest {

    @Test
    public void shouldLoadGenesis_whenShortWay() {
        loadGenesis(null, "frontier-test.json");
        assertTrue(true);
    }

    @Test
    public void shouldLoadGenesis_whenFullPathSpecified() throws URISyntaxException {
        URL url = GenesisLoadTest.class.getClassLoader().getResource("genesis/frontier-test.json");

        // full path
        System.out.println("url.getPath() " + url.getPath());
        loadGenesis(url.getPath(), null);

        Path path = new File(url.toURI()).toPath();
        Path curPath = new File("").getAbsoluteFile().toPath();
        String relPath = curPath.relativize(path).toFile().getPath();
        System.out.println("Relative path: " + relPath);
        loadGenesis(relPath, null);
        assertTrue(true);
    }

    @Test
    public void shouldLoadGenesisFromFile_whenBothSpecified() {
        URL url = GenesisLoadTest.class.getClassLoader().getResource("genesis/frontier-test.json");

        // full path
        System.out.println("url.getPath() " + url.getPath());
        loadGenesis(url.getPath(), "NOT_EXIST");
        assertTrue(true);
    }

    @Test(expected = RuntimeException.class)
    public void shouldError_whenWrongPath() {
        loadGenesis("NON_EXISTED_PATH", null);
        assertTrue(false);
    }

    @Test
    public void shouldLoadGenesis_withBlockchainConfig() {
        SystemProperties properties = loadGenesis(null, "genesis-with-config.json");
        properties.getGenesis();
        BlockchainNetConfig bnc = properties.getBlockchainConfig();

        assertThat(bnc.getConfigForBlock(0), instanceOf(FrontierConfig.class));
        assertThat(bnc.getConfigForBlock(149), instanceOf(FrontierConfig.class));

        assertThat(bnc.getConfigForBlock(150), instanceOf(DaoHFConfig.class));
        assertThat(bnc.getConfigForBlock(299), instanceOf(DaoHFConfig.class));
        DaoHFConfig daoHFConfig = (DaoHFConfig) bnc.getConfigForBlock(200);

        assertThat(bnc.getConfigForBlock(300), instanceOf(HomesteadConfig.class));
        assertThat(bnc.getConfigForBlock(449), instanceOf(HomesteadConfig.class));

        assertThat(bnc.getConfigForBlock(450), instanceOf(Eip150HFConfig.class));
        assertThat(bnc.getConfigForBlock(10_000_000), instanceOf(Eip150HFConfig.class));
    }

    @Test
    public void shouldLoadGenesis_withCodeAndNonceInAlloc() {
        final Genesis genesis = GenesisLoader.loadGenesis(
                getClass().getResourceAsStream("/genesis/genesis-alloc.json"));
        final StandaloneBlockchain bc = new StandaloneBlockchain();

        bc.withGenesis(genesis);

        final byte[] account = Hex.decode("cd2a3d9f938e13cd947ec05abc7fe734df8dd826");
        byte[] expectedCode = Hex.decode("00ff00");
        long expectedNonce = 255;       //FF

        final BigInteger actualNonce = bc.getBlockchain().getRepository().getNonce(account);
        final byte[] actualCode = bc.getBlockchain().getRepository().getCode(account);

//        System.out.println("nonce: " + actualNonce);
//        System.out.println("code: " + Hex.toHexString(actualCode));

        assertEquals(BigInteger.valueOf(expectedNonce), actualNonce);
        assertTrue(equal(expectedCode, actualCode));
    }

    private SystemProperties loadGenesis(String genesisFile, String genesisResource) {
        Config config = ConfigFactory.empty();

        if (genesisResource != null) {
            config = config.withValue("genesis",
                    ConfigValueFactory.fromAnyRef(genesisResource));
        }
        if (genesisFile != null) {
            config = config.withValue("genesisFile",
                    ConfigValueFactory.fromAnyRef(genesisFile));
        }

        SystemProperties properties = new SystemProperties(config);
        properties.getGenesis();
        return properties;
    }
}
