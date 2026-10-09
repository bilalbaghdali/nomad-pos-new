package com.nomad.pos;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Nomad POS mobile-first native UI. This version stores product and sale data
 * locally; synchronization and direct ESC/POS printer protocols are not implemented.
 */
public class MainActivity extends Activity {
    private static final int BG=0xFFF4F7FA, WHITE=Color.WHITE, INK=0xFF152235,
            SUB=0xFF64748B, TEAL=0xFF0D9488, NAVY=0xFF1E40AF, RED=0xFFB91C1C,
            BORDER=0xFFE2E8F0, TINT=0xFFEAF7F5;
    private static final int HOME=0, POS=1, STOCK=2, SALES=3, MORE=4,
            CUSTOMERS=5, SUPPLIERS=6, PURCHASES=7, EXPENSES=8, SHIFT=9,
            REPORTS=10, QUOTES=11, BARCODE=12, SETTINGS=13, ACTIVATE=14,
            ABOUT=15, NEW_PRODUCT=16, PRINT=17;
    private final ArrayList<Product> products=new ArrayList<>();
    private final ArrayList<JSONObject> sales=new ArrayList<>();
    private final LinkedHashMap<Long,Integer> cart=new LinkedHashMap<>();
    private LinearLayout root, page, checkout;
    private int current=HOME;
    private String posSearch="";
    private String deviceId;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(WHITE);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        deviceId=makeId();
        load();
        open(HOME);
    }

    @Override public void onBackPressed() {
        if(current==HOME) super.onBackPressed(); else open(HOME);
    }

    private int dp(float n) {
        return (int)(getResources().getDisplayMetrics().density*n+0.5f);
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView v=new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);
        v.setIncludeFontPadding(true);
        if(bold) v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return v;
    }

    private GradientDrawable shape(int fill,int radius,int stroke) {
        GradientDrawable g=new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(radius));
        if(stroke!=0) g.setStroke(dp(1),stroke);
        return g;
    }

    private LinearLayout column() {
        LinearLayout v=new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        v.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        return v;
    }
    private LinearLayout row() {
        LinearLayout v=new LinearLayout(this);
        v.setOrientation(LinearLayout.HORIZONTAL);
        v.setGravity(Gravity.CENTER_VERTICAL);
        v.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        return v;
    }
    private LinearLayout panel() {
        LinearLayout v=column();
        v.setPadding(dp(14),dp(13),dp(14),dp(13));
        v.setBackground(shape(WHITE,17,BORDER));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
        lp.setMargins(dp(3),dp(5),dp(3),dp(5));
        v.setLayoutParams(lp);
        return v;
    }
    private void space(LinearLayout p,int h) {
        View v=new View(this); p.addView(v,new LinearLayout.LayoutParams(1,dp(h)));
    }
    private TextView button(String label,int bg,int fg,View.OnClickListener action) {
        TextView b=text(label,15,fg,true);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(12),dp(10),dp(12),dp(10));
        b.setMinimumHeight(dp(48));
        b.setBackground(shape(bg,12,bg==WHITE?BORDER:0));
        b.setOnClickListener(action);
        return b;
    }
    private void addButton(LinearLayout parent,String label,int bg,int fg,View.OnClickListener action) {
        TextView b=button(label,bg,fg,action);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);
        p.setMargins(dp(3),dp(6),dp(3),dp(6));
        parent.addView(b,p);
    }
    private EditText field(String hint,int inputType) {
        EditText e=new EditText(this);
        e.setHint(hint);
        e.setSingleLine(true);
        e.setTextSize(15);
        e.setTextColor(INK);
        e.setHintTextColor(SUB);
        e.setInputType(inputType);
        e.setPadding(dp(14),0,dp(14),0);
        e.setMinimumHeight(dp(52));
        e.setBackground(shape(WHITE,12,BORDER));
        e.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        return e;
    }
    private void addField(LinearLayout parent,EditText edit) {
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(54));
        lp.setMargins(dp(3),dp(5),dp(3),dp(5));
        parent.addView(edit,lp);
    }
    private void caption(String heading) {
        TextView h=text(heading,18,INK,true);
        h.setPadding(dp(6),dp(13),dp(6),dp(5));
        page.addView(h);
    }
    private void hint(LinearLayout parent,String message) {
        TextView t=text(message,14,SUB,false);
        t.setPadding(dp(8),dp(10),dp(8),dp(12));
        parent.addView(t);
    }

    private String titleFor(int which) {
        switch(which) {
            case HOME:return "الرئيسية";
            case POS:return "نقطة البيع";
            case STOCK:return "المنتجات والمخزون";
            case SALES:return "سجل المبيعات";
            case MORE:return "الأقسام";
            case CUSTOMERS:return "الزبائن";
            case SUPPLIERS:return "الموردون";
            case PURCHASES:return "المشتريات";
            case EXPENSES:return "المصاريف";
            case SHIFT:return "المناوبة";
            case REPORTS:return "التقارير";
            case QUOTES:return "الفواتير المبدئية";
            case BARCODE:return "الباركود";
            case PRINT:return "الطباعة";
            case SETTINGS:return "الإعدادات";
            case ACTIVATE:return "التفعيل";
            case ABOUT:return "حول البرنامج";
            default:return "إضافة منتج";
        }
    }

    private void open(int next) {
        current=next;
        root=column();
        root.setBackgroundColor(BG);
        if(Build.VERSION.SDK_INT>=30) {
            root.setOnApplyWindowInsetsListener((v,insets)-> {
                android.graphics.Insets safe=insets.getInsets(WindowInsets.Type.systemBars());
                v.setPadding(0,safe.top,0,safe.bottom);
                return insets;
            });
        } else root.setFitsSystemWindows(true);

        LinearLayout header=row();
        header.setBackgroundColor(WHITE);
        header.setPadding(dp(13),dp(6),dp(13),dp(6));
        if(next!=HOME) {
            TextView back=button("⌂",WHITE,INK,v->open(HOME));
            header.addView(back,new LinearLayout.LayoutParams(dp(48),dp(48)));
        }
        LinearLayout heading=column();
        TextView t=text(next==HOME?"Nomad POS":titleFor(next),21,INK,true);
        t.setMaxLines(1); t.setEllipsize(TextUtils.TruncateAt.END);
        heading.addView(t);
        heading.addView(text(next==HOME?"إدارة تجارتك بسهولة  •  v0.4.0":"واجهة ملائمة للهاتف",12,SUB,false));
        LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(0,-2,1);
        hp.setMargins(dp(8),0,dp(8),0);
        header.addView(heading,hp);
        root.addView(header,new LinearLayout.LayoutParams(-1,dp(67)));

        ScrollView sc=new ScrollView(this);
        sc.setFillViewport(true);
        sc.setClipToPadding(false);
        page=column();
        int pad=getResources().getConfiguration().screenWidthDp>=600?dp(24):dp(12);
        page.setPadding(pad,dp(11),pad,dp(22));
        sc.addView(page,new ScrollView.LayoutParams(-1,-2));
        root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));

        switch(next) {
            case HOME:showHome();break;
            case POS:showPos();break;
            case STOCK:showInventory();break;
            case SALES:showSales();break;
            case MORE:showMore();break;
            case NEW_PRODUCT:productDialog(null); break;
            case CUSTOMERS:case SUPPLIERS:showPeople(next);break;
            case REPORTS:showReports();break;
            case SHIFT:showShift();break;
            case ACTIVATE:showActivation();break;
            case ABOUT:showAbout();break;
            case SETTINGS:showSettings();break;
            case PURCHASES:showPlaceholder("تسجيل فواتير الشراء", "نظام فواتير الشراء غير متاح في هذه النسخة التجريبية.");break;
            case EXPENSES:showPlaceholder("تسجيل المصاريف", "نظام حفظ المصاريف غير متاح في هذه النسخة التجريبية.");break;
            case QUOTES:showPlaceholder("الفواتير المبدئية", "إنشاء الفواتير المبدئية وتحويلها إلى فواتير بيع قيد التطوير.");break;
            case BARCODE:showBarcode();break;
            case PRINT:showPrintHub();break;
            default:showPlaceholder(titleFor(next),"هذه الواجهة قيد التطوير.");
        }
        if(next==POS) {
            checkout=column();
            checkout.setBackgroundColor(WHITE);
            checkout.setPadding(dp(12),dp(5),dp(12),dp(5));
            root.addView(checkout,new LinearLayout.LayoutParams(-1,-2));
            renderCheckout();
        } else checkout=null;
        root.addView(navBar(next),new LinearLayout.LayoutParams(-1,dp(66)));
        setContentView(root);
        if(Build.VERSION.SDK_INT>=30) root.requestApplyInsets();
    }

    private LinearLayout navBar(int selected) {
        LinearLayout nav=row();
        nav.setBackgroundColor(WHITE);
        String[] names={"الرئيسية","البيع","المخزون","الفواتير","المزيد"};
        String[] symbols={"⌂","▣","▤","▧","☷"};
        int[] dest={HOME,POS,STOCK,SALES,MORE};
        int active=selected<=MORE?selected:MORE;
        for(int i=0;i<names.length;i++) {
            final int to=dest[i];
            LinearLayout item=column();
            item.setGravity(Gravity.CENTER);
            TextView icon=text(symbols[i],23,i==active?TEAL:SUB,true);
            icon.setGravity(Gravity.CENTER);item.addView(icon);
            TextView label=text(names[i],11,i==active?TEAL:SUB,i==active);
            label.setGravity(Gravity.CENTER);
            label.setMaxLines(1);
            item.addView(label);
            item.setOnClickListener(v->open(to));
            nav.addView(item,new LinearLayout.LayoutParams(0,-1,1));
        }
        return nav;
    }

    private LinearLayout statsCard(String label,String amount) {
        LinearLayout box=panel();
        box.setMinimumHeight(dp(94));
        TextView value=text(amount,20,TEAL,true);
        value.setMaxLines(2);
        if(Build.VERSION.SDK_INT>=26) value.setAutoSizeTextTypeUniformWithConfiguration(13,20,1,1);
        box.addView(value,new LinearLayout.LayoutParams(-1,-2));
        space(box,4);
        box.addView(text(label,13,SUB,false));
        return box;
    }
    private void statsPair(String label1,String amount1,String label2,String amount2) {
        int width=getResources().getConfiguration().screenWidthDp;
        if(width<350) {
            page.addView(statsCard(label1,amount1));
            page.addView(statsCard(label2,amount2));
        } else {
            LinearLayout r=row();
            r.addView(statsCard(label1,amount1),new LinearLayout.LayoutParams(0,-2,1));
            r.addView(statsCard(label2,amount2),new LinearLayout.LayoutParams(0,-2,1));
            page.addView(r);
        }
    }
    private void showHome() {
        double total=todayTotal();
        statsPair("مبيعات اليوم",money(total),"عدد المنتجات",String.valueOf(products.size()));
        int low=0, units=0;
        for(Product p:products){ units+=p.stock; if(p.stock<=3)low++; }
        statsPair("الفواتير اليوم",String.valueOf(todaySalesCount()),"قطع المخزون",String.valueOf(units));
        if(low>0){
            TextView alert=text("تنبيه: "+low+" منتج بكميات منخفضة",14,RED,true);
            alert.setPadding(dp(13),dp(14),dp(13),dp(14));
            alert.setBackground(shape(0xFFFEF2F2,12,0));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(dp(4),dp(9),dp(4),0);
            page.addView(alert,p);
        }
        caption("الوصول السريع");
        int[] destinations={POS,STOCK,SALES,CUSTOMERS,SUPPLIERS,PURCHASES,QUOTES,REPORTS,SHIFT,PRINT,SETTINGS};
        String[] symbols={"▣","▤","▧","♙","▰","▥","▦","▥","◷","▨","⚙"};
        for(int i=0;i<destinations.length;i+=2) {
            LinearLayout r=row();
            for(int j=i;j<Math.min(i+2,destinations.length);j++) {
                final int screen=destinations[j];
                r.addView(tile(symbols[j],titleFor(screen),()->open(screen)),
                        new LinearLayout.LayoutParams(0,dp(99),1));
            }
            page.addView(r);
        }
    }
    private interface Tap { void run(); }
    private LinearLayout tile(String symbol,String name,Tap click) {
        LinearLayout v=panel();
        v.setGravity(Gravity.CENTER);
        TextView ic=text(symbol,26,TEAL,true);ic.setGravity(Gravity.CENTER);
        v.addView(ic);
        TextView label=text(name,14,INK,true);
        label.setMaxLines(2);label.setGravity(Gravity.CENTER);
        v.addView(label);
        v.setOnClickListener(w->click.run());
        return v;
    }

    private void showInventory(){
        addButton(page,"＋ إضافة منتج",TEAL,WHITE,v->productDialog(null));
        EditText search=field("ابحث باسم المنتج أو الباركود",android.text.InputType.TYPE_CLASS_TEXT);
        addField(page,search);
        LinearLayout result=column();
        page.addView(result);
        drawInventory(result,"");
        search.addTextChangedListener(watcher(s->drawInventory(result,s)));
    }
    private interface Changed { void apply(String value); }
    private TextWatcher watcher(Changed c) {
        return new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            public void onTextChanged(CharSequence s,int start,int before,int count){c.apply(s.toString());}
            public void afterTextChanged(Editable s){}
        };
    }
    private void drawInventory(LinearLayout target,String query) {
        target.removeAllViews();
        int count=0;
        String q=query.toLowerCase(Locale.ROOT).trim();
        for(Product p:products) {
            if(!p.name.toLowerCase(Locale.ROOT).contains(q) && !p.barcode.toLowerCase(Locale.ROOT).contains(q))continue;
            count++;
            LinearLayout card=panel();
            LinearLayout r=row();
            TextView icon=text("▣",24,NAVY,true);icon.setGravity(Gravity.CENTER);
            r.addView(icon,new LinearLayout.LayoutParams(dp(42),dp(65)));
            LinearLayout info=column();
            TextView name=text(p.name,16,INK,true);name.setMaxLines(2);
            info.addView(name);
            info.addView(text(p.barcode.isEmpty()?"بدون باركود":p.barcode,12,SUB,false));
            info.addView(text(money(p.price),15,TEAL,true));
            r.addView(info,new LinearLayout.LayoutParams(0,-2,1));
            LinearLayout side=column();side.setGravity(Gravity.CENTER);
            TextView qty=text(String.valueOf(p.stock),18,p.stock<=3?RED:TEAL,true);
            qty.setGravity(Gravity.CENTER);side.addView(qty);
            TextView lbl=text("قطعة",11,SUB,false);lbl.setGravity(Gravity.CENTER);side.addView(lbl);
            r.addView(side,new LinearLayout.LayoutParams(dp(52),-2));
            card.addView(r);
            card.setOnClickListener(v->productDialog(p));
            target.addView(card);
        }
        if(count==0)empty(target,query.isEmpty()?"لا توجد منتجات بعد. أضف أول منتج للبدء.":"لا توجد نتائج مطابقة.");
    }
    private void empty(LinearLayout parent,String msg){
        LinearLayout p=panel();p.setGravity(Gravity.CENTER);p.setMinimumHeight(dp(120));
        TextView t=text(msg,15,SUB,false);t.setGravity(Gravity.CENTER);p.addView(t);
        parent.addView(p);
    }
    private void productDialog(Product edit){
        if(current==NEW_PRODUCT) open(STOCK);
        LinearLayout form=column();form.setPadding(dp(17),dp(7),dp(17),dp(4));
        EditText name=field("اسم المنتج *",android.text.InputType.TYPE_CLASS_TEXT);
        EditText barcode=field("الباركود (اختياري)",android.text.InputType.TYPE_CLASS_TEXT);
        EditText price=field("سعر البيع بالدينار *",8194);
        EditText buy=field("سعر الشراء بالدينار",8194);
        EditText qty=field("الكمية المتوفرة",android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
        for(EditText v:new EditText[]{name,barcode,price,buy,qty})addField(form,v);
        if(edit!=null){
            name.setText(edit.name);barcode.setText(edit.barcode);
            price.setText(number(edit.price));buy.setText(number(edit.buy));
            qty.setText(String.valueOf(edit.stock));
        }
        ScrollView scroll=new ScrollView(this);
        scroll.addView(form);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(edit==null?"إضافة منتج":"تعديل المنتج")
                .setView(scroll).setNegativeButton("إلغاء",null)
                .setPositiveButton("حفظ",null).create();
        dialog.setOnShowListener(z->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String n=name.getText().toString().trim();
            Double amount=parse(price.getText().toString());
            Double cost=parse(buy.getText().toString().isEmpty()?"0":buy.getText().toString());
            int stock;
            try {stock=Integer.parseInt(qty.getText().toString().trim());}
            catch(Exception e){qty.setError("أدخل كمية صحيحة");return;}
            if(n.isEmpty()){name.setError("اسم المنتج مطلوب");return;}
            if(amount==null||amount<0){price.setError("سعر غير صالح");return;}
            if(cost==null||cost<0){buy.setError("سعر غير صالح");return;}
            if(stock<0){qty.setError("الكمية لا يمكن أن تكون سالبة");return;}
            Product item=edit==null?new Product():edit;
            item.name=n;item.barcode=barcode.getText().toString().trim();
            item.price=amount;item.buy=cost;item.stock=stock;
            if(edit==null)products.add(item);
            save();
            dialog.dismiss();
            open(STOCK);
        }));
        dialog.show();
    }
    private Double parse(String s) {
        try{
            double n=Double.parseDouble(s.trim().replace(',','.'));
            return Double.isFinite(n)?n:null;
        } catch(Exception e){return null;}
    }
    private String number(double amount){return String.format(Locale.US,"%.2f",amount);}
    private String money(double amount){return String.format(new Locale("fr","DZ"),"%,.2f",amount)+" دج";}

    private void showPos() {
        LinearLayout customer=panel();
        customer.addView(text("●  بيع نقدي    •    اختر المنتج لإضافته للسلة",14,INK,true));
        page.addView(customer);
        EditText search=field("بحث عن منتج أو باركود",android.text.InputType.TYPE_CLASS_TEXT);
        search.setText(posSearch);
        addField(page,search);
        caption("المنتجات");
        LinearLayout items=column();page.addView(items);
        drawPosItems(items,posSearch);
        search.addTextChangedListener(watcher(s->{posSearch=s;drawPosItems(items,s);}));
        caption("السلة");
        LinearLayout cartHolder=column();
        page.addView(cartHolder);
        drawCart(cartHolder);
        cartHolder.setTag("cart");
    }
    private void drawPosItems(LinearLayout target,String query) {
        target.removeAllViews();
        int found=0;
        String q=query.toLowerCase(Locale.ROOT).trim();
        for(Product p:products) {
            if(!p.name.toLowerCase(Locale.ROOT).contains(q)&&!p.barcode.toLowerCase(Locale.ROOT).contains(q))continue;
            found++;
            LinearLayout card=panel();
            LinearLayout r=row();
            LinearLayout z=column();
            TextView n=text(p.name,15,INK,true);n.setMaxLines(2);z.addView(n);
            z.addView(text(money(p.price)+"  •  متوفر "+p.stock,13,SUB,false));
            r.addView(z,new LinearLayout.LayoutParams(0,-2,1));
            TextView add=button("＋",p.stock>0?TEAL:BORDER,p.stock>0?WHITE:SUB,
                    v->{addToCart(p);drawPosItems(target,query);refreshCartView();});
            add.setEnabled(p.stock>0);
            r.addView(add,new LinearLayout.LayoutParams(dp(52),dp(49)));
            card.addView(r);target.addView(card);
            if(q.isEmpty()&&found>=30)break;
        }
        if(found==0)empty(target,products.isEmpty()?"المخزون فارغ. أضف منتجات من قسم المخزون.":"لا يوجد منتج مطابق.");
    }
    private void addToCart(Product p) {
        int amount=cart.containsKey(p.id)?cart.get(p.id):0;
        if(amount>=p.stock){toast("الكمية المطلوبة غير متوفرة");return;}
        cart.put(p.id,amount+1);
    }
    private void refreshCartView(){
        View slot=page.findViewWithTag("cart");
        if(slot instanceof LinearLayout)drawCart((LinearLayout)slot);
        renderCheckout();
    }
    private void drawCart(LinearLayout target) {
        target.removeAllViews();
        if(cart.isEmpty()){empty(target,"السلة فارغة. اضغط ＋ بجانب أحد المنتجات.");return;}
        for(Map.Entry<Long,Integer> entry:new ArrayList<>(cart.entrySet())){
            Product p=findProduct(entry.getKey());
            if(p==null)continue;
            LinearLayout card=panel();
            LinearLayout r=row();
            LinearLayout details=column();
            TextView name=text(p.name,14,INK,true);name.setMaxLines(2);details.addView(name);
            details.addView(text(money(p.price*entry.getValue()),13,TEAL,true));
            r.addView(details,new LinearLayout.LayoutParams(0,-2,1));
            TextView minus=button("−",WHITE,INK,v->{
                int n=cart.get(p.id)-1;
                if(n==0)cart.remove(p.id);else cart.put(p.id,n);
                refreshCartView();
            });
            r.addView(minus,new LinearLayout.LayoutParams(dp(39),dp(44)));
            TextView count=text(String.valueOf(entry.getValue()),15,INK,true);count.setGravity(Gravity.CENTER);
            r.addView(count,new LinearLayout.LayoutParams(dp(34),dp(44)));
            TextView plus=button("+",TINT,TEAL,v->{addToCart(p);refreshCartView();});
            r.addView(plus,new LinearLayout.LayoutParams(dp(39),dp(44)));
            card.addView(r);target.addView(card);
        }
    }
    private double cartTotal() {
        double result=0;
        for(Map.Entry<Long,Integer> e:cart.entrySet()){
            Product p=findProduct(e.getKey());
            if(p!=null)result+=p.price*e.getValue();
        }
        return result;
    }
    private void renderCheckout(){
        if(checkout==null)return;
        checkout.removeAllViews();
        LinearLayout r=row();
        LinearLayout t=column();
        t.addView(text("الإجمالي",12,SUB,false));
        t.addView(text(money(cartTotal()),19,INK,true));
        r.addView(t,new LinearLayout.LayoutParams(0,-2,1));
        TextView pay=button("تأكيد البيع",cart.isEmpty()?BORDER:TEAL,
                cart.isEmpty()?SUB:WHITE,v->confirmSale());
        pay.setEnabled(!cart.isEmpty());
        r.addView(pay,new LinearLayout.LayoutParams(dp(142),dp(52)));
        checkout.addView(r);
    }
    private void confirmSale(){
        if(cart.isEmpty())return;
        new AlertDialog.Builder(this).setTitle("تأكيد عملية البيع")
                .setMessage("المجموع: "+money(cartTotal())+"\nسيتم خصم الكميات من المخزون وحفظ الفاتورة محليًا.")
                .setNegativeButton("إلغاء",null)
                .setPositiveButton("تأكيد",(d,w)->{
                    for(Map.Entry<Long,Integer> e:cart.entrySet()){
                        Product p=findProduct(e.getKey());
                        if(p==null||p.stock<e.getValue()){toast("تحقق من كمية المخزون");return;}
                    }
                    JSONObject sale=new JSONObject();
                    try {
                        sale.put("date",System.currentTimeMillis());
                        sale.put("total",cartTotal());
                        JSONArray lines=new JSONArray();
                        for(Map.Entry<Long,Integer> e:cart.entrySet()){
                            Product p=findProduct(e.getKey());
                            p.stock-=e.getValue();
                            JSONObject line=new JSONObject();
                            line.put("name",p.name);line.put("qty",e.getValue());
                            line.put("price",p.price);lines.put(line);
                        }
                        sale.put("lines",lines);
                        sales.add(0,sale);
                        cart.clear();save();posSearch="";
                        open(SALES);
                        toast("تم حفظ البيع بنجاح");
                    }catch(Exception ex){toast("تعذر حفظ العملية");}
                }).show();
    }

    private void showSales(){
        caption("الفواتير المحفوظة على هذا الهاتف");
        if(sales.isEmpty()){empty(page,"لا توجد مبيعات مسجلة بعد.");return;}
        for(JSONObject s:sales){
            long time=s.optLong("date");
            LinearLayout card=panel();
            LinearLayout r=row();
            LinearLayout info=column();
            info.addView(text("فاتورة بيع",16,INK,true));
            info.addView(text(date(time),12,SUB,false));
            r.addView(info,new LinearLayout.LayoutParams(0,-2,1));
            TextView sum=text(money(s.optDouble("total")),15,TEAL,true);
            r.addView(sum);
            card.addView(r);
            addButton(card,"معاينة وطباعة الفاتورة",TINT,TEAL,v->launchPrint(s));
            page.addView(card);
            card.setOnClickListener(v->{
                StringBuilder b=new StringBuilder();
                JSONArray lines=s.optJSONArray("lines");
                if(lines!=null)for(int k=0;k<lines.length();k++){
                    JSONObject line=lines.optJSONObject(k);
                    if(line!=null)b.append(line.optString("name")).append(" × ")
                            .append(line.optInt("qty")).append(" = ")
                            .append(money(line.optDouble("price")*line.optInt("qty"))).append("\n");
                }
                b.append("\nالإجمالي: ").append(money(s.optDouble("total")));
                new AlertDialog.Builder(this).setTitle("تفاصيل البيع").setMessage(b.toString())
                        .setPositiveButton("إغلاق",null).show();
            });
        }
    }
    private String date(long ms){
        return new SimpleDateFormat("dd/MM/yyyy  HH:mm",Locale.FRANCE).format(new Date(ms));
    }
    private String day(long ms){
        return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date(ms));
    }
    private int todaySalesCount(){
        String today=day(System.currentTimeMillis());int c=0;
        for(JSONObject s:sales)if(today.equals(day(s.optLong("date"))))c++;
        return c;
    }
    private double todayTotal(){
        String today=day(System.currentTimeMillis());double sum=0;
        for(JSONObject s:sales)if(today.equals(day(s.optLong("date"))))sum+=s.optDouble("total");
        return sum;
    }
    private void showPeople(int section){
        caption(section==CUSTOMERS?"الزبائن":"الموردون");
        showPlaceholder("سجل "+titleFor(section),
                "عرض الحسابات وإضافة الأشخاص غير متاحين بعد في هذه النسخة. لا يتم إنشاء سجلات وهمية.");
    }
    private void showReports(){
        caption("ملخص محلي");
        statsPair("مبيعات اليوم",money(todayTotal()),"عدد عمليات اليوم",String.valueOf(todaySalesCount()));
        int low=0;for(Product p:products)if(p.stock<=3)low++;
        statsPair("إجمالي الفواتير",String.valueOf(sales.size()),"مخزون منخفض",String.valueOf(low));
        hint(page,"الإحصاءات ناتجة عن البيانات المحفوظة على هذا الهاتف فقط.");
    }
    private void showShift(){
        caption("إدارة المناوبة");
        showPlaceholder("المناوبات","نظام الصندوق وفتح المناوبة وإغلاقها غير متاح بعد. تسجيل البيع المحلي متاح دون مناوبة حاليًا.");
    }
    private void showBarcode(){
        caption("الباركود");
        if(products.isEmpty()){empty(page,"أضف منتجًا أولاً من المخزون.");return;}
        for(Product p:products){
            if(p.barcode.isEmpty())continue;
            LinearLayout c=panel();
            c.addView(text(p.name,15,INK,true));
            TextView code=text(p.barcode,14,NAVY,false);
            code.setTextIsSelectable(true);c.addView(code);
            c.setOnClickListener(v->{
                ((ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE))
                        .setPrimaryClip(ClipData.newPlainText("Barcode",p.barcode));
                toast("تم نسخ الباركود");
            });
            page.addView(c);
        }
        hint(page,"انقر على المنتج لنسخ الباركود. طباعة الملصقات غير متاحة بعد.");
    }
    private void showSettings(){
        caption("معلومات النسخة");
        LinearLayout p=panel();
        p.addView(text("العملة: الدينار الجزائري (دج)",15,INK,true));
        space(p,8);
        p.addView(text("الوضع: تخزين محلي على الهاتف",14,SUB,false));
        space(p,8);
        p.addView(text("الإصدار: 0.4.0",14,SUB,false));
        page.addView(p);
        addButton(page,"إعدادات الطباعة ومعاينة الفواتير",TINT,TEAL,v->open(PRINT));
        hint(page,"الطباعة عبر نظام أندرويد، والمزامنة مع الكمبيوتر غير متاحة بعد.");
    }
    private void showAbout(){
        caption("Nomad POS");
        LinearLayout p=panel();
        p.addView(text("Nomad POS • v0.4.0",20,INK,true));
        hint(p,"نسخة مهيأة للهواتف: بطاقات واضحة، تنقل سفلي مبسّط، ومخزون ومبيعات محفوظة محليًا.");
        page.addView(p);
        addButton(page,"معلومات التفعيل",WHITE,TEAL,v->open(ACTIVATE));
    }
    private void showActivation(){
        caption("معرّف الهاتف");
        TextView id=text(deviceId,18,INK,true);
        id.setTextIsSelectable(true);
        LinearLayout p=panel();p.addView(id);page.addView(p);
        addButton(page,"نسخ المعرّف",WHITE,NAVY,v->{
            ((ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE))
                    .setPrimaryClip(ClipData.newPlainText("Nomad ID",deviceId));
            toast("تم نسخ المعرّف");
        });
        EditText code=field("أدخل رمز التفعيل",android.text.InputType.TYPE_CLASS_TEXT);
        addField(page,code);
        addButton(page,"تفعيل",TEAL,WHITE,v->{
            if(Activation.code(deviceId).equalsIgnoreCase(code.getText().toString().trim())){
                getPreferences(0).edit().putBoolean("active",true).apply();
                toast("تم التفعيل بنجاح");open(ACTIVATE);
            }else toast("رمز غير صحيح");
        });
        hint(page,getPreferences(0).getBoolean("active",false)?"الحالة: مفعّل":"الحالة: غير مفعّل");
    }
    private void showPlaceholder(String title,String message) {
        LinearLayout p=panel();
        p.addView(text(title,17,INK,true));
        hint(p,message);
        page.addView(p);
    }
    private void launchPrint(JSONObject sale) {
        Intent intent=new Intent(this,PrintActivity.class);
        if(sale!=null)intent.putExtra("sale",sale.toString());
        startActivity(intent);
    }
    private void showPrintHub() {
        caption("الطباعة والمعاينة");
        LinearLayout info=panel();
        info.addView(text("معاينة فواتير البيع على الهاتف",17,INK,true));
        hint(info,"اختر فاتورة محفوظة، ثم غيّر اللغة العربية أو الفرنسية وحجم الورق 58mm أو 80mm أو A4 قبل إرسالها للطباعة.");
        page.addView(info);
        if(sales.isEmpty()){
            empty(page,"لا توجد فواتير للبيع. احفظ عملية بيع لتتمكن من معاينتها وطباعتها.");
            addButton(page,"الذهاب إلى نقطة البيع",TEAL,WHITE,v->open(POS));
            return;
        }
        caption("اختر الفاتورة");
        for(JSONObject sale:sales) {
            LinearLayout card=panel();
            card.addView(text("فاتورة بيع • "+date(sale.optLong("date")),14,INK,true));
            space(card,5);
            card.addView(text(money(sale.optDouble("total")),17,TEAL,true));
            addButton(card,"معاينة وطباعة",TINT,TEAL,v->launchPrint(sale));
            page.addView(card);
        }
    }

    private void showMore(){
        caption("المبيعات والحسابات");
        menuRow("الزبائن",CUSTOMERS);menuRow("الموردون",SUPPLIERS);
        menuRow("المشتريات",PURCHASES);menuRow("الفواتير المبدئية",QUOTES);
        caption("إدارة المتجر");
        menuRow("المصاريف",EXPENSES);menuRow("المناوبة",SHIFT);
        menuRow("التقارير",REPORTS);menuRow("الباركود",BARCODE);
        menuRow("الطباعة",PRINT);
        caption("التطبيق");
        menuRow("الإعدادات",SETTINGS);menuRow("التفعيل",ACTIVATE);
        menuRow("حول البرنامج",ABOUT);
    }
    private void menuRow(String label,int destination){
        LinearLayout p=panel();
        LinearLayout r=row();
        TextView n=text(label,16,INK,true);
        r.addView(n,new LinearLayout.LayoutParams(0,dp(36),1));
        TextView arrow=text("‹",24,TEAL,true);
        arrow.setGravity(Gravity.CENTER);
        r.addView(arrow,new LinearLayout.LayoutParams(dp(32),dp(36)));
        p.addView(r);
        p.setOnClickListener(v->open(destination));
        page.addView(p);
    }

    private Product findProduct(long id){
        for(Product p:products)if(p.id==id)return p;
        return null;
    }
    private void load(){
        try{
            JSONArray ps=new JSONArray(getPreferences(0).getString("products","[]"));
            for(int i=0;i<ps.length();i++){
                JSONObject obj=ps.optJSONObject(i);
                if(obj!=null)products.add(Product.from(obj));
            }
            JSONArray ss=new JSONArray(getPreferences(0).getString("sales","[]"));
            for(int i=0;i<ss.length();i++){
                JSONObject sale=ss.optJSONObject(i);
                if(sale!=null)sales.add(sale);
            }
        }catch(Exception e){toast("تعذرت قراءة بعض البيانات المحلية");}
    }
    private void save(){
        JSONArray ps=new JSONArray(), ss=new JSONArray();
        for(Product p:products)ps.put(p.toJson());
        for(JSONObject sale:sales)ss.put(sale);
        boolean ok=getPreferences(0).edit().putString("products",ps.toString())
                .putString("sales",ss.toString()).commit();
        if(!ok)toast("تحذير: لم يتم حفظ البيانات");
    }
    private void toast(String text){Toast.makeText(this,text,Toast.LENGTH_SHORT).show();}
    private String makeId(){
        String androidId=Settings.Secure.getString(getContentResolver(),Settings.Secure.ANDROID_ID);
        try{
            byte[] digest=MessageDigest.getInstance("SHA-256")
                    .digest(("NOMAD-"+androidId).getBytes(StandardCharsets.UTF_8));
            StringBuilder b=new StringBuilder("NMD-");
            for(int i=0;i<6;i++)b.append(String.format(Locale.US,"%02X",digest[i]));
            return b.toString();
        }catch(Exception e){return "NMD-UNKNOWN";}
    }
    static class Product {
        long id=System.currentTimeMillis()+(long)(Math.random()*1000000);
        String name="",barcode="";
        double price=0,buy=0;
        int stock=0;
        JSONObject toJson(){
            JSONObject o=new JSONObject();
            try{o.put("id",id);o.put("name",name);o.put("barcode",barcode);
                o.put("price",price);o.put("buy",buy);o.put("stock",stock);}catch(Exception ignored){}
            return o;
        }
        static Product from(JSONObject o){
            Product p=new Product();p.id=o.optLong("id");p.name=o.optString("name");
            p.barcode=o.optString("barcode");p.price=o.optDouble("price");
            p.buy=o.optDouble("buy");p.stock=o.optInt("stock");return p;
        }
    }
    static class Activation{
        static String code(String id){
            try{
                byte[] h=MessageDigest.getInstance("SHA-256")
                        .digest(("NOMAD-ACT-2026|"+id).getBytes(StandardCharsets.UTF_8));
                StringBuilder s=new StringBuilder("NP-");
                for(int i=0;i<8;i++)s.append(String.format(Locale.US,"%02X",h[i]));
                return s.toString();
            }catch(Exception e){return "";}
        }
    }
}
