package com.aliyz.fabric.sdk;

import com.alibaba.fastjson.JSON;
import com.aliyz.fabric.sdk.exception.HFSDKException;
import com.aliyz.fabric.sdk.model.SampleOrg;
import com.aliyz.fabric.sdk.utils.HFSDKUtils;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric.sdk.security.CryptoSuiteFactory;
import org.hyperledger.fabric.shim.ChaincodeBase;
import org.hyperledger.fabric_ca.sdk.Attribute;
import org.hyperledger.fabric_ca.sdk.HFCAAffiliation;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.HFCAIdentity;
import org.junit.Test;

import java.io.File;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * All rights Reserved, Designed By www.aliyz.com
 *
 * <p></p>
 * Created by aliyz at 2020-08-04 10:11
 * Copyright: 2020 www.aliyz.com Inc. All rights reserved.
 */
public class ISdkTest {

    private static final String ORG_1 = "Org1";
    private static final String ORG_2 = "Org2";
    private static final String ORG_1_MSP = "Org1MSP";
    private static final String ORG_2_MSP = "Org2MSP";
    private static final String PEER_0_ORG_1 = "peer0.org1.example.com";
    private static final String PEER_1_ORG_1 = "peer1.org1.example.com";
    private static final String PEER_0_ORG_2 = "peer0.org2.example.com";
    private static final String ORDERER = "orderer.example.com";



    private static final String CHAIN_CODE_SOURCE_PATH = "/Users/aliyz/Workspace/java/code.tusdao.com/fabric/fabric-sdk-java/src/test/fixture/sdkintegration/gocc/sample1";
    private static final String CHAIN_CODE_METAINFO_PATH = "/Users/aliyz/Workspace/java/code.tusdao.com/fabric/fabric-sdk-java/src/test/fixture/meta-infs/end2endit";
    private static final String CHANNEL_TX_SOURCE_PATH = "/Users/aliyz/Workspace/deploy/fabric/channel-artifacts";
    private static final String CHAIN_CODE_PATH = "github.com";
    private static final String CHAIN_CODE_NAME = "example_cc";
    private static final String CHAIN_CODE_VERSION = "1.0";
    private static final String MY_CHANNEL_NAME = "mychannel";
    private static NetworkConfig networkConfig;
    private static HFClient org1Client;

