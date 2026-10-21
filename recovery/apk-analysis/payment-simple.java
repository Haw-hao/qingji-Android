package ink.qingji.app.notification;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import kotlin.Metadata;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.internal.Intrinsics;
import kotlin.ranges.IntRange;
import kotlin.text.MatchResult;
import kotlin.text.Regex;
import kotlin.text.RegexOption;
import kotlin.text.StringsKt;
import kotlin.text.Typography;

/* JADX INFO: compiled from: PaymentNotificationParser.kt */
/* JADX INFO: loaded from: classes2.dex */
@Metadata(d1 = {"\u0000*\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\b\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0000\u001a \u0010\u0006\u001a\u0004\u0018\u00010\u00072\u0006\u0010\b\u001a\u00020\u00042\u0006\u0010\t\u001a\u00020\u00042\u0006\u0010\n\u001a\u00020\u0004\u001a\u0018\u0010\u000b\u001a\u00020\u00042\u0006\u0010\n\u001a\u00020\u00042\u0006\u0010\f\u001a\u00020\rH\u0002\u001a\u0018\u0010\u000e\u001a\u00020\u00042\u0006\u0010\u000f\u001a\u00020\u00042\u0006\u0010\u0010\u001a\u00020\u0011H\u0002\"\u000e\u0010\u0000\u001a\u00020\u0001X\u0082\u0004¢\u0006\u0002\n\u0000\"\u0014\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003X\u0082\u0004¢\u0006\u0002\n\u0000\"\u0014\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003X\u0082\u0004¢\u0006\u0002\n\u0000¨\u0006\u0012"}, d2 = {"amountPattern", "Lkotlin/text/Regex;", "expenseWords", "", "", "incomeWords", "parsePaymentNotification", "Link/qingji/app/notification/ParsedPaymentNotification;", "packageName", "title", "text", "extractMerchant", "amountEnd", "", "inferCategory", "merchant", "income", "", "app_release"}, k = 2, mv = {2, 0, 0}, xi = 48)
public final class PaymentNotificationParserKt {
    private static final Regex amountPattern = null;
    private static final List<String> expenseWords = null;
    private static final List<String> incomeWords = null;

    static {
        amountPattern = new Regex("(?:¥|￥|RMB)\\s*([0-9]+(?:[.,][0-9]{1,2})?)", RegexOption.IGNORE_CASE);
        expenseWords = CollectionsKt.listOf(new String[]{"微信支付", "付款", "消费", "支出", "转出"});
        incomeWords = CollectionsKt.listOf(new String[]{"收款", "到账", "收入", "转入"});
    }

    public static final ParsedPaymentNotification parsePaymentNotification(String r21, String r22, String r23) {
        Intrinsics.checkNotNullParameter(r21, "packageName");
        Intrinsics.checkNotNullParameter(r22, "title");
        Intrinsics.checkNotNullParameter(r23, "text");
        String r1 = r22 + " " + r23;
        if (Intrinsics.areEqual(r21, "com.tencent.mm") == false) goto L17;
        List r3 = CollectionsKt.listOf(new String[]{"微信支付", "微信收款", "微信转账", "微信红包"});
        if ((r3 instanceof Collection) == true) goto L7;
    L9:
        Iterator r4 = r3.iterator();
    L11:
        if (r4.hasNext() == false) goto L17;
        if (StringsKt.contains$default(r1, (CharSequence) r4.next(), false, 2, null) == false) goto L11;
        String r0 = "微信";
    L15:
        String r14 = r0;
        String r2 = r1;
        MatchResult r5 = Regex.find$default(amountPattern, r2, 0, 2, null);
        if (r5 == null) goto L58;
        List<String> r6 = r5.getGroupValues();
        if (r6 == null) goto L58;
        String r15 = (String) CollectionsKt.getOrNull(r6, 1);
        if (r15 == null) goto L58;
        String r7 = StringsKt.replace$default(r15, ",", ".", false, 4, null);
        if (r7 == null) goto L58;
        Double r8 = StringsKt.toDoubleOrNull(r7);
        if (r8 == null) goto L58;
        double r11 = r8.doubleValue();
        List<String> r9 = incomeWords;
        if ((r9 instanceof Collection) == true) goto L42;
    L44:
        Iterator<T> r10 = r9.iterator();
    L46:
        if (r10.hasNext() == false) goto L43;
        if (StringsKt.contains$default(r2, (CharSequence) r10.next(), false, 2, null) == false) goto L46;
        boolean r13 = true;
    L50:
        String r12 = r23;
        MatchResult r16 = Regex.find$default(amountPattern, r12, 0, 2, null);
        if (r16 == null) goto L55;
        IntRange r17 = r16.getRange();
        if (r17 == null) goto L55;
        int r18 = r17.getLast();
    L56:
        String r19 = extractMerchant(r23, r18);
        return new ParsedPaymentNotification(r11, r13, r14, r19, inferCategory(r19, r13), StringsKt.trim(r12).toString());
    L55:
        r18 = -1;
    L43:
        r13 = false;
        goto L50
    L42:
        if (r9.isEmpty() == false) goto L44;
    L58:
        return null;
    L7:
        if (r3.isEmpty() == false) goto L9;
    L17:
        if (Intrinsics.areEqual(r21, "com.eg.android.AlipayGphone") == false) goto L58;
        List r20 = CollectionsKt.listOf(new String[]{"支付宝付款", "支付宝收款", "支付宝转账", "支付宝红包"});
        if ((r20 instanceof Collection) == true) goto L21;
    L23:
        Iterator r24 = r20.iterator();
    L25:
        if (r24.hasNext() == false) goto L58;
        if (StringsKt.contains$default(r1, (CharSequence) r24.next(), false, 2, null) == false) goto L25;
        r0 = "支付宝";
        goto L15
    L21:
        if (r20.isEmpty() == false) goto L23;
        goto L23
    }

