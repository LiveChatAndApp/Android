package cn.wildfire.chat.kit.utils;

import android.content.Context;
import android.text.InputFilter;
import android.text.Spanned;
import android.widget.Toast;

public class TextLengthFilter implements InputFilter {

    private int mMax = 0;
    private Context context;

    public TextLengthFilter(int max, Context context) {
        mMax = max;
        this.context = context;
    }

    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        int keep = mMax - (dest.length() - (dend - dstart));
        if (keep <= 0) {
            //这里，用来给用户提示
            Toast.makeText(context, "字数不能超过" + mMax, Toast.LENGTH_SHORT).show();
            return "";
        } else if (keep >= end - start) {
            return null; // keep original
        } else {
            keep += start;
            if (Character.isHighSurrogate(source.charAt(keep - 1))) {
                --keep;
                if (keep == start) {
                    return "";
                }
            }
            return source.subSequence(start, keep);
        }
    }

    /**
     * @return the maximum length enforced by this input filter
     */
    public int getMax() {
        return mMax;
    }
}