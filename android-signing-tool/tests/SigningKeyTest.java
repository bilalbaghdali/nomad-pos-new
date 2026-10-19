import java.io.*;
import com.nomad.tools.signing.SigningKeyUtil;
public class SigningKeyTest {
 public static void main(String[] args) throws Exception {
   char[] pass="DemoStrongPass2026!".toCharArray();
   SigningKeyUtil.Bundle b=SigningKeyUtil.generate(pass);
   if(!SigningKeyUtil.readFingerprint(b.p12, pass).equals(b.fingerprint))throw new AssertionError("certificate mismatch");
   try {SigningKeyUtil.readFingerprint(b.p12,"wrong-pass".toCharArray());throw new AssertionError("wrong password accepted");}catch(IOException ok){}
   try(FileOutputStream out=new FileOutputStream(args[0])) {out.write(b.p12);}
   System.out.println("PASS. Fingerprint: "+b.fingerprint);
 }
}