    private static final String extractMerchant(String r1, int r2) {
        if (r2 < 0) goto L8;
        int r3 = r2 + 1;
        if (r3 >= r1.length()) goto L10;
        String r4 = r1.substring(r3);
        Intrinsics.checkNotNullExpressionValue(r4, "substring(...)");
        return StringsKt.trim(StringsKt.removePrefix(StringsKt.removePrefix(StringsKt.trim(r4, new char[]{' ', '\t', '\n', '\r', ',', 65292, ':', 65306, '-', Typography.mdash}), "来自"), "付款给")).toString();
    L10:
        return "";
    L8:
        return "";
    }

    private static final String inferCategory(String r9, boolean r10) {
        if (r10 == false) goto L5;
        return "工资";
    L5:
        String r11 = r9.toLowerCase(Locale.ROOT);
        Intrinsics.checkNotNullExpressionValue(r11, "toLowerCase(...)");
        List r0 = CollectionsKt.listOf(new String[]{"奶茶", "咖啡", "饮料", "茶"});
        if ((r0 instanceof Collection) == true) goto L8;
    L10:
        Iterator r1 = r0.iterator();
    L12:
        if (r1.hasNext() == false) goto L16;
        if (StringsKt.contains$default(r11, (CharSequence) r1.next(), false, 2, null) == false) goto L12;
        return "饮料";
    L16:
        List r2 = CollectionsKt.listOf(new String[]{"餐厅", "饭", "面", "麦当劳", "肯德基", "外卖"});
        if ((r2 instanceof Collection) == true) goto L19;
    L21:
        Iterator r3 = r2.iterator();
    L23:
        if (r3.hasNext() == false) goto L27;
        if (StringsKt.contains$default(r11, (CharSequence) r3.next(), false, 2, null) == false) goto L23;
        return "餐饮";
    L27:
        List r12 = CollectionsKt.listOf(new String[]{"滴滴", "打车", "地铁", "公交"});
        if ((r12 instanceof Collection) == true) goto L30;
    L32:
        Iterator r13 = r12.iterator();
    L34:
        if (r13.hasNext() == false) goto L38;
        if (StringsKt.contains$default(r11, (CharSequence) r13.next(), false, 2, null) == false) goto L34;
        return "交通";
    L38:
        List r14 = CollectionsKt.listOf(new String[]{"淘宝", "京东", "购物"});
        if ((r14 instanceof Collection) == true) goto L41;
    L43:
        Iterator r15 = r14.iterator();
    L45:
        if (r15.hasNext() == false) goto L49;
        if (StringsKt.contains$default(r11, (CharSequence) r15.next(), false, 2, null) == false) goto L45;
        return "购物";
    L49:
        return "其他";
    L41:
        if (r14.isEmpty() == false) goto L43;
    L30:
        if (r12.isEmpty() == false) goto L32;
    L19:
        if (r2.isEmpty() == false) goto L21;
    L8:
        if (r0.isEmpty() == false) goto L10;
        goto L10
    }
}