    static {
        try {
//            networkConfig = NetworkConfig.fromYamlFile(new File("src/main/resources/network-config/fabric-aliyz.local-network_config.yaml"));
//
//            org1Client = getTheClient(ORG_1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void constructChannelTest () {
        System.out.println("---------------^^ 创建通道 ^^--------------");

        String channelName = MY_CHANNEL_NAME;
        SampleOrg org1 = null;
        Collection<Peer> orgPeers = new ArrayList<>();
        Collection<Orderer> orderers = new ArrayList<>();
        boolean createFabricChannel = true;
        String channelTxPath = CHANNEL_TX_SOURCE_PATH;

        try {
            Peer peer = org1Client.newPeer(PEER_0_ORG_1, networkConfig.getPeerUrl(PEER_0_ORG_1), networkConfig.getPeerProperties(PEER_0_ORG_1));
            orgPeers.add(peer);

            Orderer orderer = org1Client.newOrderer(ORDERER, "grpcs://localhost:7050", networkConfig.getOrdererProperties(ORDERER));
            orderers.add(orderer);

            Channel channel = CHSDK.constructChannel(org1Client, MY_CHANNEL_NAME, org1, orgPeers, orderers, createFabricChannel, channelTxPath);

            System.out.println("/////////////////channel/////////////////" + channel.getName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("##---------------^^ 结束 ^^--------------##");

    }

    // 部署链码测试
    @Test
    public void deployChaincodeTest() {
        System.out.println("---------------^^ 部署链码 ^^--------------");

        String[] org1PeerNames = new String[]{PEER_0_ORG_1};
        String[] org2PeerNames = new String[]{PEER_0_ORG_2};
        String chaincodeSourcePath = CHAIN_CODE_SOURCE_PATH;
        String chaincodeMetainfoPath = CHAIN_CODE_METAINFO_PATH;
        String chaincodePath = CHAIN_CODE_PATH;
        String chaincodeName = CHAIN_CODE_NAME;
        String chaincodeVersion = CHAIN_CODE_VERSION;
        TransactionRequest.Type chaincodeType = TransactionRequest.Type.GO_LANG;
        long sequence = 1;
        boolean initRequired = true;

        try {

            Channel channel = constructChannel(org1Client, MY_CHANNEL_NAME);
            System.out.println("deployChaincode - channelName = " + channel.getName());

            String chaincodeLabel = HFSDKUtils.genSampleChaincodeLabel(chaincodeName, chaincodeVersion);

            LifecycleChaincodeEndorsementPolicy lccEndorsementPolicy = LifecycleChaincodeEndorsementPolicy.fromSignaturePolicyYamlFile(
                    Paths.get("/Users/aliyz/Workspace/java/code.aliyz.com/aliyz-case/plugin/fabric-sdk/src/main/resources/policy-config/org1.chaincode-endorsement_policy.yaml"));

            // TODO
            ChaincodeCollectionConfiguration ccCollectionConfiguration = ChaincodeCollectionConfiguration.fromYamlFile(
                    new File("/Users/aliyz/Workspace/java/code.aliyz.com/aliyz-case/plugin/fabric-sdk/src/main/resources/coll-config/org1.collection-config.yaml"));

            Collection<Peer> org1Peers = HFSDKUtils.extractPeersFromChannel(channel, org1PeerNames);
//            Collection<Peer> org2Peers = HFSDKUtils.extractPeersFromChannel(channel, org2PeerNames);

            //////////////////////////////////////
            // 一、打包链码
            LifecycleChaincodePackage lifecycleChaincodePackage = CCSDK.packageChaincode(chaincodeLabel, chaincodeSourcePath,
                    chaincodeMetainfoPath, chaincodePath, chaincodeName, chaincodeType);
            System.out.println("////////////////// cc package ///////////////" + lifecycleChaincodePackage.getLabel());

            //////////////////////////////////////
            // 二、安装链码
            String packageId = CCSDK.installChaincode(org1Client, channel, org1Peers, lifecycleChaincodePackage);
            System.out.println("////////////////// cc packageId ///////////////" + packageId);

            boolean checkInstall = CCSDK.queryInstalled(org1Client, org1Peers, packageId, chaincodeLabel);
            System.out.println("////////////////// check installed cc ///////////////" + checkInstall);

            //////////////////////////////////////
            // 三、审核链码
            BlockEvent.TransactionEvent approveEvents = CCSDK.approveForMyOrg(org1Client, channel, org1Peers, sequence, chaincodeName,
                    chaincodeVersion, lccEndorsementPolicy, ccCollectionConfiguration, initRequired, packageId)
                    .get(60, TimeUnit.SECONDS);
            System.out.println("////////////////// approve cc result ///////////////" + approveEvents.isValid());

            boolean checkResult = CCSDK.checkCommitReadiness(org1Client, channel, sequence, chaincodeName, chaincodeLabel, lccEndorsementPolicy,
                    ccCollectionConfiguration, initRequired, org1Peers, new HashSet<>(Arrays.asList(ORG_1_MSP)),
                    Collections.emptySet());
            System.out.println("////////////////// check approve result ///////////////" + checkResult);

            //////////////////////////////////////
            // 四、提交链码
            BlockEvent.TransactionEvent commitEvent = CCSDK.commitChaincodeDefinition(org1Client, channel, sequence, chaincodeName,
                    chaincodeVersion, lccEndorsementPolicy, ccCollectionConfiguration, initRequired, org1Peers)
                    .get(60, TimeUnit.SECONDS);
            System.out.println("////////////////// commit cc result ///////////////" + commitEvent.isValid());
            checkResult = CCSDK.queryCommitted(org1Client, channel, chaincodeName, org1Peers, sequence, initRequired, null,
                    ccCollectionConfiguration);
            System.out.println("////////////////// check commit result ///////////////" + checkResult);

            //////////////////////////////////////
            // 五、初始化链码
            BlockEvent.TransactionEvent initEvent = CCSDK.initChaincode(org1Client, org1Client.getUserContext(), channel, initRequired, chaincodeName,
                    chaincodeVersion, chaincodeType, new String[]{"a,", "100", "b", "300"})
                    .get(60, TimeUnit.SECONDS);
            System.out.println("////////////////// init cc result ///////////////" + initEvent.isValid());

            System.out.println("##---------------^^ 结束 ^^--------------##");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 链码查询测试
    @Test
    public void queryChaincodeTest () {
        try {

            System.out.println("---------------^^ 查询链码 ^^--------------");

            Channel channel = constructChannel(org1Client, MY_CHANNEL_NAME);

            String payload = CCSDK.queryChaincode(org1Client, org1Client.getUserContext(), channel, "move", CHAIN_CODE_NAME, CHAIN_CODE_VERSION,
                    new String[]{"a", "b", "10"});

            System.out.println("-------------- payload -------------" + JSON.toJSONString(payload));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void toolTest () {
        String path = Paths.get(CHANNEL_TX_SOURCE_PATH, MY_CHANNEL_NAME.concat(".tx")).toString();
        System.out.println(path);
    }


    @Test
    // 用户注册
    public void registerTest () {
        System.out.println("---------------^^ 用户登记 ^^--------------");

        try {

            Properties props = new Properties();
            props.put("pemFile",
                    "/Users/aliyz/Workspace/deploy/fabric/organizations/fabric-ca/org/tls-cert.pem");
            props.put("allowAllHostNames", "true");

            HFCAClient client = HFCAClient.createNewInstance("https://localhost:7054", props);
            CryptoSuite cryptoSuite = CryptoSuiteFactory.getDefault().getCryptoSuite();
            client.setCryptoSuite(cryptoSuite);

            Collection<Attribute> attrs = Arrays.asList(new Attribute("hf.AffiliationMgr", "true"));
            String secret = CASDK.register(client, "org.department2", "admin", "adminpw", "aliyz10", "aliyz10pw", attrs);
            System.out.println("-------------- enrollSecret -------------" + secret);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    // 用户登记、获取证书
    public void enrollTest () {
        System.out.println("---------------^^ 用户注册 ^^--------------");

        try {
            Properties props = new Properties();
            props.put("pemFile",
                    "/Users/aliyz/Workspace/deploy/fabric/organizations/fabric-ca/org/tls-cert.pem");
            props.put("allowAllHostNames", "true");

            HFCAClient client = HFCAClient.createNewInstance("https://localhost:7054", props);
            CryptoSuite cryptoSuite = CryptoSuiteFactory.getDefault().getCryptoSuite();
            client.setCryptoSuite(cryptoSuite);

            Enrollment enrollment = CASDK.enroll(client, "aliyz12", "aliyz12pw", null);
            System.out.println("-------------- enrollment -------------" + enrollment);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getAffiliationsTest () {
        System.out.println("---------------^^ 所属机构列表 ^^--------------");

        try {
            Properties props = new Properties();
            props.put("pemFile",
                    "/Users/aliyz/Workspace/deploy/fabric/organizations/fabric-ca/org/tls-cert.pem");
            props.put("allowAllHostNames", "true");

            HFCAClient client = HFCAClient.createNewInstance("https://localhost:7054", props);
            CryptoSuite cryptoSuite = CryptoSuiteFactory.getDefault().getCryptoSuite();
            client.setCryptoSuite(cryptoSuite);

            User registrar = new User() {
                @Override
                public String getName() {
                    return null;
                }

                @Override
                public Set<String> getRoles() {
                    return null;
                }

                @Override
                public String getAccount() {
                    return null;
                }

                @Override
                public String getAffiliation() {
                    return null;
                }

                @Override
                public Enrollment getEnrollment() {
                    return CASDK.enroll(client, "admin", "adminpw", null);
                }

                @Override
                public String getMspId() {
                    return null;
                }
            };

            HFCAAffiliation affiliation = client.getHFCAAffiliations(registrar);
            Collection<HFCAIdentity> identities = client.getHFCAIdentities(registrar);

            System.out.println("-------------- affiliation -------------" + JSON.toJSONString(affiliation.getChildren()));
            System.out.println("-------------- identities -------------" + JSON.toJSONString(identities));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    // 创建身份
    public void createIdentityTest () {
        System.out.println("---------------^^ 创建身份 ^^--------------");

        try {
            Properties props = new Properties();
            props.put("pemFile",
                    "/Users/aliyz/Workspace/deploy/fabric/organizations/fabric-ca/org/tls-cert.pem");
            props.put("allowAllHostNames", "true");

            HFCAClient client = HFCAClient.createNewInstance("https://localhost:7054", props);
            CryptoSuite cryptoSuite = CryptoSuiteFactory.getDefault().getCryptoSuite();
            client.setCryptoSuite(cryptoSuite);

            boolean result = CASDK.createIdentity(client, "admin", "adminpw", "org.department2", "user1", "user1pw", null);
            System.out.println("-------------- result -------------" + result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    // 创建联盟
    public void createAffiliationTest () {
        System.out.println("---------------^^ 创建联盟 ^^--------------");

        try {
            Properties props = new Properties();
            props.put("pemFile",
                    "/Users/aliyz/Workspace/deploy/fabric/organizations/fabric-ca/org/tls-cert.pem");
            props.put("allowAllHostNames", "true");

            HFCAClient client = HFCAClient.createNewInstance("https://localhost:7054", props);
            CryptoSuite cryptoSuite = CryptoSuiteFactory.getDefault().getCryptoSuite();
            client.setCryptoSuite(cryptoSuite);

            boolean result = CASDK.createAffiliation(client, "admin", "adminpw", "org.department3");
            System.out.println("-------------- result -------------" + result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /** ------------------------------------------ private method ---------------------------------------- **/
    // Returns a new client instance
    private static HFClient getTheClient(String orgName) throws Exception {

        HFClient client = HFClient.createNewInstance();
        client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());

        client.setUserContext(networkConfig.getPeerAdmin(orgName));

        return client;
    }

    private Channel constructChannel(HFClient client, String channelName) throws Exception {

        //Channel newChannel = client.getChannel(channelName);
        Channel newChannel = client.loadChannelFromConfig(channelName, networkConfig);
        if (newChannel == null) {
            throw new HFSDKException("Channel " + channelName + " is not defined in the config file!");
        }

        return newChannel.initialize();
    }

    private static PrivateKey getPrivateKeyFromString(String data) {
        try {
            final Reader pemReader = new StringReader(data);

            final PrivateKeyInfo pemPair;
            try (PEMParser pemParser = new PEMParser(pemReader)) {
                pemPair = (PrivateKeyInfo) pemParser.readObject();
            }

            return new JcaPEMKeyConverter().getPrivateKey(pemPair);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    ChaincodeBase cb = null;

}
