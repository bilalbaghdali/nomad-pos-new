package com.nomad.activation;
import android.app.*;import android.os.*;import android.graphics.*;import android.content.*;import android.view.*;import android.widget.*;import java.security.*;import java.nio.charset.StandardCharsets;
public class MainActivity extends Activity{
 EditText id;TextView out;
 public void onCreate(Bundle b){super.onCreate(b);LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(dp(22),dp(34),dp(22),dp(22));r.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);r.setBackgroundColor(Color.rgb(248,250,252));
 TextView t=tv("Nomad POS Activation",27,Color.rgb(15,23,42),true);r.addView(t);r.addView(tv("مولّد رموز التفعيل • v0.1.0",15,Color.GRAY,false));
 id=new EditText(this);id.setHint("ألصق معرّف Nomad POS");id.setTextSize(17);r.addView(id,new LinearLayout.LayoutParams(-1,dp(62)));
 EditText customer=new EditText(this);customer.setHint("اسم العميل (اختياري)");r.addView(customer,new LinearLayout.LayoutParams(-1,dp(58)));
 Button gen=new Button(this);gen.setText("إنشاء رمز التفعيل");gen.setTextSize(17);r.addView(gen,new LinearLayout.LayoutParams(-1,dp(60)));
 out=tv("سيظهر رمز التفعيل هنا",19,Color.rgb(100,116,139),true);out.setGravity(Gravity.CENTER);out.setTextIsSelectable(true);r.addView(out,new LinearLayout.LayoutParams(-1,dp(100)));
 Button copy=new Button(this);copy.setText("نسخ الرمز");r.addView(copy,new LinearLayout.LayoutParams(-1,dp(56)));
 gen.setOnClickListener(v->{String x=id.getText().toString().trim().toUpperCase();if(!x.startsWith("NMD-")){Toast.makeText(this,"معرّف غير صالح",Toast.LENGTH_SHORT).show();return;}out.setText(code(x));out.setTextColor(Color.rgb(22,101,52));});
 copy.setOnClickListener(v->{String x=out.getText().toString();if(x.startsWith("NP-")){((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Activation",x));Toast.makeText(this,"تم نسخ الرمز",Toast.LENGTH_SHORT).show();}});
 setContentView(r);}
 TextView tv(String s,int z,int c,boolean b){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(c);v.setPadding(dp(8),dp(10),dp(8),dp(10));if(b)v.setTypeface(android.graphics.Typeface.DEFAULT,1);return v;}
 static String code(String id){try{byte[] h=MessageDigest.getInstance("SHA-256").digest(("NOMAD-ACT-2026|"+id).getBytes(StandardCharsets.UTF_8));StringBuilder s=new StringBuilder("NP-");for(int i=0;i<8;i++)s.append(String.format("%02X",h[i]));return s.toString();}catch(Exception e){return "";}}
 int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
}