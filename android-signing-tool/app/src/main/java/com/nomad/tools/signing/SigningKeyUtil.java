package com.nomad.tools.signing;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class SigningKeyUtil {
    private SigningKeyUtil() { }
    public static final class Bundle {
        public final byte[] p12;
        public final String fingerprint;
        public final String alias;
        public final Date expires;
        Bundle(byte[] p12, String fingerprint, String alias, Date expires) {
            this.p12=p12; this.fingerprint=fingerprint; this.alias=alias; this.expires=expires;
        }
    }
    public static Bundle generate(char[] password) throws Exception {
        if (password == null || password.length < 12)
            throw new IllegalArgumentException("A minimum of 12 password characters is required.");
        KeyPairGenerator generator=KeyPairGenerator.getInstance("RSA");
        generator.initialize(4096, new SecureRandom());
        KeyPair pair=generator.generateKeyPair();
        if (pair.getPrivate().getEncoded() == null)
            throw new IllegalStateException("The generated private key cannot be exported.");
        Date start=new Date(System.currentTimeMillis()-24L*60L*60L*1000L);
        Calendar c=Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.setTime(start);
        c.add(Calendar.YEAR, 25);
        Date expiry=c.getTime();
        byte[] name=seq(set(seq(oid("2.5.4.3"),der(0x0c,"Nomad POS Official Release".getBytes(StandardCharsets.UTF_8)))));
        byte[] alg=seq(oid("1.2.840.113549.1.1.11"),der(0x05,new byte[0]));
        byte[] serialBytes=new byte[18];
        new SecureRandom().nextBytes(serialBytes);
        BigInteger serial=new BigInteger(1,serialBytes);
        byte[] tbs=seq(der(0xa0,der(0x02,new byte[]{2})),
           der(0x02,serial.toByteArray()),alg,name,seq(asnTime(start),asnTime(expiry)),
           name,pair.getPublic().getEncoded());
        Signature signer=Signature.getInstance("SHA256withRSA");
        signer.initSign(pair.getPrivate());
        signer.update(tbs);
        byte[] certDer=seq(tbs,alg,bitString(signer.sign()));
        X509Certificate cert=(X509Certificate)CertificateFactory.getInstance("X.509")
           .generateCertificate(new ByteArrayInputStream(certDer));
        cert.checkValidity();
        cert.verify(pair.getPublic());
        KeyStore store=KeyStore.getInstance("PKCS12");
        store.load(null,password);
        store.setKeyEntry("nomadpos",pair.getPrivate(),password,new Certificate[]{cert});
        ByteArrayOutputStream output=new ByteArrayOutputStream();
        store.store(output,password);
        byte[] bytes=output.toByteArray();
        KeyStore probe=KeyStore.getInstance("PKCS12");
        probe.load(new ByteArrayInputStream(bytes),password);
        if (!probe.containsAlias("nomadpos") || probe.getKey("nomadpos",password)==null)
            throw new IllegalStateException("PKCS#12 verification failed.");
        return new Bundle(bytes,sha256(cert.getEncoded()),"nomadpos",expiry);
    }
    public static String readFingerprint(byte[] pkcs12,char[] password) throws Exception {
        KeyStore store=KeyStore.getInstance("PKCS12");
        store.load(new ByteArrayInputStream(pkcs12),password);
        java.util.Enumeration<String> aliases=store.aliases();
        while(aliases.hasMoreElements()) {
            String alias=aliases.nextElement();
            Certificate cert=store.getCertificate(alias);
            if(cert!=null && store.isKeyEntry(alias))return sha256(cert.getEncoded());
        }
        throw new IllegalStateException("No signing key certificate was found.");
    }
    public static String sha256(byte[] bytes) throws Exception {
        byte[] digest=MessageDigest.getInstance("SHA-256").digest(bytes);
        StringBuilder result=new StringBuilder();
        for(int i=0;i<digest.length;i++){
            if(i>0)result.append(':');
            result.append(String.format(Locale.US,"%02X",digest[i]&0xff));
        }
        return result.toString();
    }
    private static byte[] asnTime(Date value) {
        Calendar c=Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.setTime(value);
        boolean generalized=c.get(Calendar.YEAR)>=2050;
        SimpleDateFormat f=new SimpleDateFormat(generalized?"yyyyMMddHHmmss'Z'":"yyMMddHHmmss'Z'",Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return der(generalized?0x18:0x17,f.format(value).getBytes(StandardCharsets.US_ASCII));
    }
    private static byte[] bitString(byte[] b){return der(0x03,join(new byte[]{0},b));}
    private static byte[] seq(byte[]... parts){return der(0x30,join(parts));}
    private static byte[] set(byte[]... parts){return der(0x31,join(parts));}
    private static byte[] oid(String dot) {
        String[] v=dot.split("\\.");
        int first=Integer.parseInt(v[0])*40+Integer.parseInt(v[1]);
        ByteArrayOutputStream b=new ByteArrayOutputStream();
        encodeBase128(b,first);
        for(int i=2;i<v.length;i++)encodeBase128(b,Long.parseLong(v[i]));
        return der(0x06,b.toByteArray());
    }
    private static void encodeBase128(ByteArrayOutputStream b,long x) {
        byte[] tmp=new byte[10];int n=0;
        tmp[n++]=(byte)(x&127);x>>>=7;
        while(x!=0){tmp[n++]=(byte)((x&127)|128);x>>>=7;}
        for(int i=n-1;i>=0;i--)b.write(tmp[i]);
    }
    private static byte[] der(int tag,byte[] content) {
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        out.write(tag);int len=content.length;
        if(len<128)out.write(len);
        else if(len<256){out.write(0x81);out.write(len);}
        else if(len<65536){out.write(0x82);out.write(len>>8);out.write(len);}
        else{out.write(0x83);out.write(len>>16);out.write(len>>8);out.write(len);}
        out.write(content,0,len);return out.toByteArray();
    }
    private static byte[] join(byte[]... parts){
        int n=0;for(byte[] x:parts)n+=x.length;
        byte[] all=new byte[n];int p=0;
        for(byte[] x:parts){System.arraycopy(x,0,all,p,x.length);p+=x.length;}
        return all;
    }
}
