package com.nomad.pos;

import android.app.*;
import android.os.*;
import android.provider.Settings;
import android.graphics.Color;
import android.graphics.Typeface;
import android.content.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MainActivity extends Activity {
    final int GREEN=Color.rgb(22,101,52), DARK=Color.rgb(15,23,42), MUTED=Color.rgb(100,116,139), BG=Color.rgb(248,250,252);
    LinearLayout root, content; TextView title; String deviceId;
    String[] sections={"نقطة البيع","المنتجات","المخزون","العملاء","الموردون","المشتريات","فواتير البيع","الفواتير المبدئية","المصاريف","المناوبة","التقارير","الباركود","الإعدادات","التفعيل","حول البرنامج"};
    String[] icons={"🛒","📦","🏷","👥","🚚","🧾","📄","📝","💳","🕘","📊","▦","⚙","🔐","ⓘ"};
    @Override public void onCreate(Bundle b){super.onCreate(b); getWindow().setStatusBarColor(BG); getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR); deviceId=makeId(); dashboard();}
    TextView tv(String s,int sp,int c,boolean bold){ TextView v=new TextView(this); v.setText(s);v.setTextSize(sp);v.setTextColor(c);v.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);v.setPadding(dp(12),dp(8),dp(12),dp(8)); if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}
    void base(String name){ root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
      LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(dp(10),dp(8),dp(10),dp(8));bar.setBackgroundColor(Color.WHITE);
      TextView back=tv("‹",34,DARK,true);back.setGravity(Gravity.CENTER);back.setOnClickListener(v->dashboard());bar.addView(back,new LinearLayout.LayoutParams(dp(48),dp(52)));
      title=tv(name,21,DARK,true);bar.addView(title,new LinearLayout.LayoutParams(0,dp(52),1));root.addView(bar);
      ScrollView sc=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(16),dp(18),dp(16),dp(24));sc.addView(content);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);}
    void dashboard(){ root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
      LinearLayout head=new LinearLayout(this);head.setOrientation(LinearLayout.VERTICAL);head.setPadding(dp(20),dp(22),dp(20),dp(18));head.setBackgroundColor(Color.WHITE);
      head.addView(tv("Nomad POS",28,DARK,true));head.addView(tv("إدارة متجرك بسهولة • v0.1.0",14,MUTED,false));root.addView(head);
      ScrollView sc=new ScrollView(this);LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(14),dp(14),dp(14),dp(24));
      LinearLayout stats=new LinearLayout(this); stats.setWeightSum(2); stats.addView(stat("مبيعات اليوم","0.00 دج"),new LinearLayout.LayoutParams(0,dp(105),1));stats.addView(stat("الفواتير","0"),new LinearLayout.LayoutParams(0,dp(105),1));box.addView(stats);
      TextView h=tv("الأقسام",19,DARK,true);h.setPadding(dp(8),dp(18),dp(8),dp(8));box.addView(h);
      for(int i=0;i<sections.length;i+=2){LinearLayout row=new LinearLayout(this);row.setWeightSum(2);for(int j=i;j<i+2&&j<sections.length;j++){final String s=sections[j];final int k=j;row.addView(card(icons[j],s,v->open(k)),new LinearLayout.LayoutParams(0,dp(112),1));}box.addView(row);}
      sc.addView(box);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);}
    View stat(String a,String b){LinearLayout c=panel();c.setOrientation(LinearLayout.VERTICAL);c.setGravity(Gravity.CENTER);c.addView(tv(b,23,GREEN,true));c.addView(tv(a,14,MUTED,false));return c;}
    View card(String icon,String name,View.OnClickListener l){LinearLayout c=panel();c.setOrientation(LinearLayout.VERTICAL);c.setGravity(Gravity.CENTER);TextView ic=tv(icon,30,GREEN,false);ic.setGravity(Gravity.CENTER);TextView n=tv(name,15,DARK,true);n.setGravity(Gravity.CENTER);c.addView(ic);c.addView(n);c.setOnClickListener(l);return c;}
    LinearLayout panel(){LinearLayout c=new LinearLayout(this);GradientDrawableCompat.bg(c,Color.WHITE,18);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-1);p.setMargins(dp(6),dp(6),dp(6),dp(6));c.setLayoutParams(p);c.setElevation(dp(2));return c;}
    void open(int i){if(i==0){pos();return;} if(i==13){activation();return;} base(sections[i]); content.addView(tv(icons[i]+"  "+sections[i],25,DARK,true));content.addView(tv(desc(i),16,MUTED,false)); if(i==1)addProductUI(); else generic(i);}
    String desc(int i){String[] d={"","إدارة المنتجات والأسعار والباركود.","متابعة الكميات وحركة المخزون.","سجل العملاء والحسابات.","إدارة الموردين وأرصدتهم.","فواتير الشراء والتوريد.","سجل فواتير المبيعات.","إنشاء عروض وفواتير مبدئية.","تسجيل مصاريف المتجر.","فتح وإغلاق المناوبة وحركة الصندوق.","ملخصات المبيعات والأرباح.","إنشاء وطباعة ملصقات الباركود.","اللغة، العملة والطباعة.","","Nomad POS • الإصدار 0.1.0"};return d[i];}
    void generic(int i){Button b=new Button(this);b.setText(i==9?"فتح المناوبة":"إضافة سجل جديد");b.setTextSize(16);b.setOnClickListener(v->Toast.makeText(this,"هذه الوظيفة جاهزة للتطوير في النسخة التالية",Toast.LENGTH_SHORT).show());content.addView(b,new LinearLayout.LayoutParams(-1,dp(56)));}
    void addProductUI(){EditText search=new EditText(this);search.setHint("بحث بالاسم أو الباركود");content.addView(search,new LinearLayout.LayoutParams(-1,dp(58)));Button add=new Button(this);add.setText("+ إضافة منتج");add.setOnClickListener(v->productDialog());content.addView(add,new LinearLayout.LayoutParams(-1,dp(56)));content.addView(tv("لا توجد منتجات بعد",16,MUTED,false));}
    void productDialog(){LinearLayout l=new LinearLayout(this);l.setPadding(dp(16),0,dp(16),0);l.setOrientation(LinearLayout.VERTICAL);EditText n=new EditText(this);n.setHint("اسم المنتج");EditText p=new EditText(this);p.setHint("السعر");EditText q=new EditText(this);q.setHint("الكمية");l.addView(n);l.addView(p);l.addView(q);new AlertDialog.Builder(this).setTitle("منتج جديد").setView(l).setPositiveButton("حفظ",(d,w)->Toast.makeText(this,"سيتم ربط الحفظ بقاعدة البيانات في النسخة التالية",Toast.LENGTH_LONG).show()).setNegativeButton("إلغاء",null).show();}
    void pos(){base("نقطة البيع");EditText s=new EditText(this);s.setHint("بحث عن منتج أو مسح الباركود");content.addView(s,new LinearLayout.LayoutParams(-1,dp(60)));content.addView(tv("سلة البيع",20,DARK,true));content.addView(tv("لم تتم إضافة منتجات",16,MUTED,false));Space sp=new Space(this);content.addView(sp,new LinearLayout.LayoutParams(1,dp(120)));content.addView(tv("الإجمالي     0.00 دج",24,DARK,true));Button pay=new Button(this);pay.setText("إتمام البيع");pay.setTextSize(18);pay.setOnClickListener(v->Toast.makeText(this,"أضف منتجًا أولاً",Toast.LENGTH_SHORT).show());content.addView(pay,new LinearLayout.LayoutParams(-1,dp(60)));}
    void activation(){base("تفعيل Nomad POS");content.addView(tv("معرّف هذا التثبيت",15,MUTED,false));TextView id=tv(deviceId,18,DARK,true);id.setTextIsSelectable(true);content.addView(id);Button copy=new Button(this);copy.setText("نسخ المعرّف");copy.setOnClickListener(v->{((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Nomad ID",deviceId));Toast.makeText(this,"تم النسخ",Toast.LENGTH_SHORT).show();});content.addView(copy);EditText code=new EditText(this);code.setHint("أدخل رمز التفعيل");content.addView(code);Button act=new Button(this);act.setText("تفعيل");act.setOnClickListener(v->{String expected=Activation.code(deviceId);if(expected.equals(code.getText().toString().trim())){getPreferences(0).edit().putBoolean("active",true).apply();new AlertDialog.Builder(this).setMessage("تم تفعيل Nomad POS بنجاح").setPositiveButton("حسناً",null).show();}else Toast.makeText(this,"رمز التفعيل غير صحيح",Toast.LENGTH_LONG).show();});content.addView(act);content.addView(tv(getPreferences(0).getBoolean("active",false)?"الحالة: مفعّل ✓":"الحالة: غير مفعّل",16,getPreferences(0).getBoolean("active",false)?GREEN:MUTED,true));}
    String makeId(){String a=Settings.Secure.getString(getContentResolver(),Settings.Secure.ANDROID_ID);try{byte[] h=MessageDigest.getInstance("SHA-256").digest(("NOMAD-"+a).getBytes(StandardCharsets.UTF_8));StringBuilder x=new StringBuilder("NMD-");for(int i=0;i<6;i++)x.append(String.format("%02X",h[i]));return x.toString();}catch(Exception e){return "NMD-"+a.toUpperCase();}}
    int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    static class Activation{static String code(String id){try{byte[] h=MessageDigest.getInstance("SHA-256").digest(("NOMAD-ACT-2026|"+id).getBytes(StandardCharsets.UTF_8));StringBuilder s=new StringBuilder("NP-");for(int i=0;i<8;i++)s.append(String.format("%02X",h[i]));return s.toString();}catch(Exception e){return "";}}}
    static class GradientDrawableCompat{static void bg(View v,int c,int r){android.graphics.drawable.GradientDrawable g=new android.graphics.drawable.GradientDrawable();g.setColor(c);g.setCornerRadius(r);v.setBackground(g);}}
}