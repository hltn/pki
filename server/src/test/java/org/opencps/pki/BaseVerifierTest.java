/**
* OpenCPS PKI is the open source PKI Integration software
* Copyright (C) 2016-present OpenCPS community

* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Affero General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* any later version.

* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Affero General Public License for more details.
* You should have received a copy of the GNU Affero General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/>
*/
package org.opencps.pki;

import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author Nguyen Van Nguyen <nguyennv@iwayvietnam.com>
 */
public class BaseVerifierTest extends TestCase {
    private static final String certPath = "./src/test/java/resources/cert.pem";
    private static final String ghCertPath = "./src/test/java/resources/github.pem";
    private static final String rootCaPath = "./src/test/java/resources/rootca.pem";

    private static final String keyStorePath = "./src/test/java/resources/opencsp.jks";
    private static final String keyStorePass = "opencps";

    X509Certificate cert;
    CertificateFactory cf;
    BaseVerifier verifier;

    /**
     * Create the test case
     */
    public BaseVerifierTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(BaseVerifierTest.class);
    }

    protected void setUp() throws CertificateException, FileNotFoundException {
        cf = CertificateFactory.getInstance("X.509");
        cert = (X509Certificate) cf.generateCertificate(new FileInputStream(new File(certPath)));
        verifier = mock(BaseVerifier.class, CALLS_REAL_METHODS);
    }

    public void testReadCertificate() throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(certPath));
        String string = new String(encoded, StandardCharsets.UTF_8);

        CertificateInfo byteInfo = verifier.readCertificate(encoded);
        assertEquals("OpenCPS PKI", byteInfo.getCommonName());

        CertificateInfo stringInfo = verifier.readCertificate(string);
        assertEquals("OpenCPS PKI", stringInfo.getCommonName());
    }
    
    public void testValidateCertificate() throws CertificateException, KeyStoreException, NoSuchAlgorithmException, IOException {
        assertFalse(verifier.validateCertificate(cert));

        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null, null);

        ks.setCertificateEntry("digicert", cf.generateCertificate(new FileInputStream(rootCaPath)));

        X509Certificate ghCert = (X509Certificate) cf.generateCertificate(new FileInputStream(new File(ghCertPath)));
        assertTrue(verifier.validateCertificate(ghCert, ks));
    }

    public void testKeyStore() throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        verifier.setKeyStore(ks);
        assertEquals(ks, verifier.getKeyStore());

        ks.load(null, null);
        Certificate cert = cf.generateCertificate(new FileInputStream(rootCaPath));
        ks.setCertificateEntry("digicert", cert);

        File file = new File(keyStorePath);
        if (!file.exists()) {
            OutputStream os = new FileOutputStream(keyStorePath);
            ks.store(os, keyStorePass.toCharArray());
            os.close();
        }
        verifier.loadKeyStore(keyStorePath, keyStorePass);
        assertEquals(cert, verifier.getKeyStore().getCertificate("digicert"));;
    }

}
