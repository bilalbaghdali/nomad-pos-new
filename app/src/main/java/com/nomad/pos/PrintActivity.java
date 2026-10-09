package com.nomad.pos;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Responsive, phone-sized receipt preview. Printing uses Android's system
 * print service (including Save as PDF), not an unimplemented direct Bluetooth
 * or ESC/POS protocol. Both preview and printed pages use the same HTML.
 */
public class PrintActivity extends Activity {
    private static final int BG=0xFFF4F7FA, WHITE=Color.WHITE,
            INK=0xFF152235, SUB=0xFF64748B, TEAL=0xFF0D9488,
            BORDER=0xFFE2E8F0, TINT=0xFFEAF7F5;
    private JSONObject sale;
    private boolean french=false;
    private String paper="80";
    private WebView receiptView;
    private boolean previewReady=false;
    private TextView printButton;
    private LinearLayout languageOptions, paperOptions;

    private int dp(float n) {
        return (int)(getResources().getDisplayMetrics().density*n+0.5f);
    }
    private GradientDrawable bg(int fill, int radius, int stroke) {
        GradientDrawable d=new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radius));
        if(stroke!=0)d.setStroke(dp(1),stroke);
        return d;
    }
    private LinearLayout column() {
        LinearLayout c=new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        return c;
    }
    private LinearLayout row() {
        LinearLayout r=new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        return r;
    }
    private TextView label(String s,int size,int color,boolean bold) {
        TextView t=new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);
        t.setMaxLines(3);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return t;
    }
    private TextView action(String s, boolean active, View.OnClickListener click) {
        TextView v=label(s,14,active?WHITE:INK,true);
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(9),dp(10),dp(9),dp(10));
        v.setMinimumHeight(dp(48));
        v.setBackground(bg(active?TEAL:WHITE,12,active?0:BORDER));
        v.setOnClickListener(click);
        return v;
    }
    private void choice(LinearLayout target,String title,boolean active,View.OnClickListener click) {
        TextView b=action(title,active,click);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);
        lp.setMargins(dp(3),0,dp(3),0);
        target.addView(b,lp);
    }
    private LinearLayout card() {
        LinearLayout c=column();
        c.setPadding(dp(13),dp(14),dp(13),dp(14));
        c.setBackground(bg(WHITE,16,BORDER));
        return c;
    }
    private void addCard(LinearLayout parent, LinearLayout c) {
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
        lp.setMargins(0,0,0,dp(12));
        parent.addView(c,lp);
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        String json=getIntent().getStringExtra("sale");
        try { if(!TextUtils.isEmpty(json)) sale=new JSONObject(json); }
        catch(Exception ignored) { sale=null; }
        french=getSharedPreferences("nomad_print",MODE_PRIVATE).getBoolean("fr",false);
        paper=getSharedPreferences("nomad_print",MODE_PRIVATE).getString("paper","80");
        if(!paper.equals("58")&&!paper.equals("80")&&!paper.equals("A4"))paper="80";
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(WHITE);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);

        LinearLayout root=column();
        root.setBackgroundColor(BG);
        if(Build.VERSION.SDK_INT>=30) {
            root.setOnApplyWindowInsetsListener((v,insets)-> {
                android.graphics.Insets safe=insets.getInsets(WindowInsets.Type.systemBars());
                v.setPadding(0,safe.top,0,safe.bottom);
                return insets;
            });
        } else root.setFitsSystemWindows(true);

        LinearLayout header=row();
        header.setPadding(dp(14),dp(8),dp(14),dp(8));
        header.setBackgroundColor(WHITE);
        TextView back=action("رجوع",false,v->finish());
        header.addView(back,new LinearLayout.LayoutParams(dp(78),-2));
        TextView title=label("معاينة وطباعة الفاتورة",19,INK,true);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams titleParams=new LinearLayout.LayoutParams(0,-2,1);
        titleParams.setMargins(dp(10),0,0,0);
        header.addView(title,titleParams);
        root.addView(header,new LinearLayout.LayoutParams(-1,dp(65)));

        ScrollView scroll=new ScrollView(this);
        scroll.setClipToPadding(false);
        scroll.setFillViewport(true);
        LinearLayout body=column();
        int pad=getResources().getConfiguration().screenWidthDp>=600?dp(28):dp(12);
        body.setPadding(pad,dp(14),pad,dp(20));
        scroll.addView(body);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        if(sale==null) {
            LinearLayout c=card();
            c.addView(label("لا توجد فاتورة للمعاينة. احفظ عملية بيع أولاً، ثم اختر الفاتورة من سجل المبيعات.",15,SUB,false));
            addCard(body,c);
        } else {
            LinearLayout info=card();
            info.addView(label("فاتورة بيع محفوظة",16,INK,true));
            TextView date=label(formatDate(sale.optLong("date")),13,SUB,false);
            date.setPadding(0,dp(5),0,0);
            info.addView(date);
            addCard(body,info);

            LinearLayout settings=card();
            settings.addView(label("لغة الوثيقة / Langue",15,INK,true));
            languageOptions=row();
            LinearLayout.LayoutParams group=new LinearLayout.LayoutParams(-1,-2);
            group.topMargin=dp(8);
            settings.addView(languageOptions,group);
            TextView paperLabel=label("حجم الورق",15,INK,true);
            paperLabel.setPadding(0,dp(15),0,dp(8));
            settings.addView(paperLabel);
            paperOptions=row();
            settings.addView(paperOptions);
            addCard(body,settings);

            LinearLayout preview=card();
            preview.addView(label("المعاينة قبل الطباعة",16,INK,true));
            TextView note=label("مرّر داخل المعاينة لعرض جميع المنتجات.",12,SUB,false);
            note.setPadding(0,dp(6),0,dp(8));
            preview.addView(note);
            receiptView=new WebView(this);
            receiptView.getSettings().setJavaScriptEnabled(false);
            receiptView.getSettings().setBlockNetworkLoads(true);
            receiptView.setBackgroundColor(BG);
            receiptView.setWebViewClient(new WebViewClient(){
                @Override public void onPageFinished(WebView view,String url) {
                    previewReady=true;
                    if(printButton!=null){
                        printButton.setEnabled(true);
                        printButton.setAlpha(1f);
                    }
                }
            });
            LinearLayout.LayoutParams webParams=new LinearLayout.LayoutParams(-1,dp(340));
            preview.addView(receiptView,webParams);
            addCard(body,preview);
            TextView footerNote=label("الطباعة تتم عبر خدمة الطباعة في أندرويد. اختر طابعة متوافقة أو «حفظ كـ PDF» من شاشة النظام. قد تختلف أحجام الورق المتاحة حسب الطابعة.",12,SUB,false);
            footerNote.setPadding(dp(4),dp(4),dp(4),dp(9));
            body.addView(footerNote);
            drawChoices();
            renderPreview();
        }

        LinearLayout bottom=column();
        bottom.setPadding(dp(14),dp(8),dp(14),dp(8));
        bottom.setBackgroundColor(WHITE);
        printButton=action(sale==null?"لا توجد فاتورة":"طباعة / حفظ PDF",true,v->printReceipt());
        printButton.setEnabled(false);
        printButton.setAlpha(sale==null?0.5f:0.65f);
        bottom.addView(printButton,new LinearLayout.LayoutParams(-1,-2));
        root.addView(bottom,new LinearLayout.LayoutParams(-1,-2));
        setContentView(root);
        if(Build.VERSION.SDK_INT>=30)root.requestApplyInsets();
        // WebView can finish loading before the bottom action is constructed.
        if(previewReady && sale!=null) {
            printButton.setEnabled(true);
            printButton.setAlpha(1f);
        }
    }

    private void drawChoices() {
        languageOptions.removeAllViews();
        choice(languageOptions,"العربية",!french,v->chooseLanguage(false));
        choice(languageOptions,"Français",french,v->chooseLanguage(true));
        paperOptions.removeAllViews();
        choice(paperOptions,"58 mm",paper.equals("58"),v->choosePaper("58"));
        choice(paperOptions,"80 mm",paper.equals("80"),v->choosePaper("80"));
        choice(paperOptions,"A4",paper.equals("A4"),v->choosePaper("A4"));
    }
    private void chooseLanguage(boolean fr){
        if(french==fr)return;
        french=fr;
        getSharedPreferences("nomad_print",MODE_PRIVATE).edit().putBoolean("fr",fr).apply();
        drawChoices();
        renderPreview();
    }
    private void choosePaper(String size){
        if(paper.equals(size))return;
        paper=size;
        getSharedPreferences("nomad_print",MODE_PRIVATE).edit().putString("paper",size).apply();
        drawChoices();
        renderPreview();
    }
    private void renderPreview() {
        previewReady=false;
        if(printButton!=null){printButton.setEnabled(false);printButton.setAlpha(0.65f);}
        receiptView.loadDataWithBaseURL(null,htmlReceipt(),"text/html","UTF-8",null);
    }
    private static String safe(String value) {
        return value.replace("&","&amp;").replace("<","&lt;")
                .replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");
    }
    private String price(double value){
        return String.format(Locale.FRANCE,"%,.2f",value)+(french?" DA":" دج");
    }
    private String formatDate(long time){
        return new SimpleDateFormat("dd/MM/yyyy HH:mm",Locale.FRANCE).format(new Date(time));
    }
    private String htmlReceipt() {
        boolean a4=paper.equals("A4");
        String width=a4?"100%":paper+"mm";
        String dir=french?"ltr":"rtl";
        StringBuilder h=new StringBuilder();
        h.append("<!doctype html><html lang='").append(french?"fr":"ar")
                .append("' dir='").append(dir)
                .append("'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1'>")
                .append("<style>html,body{margin:0;padding:0;background:#f4f7fa;color:#152235;font-family:Arial,sans-serif;}")
                .append(".paper{box-sizing:border-box;margin:8px auto;padding:12px;background:white;width:")
                .append(width).append(";max-width:100%;overflow-wrap:anywhere;border:1px solid #e2e8f0;}")
                .append("h2{font-size:18px;text-align:center;margin:4px 0 10px;} .sub{font-size:11px;color:#64748b;text-align:center;}")
                .append(".line{border-top:1px dashed #99a8b7;margin:12px 0;}")
                .append("table{width:100%;border-collapse:collapse;table-layout:fixed;font-size:12px;}")
                .append("th,td{padding:5px 2px;vertical-align:top;border-bottom:1px solid #eef2f7;overflow-wrap:anywhere;}")
                .append("th:first-child,td:first-child{width:48%;text-align:start;}")
                .append("th:not(:first-child),td:not(:first-child){text-align:center;}")
                .append(".total{display:flex;justify-content:space-between;gap:10px;font-weight:bold;font-size:15px;margin-top:12px;}")
                .append(".foot{text-align:center;font-size:11px;margin-top:20px;color:#64748b;}")
                .append("@media print{html,body{background:white;} .paper{border:0;padding:2mm;margin:0;width:")
                .append(width).append(";max-width:none;} @page{margin:0;}}")
                .append("</style></head><body><div class='paper'>")
                .append("<h2>Nomad POS</h2><div class='sub'>")
                .append(french?"Ticket de vente":"وصل بيع")
                .append("</div><div class='sub'>").append(safe(formatDate(sale.optLong("date"))))
                .append("</div><div class='line'></div><table><thead><tr><th>")
                .append(french?"Article":"المنتج").append("</th><th>")
                .append(french?"Qté":"الكمية").append("</th><th>")
                .append(french?"Prix":"السعر").append("</th></tr></thead><tbody>");
        JSONArray lines=sale.optJSONArray("lines");
        if(lines!=null){
            for(int i=0;i<lines.length();i++){
                JSONObject line=lines.optJSONObject(i);
                if(line==null)continue;
                h.append("<tr><td>").append(safe(line.optString("name"))).append("</td><td>")
                        .append(line.optInt("qty")).append("</td><td>")
                        .append(safe(price(line.optDouble("price")*line.optInt("qty"))))
                        .append("</td></tr>");
            }
        }
        h.append("</tbody></table><div class='total'><span>")
                .append(french?"Total":"الإجمالي").append("</span><span>")
                .append(safe(price(sale.optDouble("total")))).append("</span></div>")
                .append("<div class='line'></div><div class='foot'>")
                .append(french?"Merci de votre visite":"شكرًا لزيارتكم")
                .append("</div></div></body></html>");
        return h.toString();
    }
    private void printReceipt(){
        if(!previewReady||receiptView==null||sale==null){
            Toast.makeText(this,"المعاينة لم تكتمل بعد",Toast.LENGTH_SHORT).show();
            return;
        }
        PrintManager manager=(PrintManager)getSystemService(Context.PRINT_SERVICE);
        if(manager==null){
            Toast.makeText(this,"خدمة الطباعة غير متاحة على هذا الهاتف",Toast.LENGTH_LONG).show();
            return;
        }
        PrintAttributes.MediaSize paperSize;
        if(paper.equals("A4"))paperSize=PrintAttributes.MediaSize.ISO_A4;
        else{
            int widthMils=paper.equals("58")?2283:3150;
            JSONArray lines=sale.optJSONArray("lines");
            int count=lines==null?0:lines.length();
            int heightMils=Math.min(30000,Math.max(4500,2600+count*700));
            paperSize=new PrintAttributes.MediaSize("NOMAD_ROLL_"+paper,
                    paper+" mm receipt",widthMils,heightMils);
        }
        PrintAttributes attrs=new PrintAttributes.Builder()
                .setMediaSize(paperSize)
                .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build();
        try {
            manager.print("Nomad-POS-"+sale.optLong("date"),
                    receiptView.createPrintDocumentAdapter("Nomad POS receipt"),attrs);
        } catch(Exception ex) {
            Toast.makeText(this,"تعذر فتح نظام الطباعة",Toast.LENGTH_LONG).show();
        }
    }
}
