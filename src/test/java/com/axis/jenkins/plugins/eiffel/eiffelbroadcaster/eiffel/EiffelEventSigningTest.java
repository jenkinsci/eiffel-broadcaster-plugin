/**
 The MIT License

 Copyright 2023 Axis Communications AB.

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
 */

package com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import org.erdtman.jcs.JsonCanonicalizer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@RunWith(Parameterized.class)
public class EiffelEventSigningTest {
    @Parameterized.Parameters(name = "{index}: eiffelAlg={0}, signingAlg={1}, hashAlg={2}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { EiffelEvent.Meta.Security.IntegrityProtection.Alg.ES256, "EC", HashAlgorithm.SHA_256,
                        new ECGenParameterSpec("secp256r1") },
                { EiffelEvent.Meta.Security.IntegrityProtection.Alg.ES384, "EC", HashAlgorithm.SHA_384,
                        new ECGenParameterSpec("secp384r1") },
                { EiffelEvent.Meta.Security.IntegrityProtection.Alg.ES512, "EC", HashAlgorithm.SHA_512,
                        new ECGenParameterSpec("secp521r1") },
                { EiffelEvent.Meta.Security.IntegrityProtection.Alg.RS256, "RSA", HashAlgorithm.SHA_256,
                        null },
                { EiffelEvent.Meta.Security.IntegrityProtection.Alg.RS384, "RSA", HashAlgorithm.SHA_384,
                        null },
                { EiffelEvent.Meta.Security.IntegrityProtection.Alg.RS512, "RSA", HashAlgorithm.SHA_512,
                        null },
        });
    }

    @Parameterized.Parameter(0)
    public EiffelEvent.Meta.Security.IntegrityProtection.Alg eiffelAlg;

    @Parameterized.Parameter(1)
    public String signingAlg;

    @Parameterized.Parameter(2)
    public HashAlgorithm hashAlg;

    @Parameterized.Parameter(3)
    public AlgorithmParameterSpec algParamSpec;

    @Test
    public void testSigning() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(signingAlg);
        if (algParamSpec != null) {
            keyGen.initialize(algParamSpec, new SecureRandom());
        }

        KeyPair pair = keyGen.generateKeyPair();
        PrivateKey priv = pair.getPrivate();
        PublicKey pub = pair.getPublic();

        EiffelActivityTriggeredEvent event = new EiffelActivityTriggeredEvent("activity name");
        event.sign(priv, "CN=test", hashAlg);

        assertThat(event.getMeta().getSecurity(), is(notNullValue()));
        assertThat(event.getMeta().getSecurity().getAuthorIdentity(), is("CN=test"));
        assertThat(event.getMeta().getSecurity().getIntegrityProtection(), is(notNullValue()));
        EiffelEvent.Meta.Security.IntegrityProtection.Alg actualEiffelAlg =
                event.getMeta().getSecurity().getIntegrityProtection().getAlg();
        assertThat(actualEiffelAlg, is(eiffelAlg));
        Signature sig = Signature.getInstance(actualEiffelAlg.getSignatureAlgorithm());
        sig.initVerify(pub);

        ObjectMapper mapper = new ObjectMapper();
        byte[] originalSignature = Base64.getDecoder().decode(
                event.getMeta().getSecurity().getIntegrityProtection().getSignature());
        event.getMeta().getSecurity().getIntegrityProtection().setSignature("");
        sig.update(new JsonCanonicalizer(mapper.writeValueAsString(mapper.valueToTree(event))).getEncodedUTF8());
        assertThat(sig.verify(originalSignature), is(true));
    }
}
