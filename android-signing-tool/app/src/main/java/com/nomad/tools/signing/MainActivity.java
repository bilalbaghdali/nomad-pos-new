package com.nomad.tools.signing;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class MainActivity extends Activity {
    private static final int SAVE_KEY = 1001, OPEN_KEY = 1002;
    private byte[] pendingP12;
    private String latestFingerprint;
    private TextView status, fingerprintText;
    private EditText password, confirmPassword;
    private Button generateButton, saveButton;
    private final int primary = Color.rgb(23,115,99), ink = Color.rgb(30,44,53);

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().setStatusBarColor(Color.rgb(17,78,69));
        getWindow().setNavigationBarColor(Color.WHITE);
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setPadding(dp(20),dp(22),dp(20),dp(25));
        root.setBackgroundColor(Color.rgb(248,250,250));
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true); scroll.addView(root);setContentView(scroll);
        addText(root,"Nomad POS — إدارة توقيع Android",24,true,primary);
        addText(root,"أداة مستقلة لإنشاء مفتاح توقيع رسمي قابل للحفظ والتصدير من هاتفك. تعمل دون إنترنت ولا تحتاج إلى كمبيوتر.",15,false,ink);
        addText(root,"مهم: المفتاح الجديد لا يسمح بتحديث نسخة موقّعة بمفتاح قديم. احتفظ بتطبيق Nomad POS القديم وبياناته حتى إكمال الترحيل.",14,true,Color.rgb(157,75,22));
        addText(root,"1 — إنشاء مفتاح إصدار ثابت",19,true,ink);
        addText(root,"أدخل كلمة مرور قوية (12 حرفًا على الأقل). احتفظ بها منفصلة عن ملف المفتاح؛ بدونها لن تتمكن من نشر تحديثات لاحقة.",14,false,ink);
        password=new EditText(this);
        password.setHint("كلمة مرور مفتاح التوقيع");
        password.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        styleInput(root,password);
        confirmPassword=new EditText(this);
        confirmPassword.setHint("تأكيد كلمة المرور");
        confirmPassword.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        styleInput(root,confirmPassword);
        generateButton=addButton(root,"إنشاء مفتاح جديد مشفّر (.p12)",()->generate());
        saveButton=addButton(root,"حفظ ملف المفتاح على الهاتف",()->save());
        saveButton.setEnabled(false);saveButton.setAlpha(0.5f);
        fingerprintText=addText(root,"بصمة الشهادة SHA-256: لم تُنشأ بعد",14,true,ink);
        addButton(root,"نسخ بصمة الشهادة فقط",()->{
            if(latestFingerprint==null){toastStatus("أنشئ مفتاحًا أولًا");return;}
            ClipboardManager manager=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
            manager.setPrimaryClip(ClipData.newPlainText("Nomad certificate SHA-256",latestFingerprint));
            toastStatus("تم نسخ بصمة الشهادة فقط");
        });
        addText(root,"2 — فحص ملف توقيع موجود",19,true,ink);
        addText(root,"يمكن فحص بصمة شهادة ملف PKCS12 (.p12 أو .pfx) تختاره بنفسك. لن يرفع التطبيق الملف إلى أي خدمة.",14,false,ink);
        addButton(root,"اختيار ملف .p12 وفحصه",()->{
            Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");startActivityForResult(i,OPEN_KEY);
        });
        addText(root,"3 — فحص توقيع تطبيق Nomad POS المثبت",19,true,ink);
        addButton(root,"عرض شهادات تطبيقات Nomad المثبتة",this::showInstalledSignatures);
        status=addText(root,"جاهز. لن يتم إرسال كلمة المرور أو المفتاح لأي جهة.",14,true,primary);
        addText(root,"احتفظ بنسختين احتياطيتين من الملف في أماكن آمنة. لا تشارك ملف .p12 أو كلمة مروره في المحادثات أو المستودعات العامة.",14,true,Color.rgb(157,75,22));
    }
    private void generate(){
        final char[] a=password.getText().toString().toCharArray();
        final char[] b=confirmPassword.getText().toString().toCharArray();
        if(a.length<12){Arrays.fill(a,'\0');Arrays.fill(b,'\0');toastStatus("استخدم كلمة مرور من 12 حرفًا على الأقل");return;}
        if(!Arrays.equals(a,b)){Arrays.fill(a,'\0');Arrays.fill(b,'\0');toastStatus("كلمتا المرور غير متطابقتين");return;}
        if(pendingP12!=null){Arrays.fill(a,'\0');Arrays.fill(b,'\0');toastStatus("احفظ المفتاح الذي أنشأته قبل إنشاء آخر");return;}
        generateButton.setEnabled(false);
        status.setText("يجري إنشاء مفتاح RSA 4096 وتشفيره داخل الهاتف...");
        new Thread(()->{
            try{
                SigningKeyUtil.Bundle result=SigningKeyUtil.generate(a);
                runOnUiThread(()->{
                    pendingP12=result.p12;latestFingerprint=result.fingerprint;
                    password.setText("");confirmPassword.setText("");
                    generateButton.setEnabled(true);saveButton.setEnabled(true);saveButton.setAlpha(1f);
                    fingerprintText.setText("بصمة الشهادة SHA-256:\n"+result.fingerprint);
                    status.setText("تم إنشاء المفتاح والتحقق منه. اضغط «حفظ» الآن، ولا تغلق التطبيق قبل حفظ الملف.");
                });
            }catch(Exception e){runOnUiThread(()->{generateButton.setEnabled(true);status.setText("تعذر إنشاء المفتاح: "+e.getMessage());});}
            finally{Arrays.fill(a,'\0');Arrays.fill(b,'\0');}
        }).start();
    }
    private void save(){
        if(pendingP12==null){toastStatus("أنشئ المفتاح أولًا");return;}
        Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/x-pkcs12");
        i.putExtra(Intent.EXTRA_TITLE,"nomad-pos-release.p12");
        startActivityForResult(i,SAVE_KEY);
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(result!=RESULT_OK || data==null || data.getData()==null){
            if(request==SAVE_KEY)toastStatus("لم يُحفظ الملف، حاول مجددًا");return;
        }
        Uri uri=data.getData();
        if(request==SAVE_KEY){
            try(OutputStream out=getContentResolver().openOutputStream(uri,"w")){
                if(out==null || pendingP12==null)throw new IllegalStateException("ملف التوقيع غير جاهز");
                out.write(pendingP12);out.flush();
                Arrays.fill(pendingP12,(byte)0);pendingP12=null;
                saveButton.setEnabled(false);saveButton.setAlpha(0.5f);
                status.setText("تم حفظ nomad-pos-release.p12. أنشئ نسختين احتياطيتين واحفظ كلمة المرور بعيدًا عن الملف.");
            }catch(Exception e){status.setText("خطأ أثناء الحفظ: "+e.getMessage()+". تحقق من الملف ولا تعتبره نسخة سليمة قبل التحقق.");}
        }else if(request==OPEN_KEY){
            char[] pass=password.getText().toString().toCharArray();
            try(InputStream in=getContentResolver().openInputStream(uri)){
                if(in==null)throw new IllegalStateException("تعذر فتح الملف");
                ByteArrayOutputStream buffer=new ByteArrayOutputStream();
                byte[] block=new byte[8192];int read;
                while((read=in.read(block))>=0){
                    if(buffer.size()+read>1024*1024)throw new IllegalStateException("حجم ملف التوقيع أكبر من الحد المقبول");
                    buffer.write(block,0,read);
                }
                byte[] contents=buffer.toByteArray();
                String fp=SigningKeyUtil.readFingerprint(contents,pass);
                Arrays.fill(contents,(byte)0);
                fingerprintText.setText("بصمة الملف المحدد SHA-256:\n"+fp);
                latestFingerprint=fp;
                status.setText("تم فحص الملف المحدد. قارن البصمة ببصمة التطبيق المطلوب تحديثه.");
            }catch(Exception e){status.setText("تعذر قراءة التوقيع. تأكد أنه PKCS12 (.p12 أو .pfx) وأن كلمة المرور صحيحة. لا تدعم هذه الأداة ملفات JKS: "+e.getClass().getSimpleName());}
            finally{Arrays.fill(pass,'\0');password.setText("");}
        }
    }
    private void showInstalledSignatures(){
        String[] candidates={"com.nanopos.nomad.fresh5","com.nanopos.nomad.stable","com.nanopos.nomad","com.nomad.pos","com.nanopos.mobile"};
        StringBuilder found=new StringBuilder();PackageManager pm=getPackageManager();
        for(String pkg:candidates){
            try{
                Signature[] s;
                if(Build.VERSION.SDK_INT>=28){
                    PackageInfo info=pm.getPackageInfo(pkg,PackageManager.GET_SIGNING_CERTIFICATES);
                    if(info.signingInfo==null)continue;
                    s=info.signingInfo.getApkContentsSigners();
                }else{
                    PackageInfo info=pm.getPackageInfo(pkg,PackageManager.GET_SIGNATURES);s=info.signatures;
                }
                if(s!=null&&s.length>0)
                    found.append(pkg).append("\n").append(SigningKeyUtil.sha256(s[0].toByteArray())).append("\n\n");
            }catch(Exception ignored){}
        }
        if(found.length()==0)found.append("لم يُعثر على حزمة من الأسماء المعروفة. قد يكون تطبيقك باسم حزمة مختلف، أو يمنع Android الاستعلام عنها.");
        new AlertDialog.Builder(this).setTitle("بصمة توقيع التطبيقات المثبتة").setMessage(found.toString())
            .setPositiveButton("موافق",(d,w)->{}).show();
    }
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private TextView addText(LinearLayout root,String message,int sp,boolean bold,int color){
        TextView v=new TextView(this);v.setText(message);v.setTextSize(sp);v.setTextColor(color);
        v.setGravity(Gravity.RIGHT);v.setPadding(0,dp(9),0,dp(9));
        if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        root.addView(v,new LinearLayout.LayoutParams(-1,-2));return v;
    }
    private void styleInput(LinearLayout root,EditText e){
        e.setTextSize(16);e.setTextColor(ink);e.setSingleLine(true);e.setPadding(dp(12),dp(9),dp(12),dp(9));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(52));p.bottomMargin=dp(10);root.addView(e,p);
    }
    private Button addButton(LinearLayout root,String label,Runnable action){
        Button b=new Button(this);b.setText(label);b.setTextSize(14);b.setAllCaps(false);
        b.setTextColor(Color.WHITE);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(primary));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(50));p.bottomMargin=dp(12);
        root.addView(b,p);b.setOnClickListener(v->action.run());return b;
    }
    private void toastStatus(String message){status.setText(message);}
}
